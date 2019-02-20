package A2;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class q1 {

	// Board size of 8x8.
	static final int BOARD_SIZE = 8;

	// Global variables regarding chess pieces, the board and the move count.
	static int numPieces;
	static volatile ChessPiece[] pieces;
	static volatile Semaphore[][] board;
	static volatile boolean run;
	static volatile int moveCount;

	// Global variables regarding synchronization for instantaneously accurate printing.
	static volatile boolean printReady;
	static volatile int numPrintReady;
	static Semaphore printLock;
	static Semaphore printReadyLock;

	public static void main(String[] args){
		String invalidArgs = "You must enter 2 arguments:\n1) Number of pieces (integer) \n2) Simulation time in seconds (double)";
		if(args.length < 2){
			System.out.println(invalidArgs);
		}else{
			numPieces = 0;
			double runTime = 0;
			try{
				numPieces = Math.min(64, Integer.parseInt(args[0]));
				runTime = Double.parseDouble(args[1]);
			} catch (NumberFormatException e){
				System.out.println(invalidArgs);
				System.exit(0);
			}

			// Set the board, pieces and locks
			board = new Semaphore[BOARD_SIZE][BOARD_SIZE];
			pieces = new ChessPiece[numPieces];
			printLock = new Semaphore(1);
			printReadyLock = new Semaphore(1);

			// Initialize pieces on the board (row by row).
			int xPos = 0;
			int yPos = -1;
			int i = 0;
			Random random = new Random();
			while(i < numPieces){
				xPos = i%BOARD_SIZE;
				if(xPos == 0)
					yPos++;

				// Randomly select whether to place a Queen or a Knight at the (xPos,yPos)th position on the board.
				ChessPiece piece = random.nextDouble() <= 0.5 ? new Queen(xPos,yPos) : new Knight(xPos,yPos);
				pieces[i] = piece;
				board[yPos][xPos] = piece;
				i++;
			}

			// Initialize all board positions that do not have a chess piece to be a lock.
			for(i = 0; i < BOARD_SIZE; i++){
				for(int j = 0; j < BOARD_SIZE; j++){
					if(board[i][j] == null){
						board[i][j] = new Semaphore(1);
					}
				}
			}

			run = true;

			// Set the number of threads to run equal to the number of pieces, plus another thread for the printing mechanism.
			ExecutorService executor = Executors.newFixedThreadPool(numPieces+1);

			// Execute the print mechanism thread.
			executor.execute(new PrintCount());

			// Execute the piece threads.
			for(i = 0; i < numPieces; i++){
				executor.execute(pieces[i]);
			}

			// Run the program for the specified amount of time given at run time.
			long start = System.currentTimeMillis();
			while(System.currentTimeMillis()-start < 1000*runTime);
			run = false;

			executor.shutdown();
			while (!executor.isTerminated());

			// Print the final move count.
			System.out.println("Final moves made: "+getMoveCount());
		}
	}

	/**
	 * Synchronized method for updating the moveCount.
	 * */
	synchronized static void updateCount(){
		moveCount++;
	}

	/**
	 * Synchronized method for getting the moveCount.
	 * */
	synchronized static int getMoveCount(){
		return moveCount;
	}

	/**
	 * Print the board layout - for testing purposes
	 * */
	static void printBoard(){
		for(int i = 0; i < BOARD_SIZE; i++){
			for(int j = 0; j < BOARD_SIZE; j++){
				char print = '-';
				if(board[i][j] instanceof Queen){
					print = 'Q';
				}else if(board[i][j] instanceof Knight){
					print = 'K';
				}
				System.out.print(print);
			}
			System.out.println();
		}
	}

	/**
	 * Thread for printing the moveCount with instantaneous accuracy.
	 * */
	static class PrintCount implements Runnable{
		public void run(){
			while(run){

				// Set the the threads intention to print, and wait for all threads to tell this thread that they've paused execution.
				synchronized(printReadyLock){
					try {
						printReady = true;
						printReadyLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				// Obtain the printLock, print, and then notify all threads that they may continue execution.
				synchronized(printLock){
					System.out.println("Moves made at current stage: "+getMoveCount());
					printBoard();
					printReady = false;
					printLock.notifyAll();
				}

				// Sleep this thread for 1 second.
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * ChessPiece abstract class
	 *
	 * Each chess piece itself is a lock to be placed on the board of empty locks.
	 * */
	static abstract class ChessPiece extends Semaphore implements Runnable{
		// X and y position of the piece on the board
		protected int x;
		protected int y;

		/**
		 * Constructor
		 *
		 * @param int x is the x position of the piece on the board.
		 * @param int y is the y position of the piece on the board.
		 * */
		public ChessPiece(int x, int y){
			// Set itself to be a Semaphore with 1 resource.
			super(1);
			this.x = x;
			this.y = y;
			try {
				// Acquire its own lock
				super.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public abstract void run();
		volatile int syncCount = 0;

		/**
		 * Function for synchronizing ChessPiece execution with the PrintCount thread
		 *
		 * When the PrintCount thread sets its intention to print, wait for all ChessPiece threads to stop execution.
		 * Notify the PrintCount thread that it may continue execution, and wait to be notified that it is done.
		 * */
		protected void printCount(){
			// If PrintCount sets its intention to print, increase the numPrintReady and wait to be notified that
			// the printing mechanism is done execution.
			if(printReady){

				synchronized(printLock){
					try {
						numPrintReady++;

						// If all threads are now waiting on the PrintCount thread, notify PrintCount to perform its
						// execution.
						if(numPrintReady == numPieces){
							synchronized(printReadyLock){
								numPrintReady = 0;
								printReadyLock.notify();
							}
						}
						printLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Queen ChessPiece
	 * */
	static class Queen extends ChessPiece {

		/**
		 * Constructor
		 *
		 * @param int x is the x position of this piece on the board.
		 * @param int y is the y position of this piece on the board.
		 * */
		public Queen(int x, int y){
			super(x,y);
		}

		@Override
		public void run() {

			Random random = new Random();

			while(run){

				// Check the PrintCount threads intention to print, and wait for permission to continue execution.
				printCount();

				// Generate a random number to decide what move this Piece will make.
				int randomDir = random.nextInt(8);
				int steps = 0;

				// Up
				if(randomDir == 0)
					steps = moveRow(false);

				// Down
				else if(randomDir == 1)
					steps = moveRow(true);

				// Left
				else if(randomDir == 2)
					steps = moveCol(false);

				// Right
				else if(randomDir == 3)
					steps = moveCol(true);

				// Right Down
				else if(randomDir == 4)
					steps = moveDiag(true,true);

				// Right Up
				else if(randomDir == 5)
					steps = moveDiag(true,false);

				// Left Up
				else if(randomDir == 6)
					steps = moveDiag(false,false);

				// Left Down
				else if(randomDir == 7)
					steps = moveDiag(false,true);

				// If the random move generated cannot be performed (ie. is 0), restart the loop
				if(steps == 0){
					continue;
				}
				else{
					// Update the count and sleep for 10-30 ms.
					updateCount();
					try {
						Thread.sleep((long) (10+20*random.nextDouble()));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		}

		/**
		 * Function that generates the Queen's next vertical move
		 *
		 * @param boolean posDirection decides which direction the piece will move vertically (+ = down, - = up).
		 *
		 * @return the number of steps covered.
		 * */
		private int moveRow(boolean posDirection){
			// Generate a random step.
			int steps = randomStep(this.y,posDirection);

			// Take note of the number of stepsCovered, the direction and the starting position.
			int stepsCovered = 0;
			int sign = posDirection ? 1 : -1;
			int startPos = this.y;

			// Store the previous acquired lock to keep note of how far this piece may travel.
			Semaphore prev = board[this.y][this.x];

			// While the number of steps covered is less than the intended amount and this piece
			// can acquire the next lock (ie. safely move to the next position), continue stepping.
			while(stepsCovered < steps){
				Semaphore l = board[startPos+sign*(stepsCovered+1)][this.x];
				if(l.tryAcquire()){
					stepsCovered++;
					prev = l;
				}else{
					break;
				}
			}

			// Swap the piece with the last locked position and update its y position.
			board[startPos+sign*(stepsCovered)][this.x] = this;
			board[this.y][this.x] = prev;
			this.y = startPos+sign*stepsCovered;

			// Release all locks acquired on the path to its current position.
			for(int i = 0; i < stepsCovered; i++){
				try{
					board[startPos+sign*i][this.x].release();
				}catch(Exception e){
					System.exit(0);
				}
			}

			return stepsCovered;
		}

		/**
		 * Function that generates the Queen's next horizontal move
		 *
		 * @param boolean posDirection decides which direction the piece will move horizontally (+ = right, - = left).
		 *
		 * @return the number of steps covered.
		 * */
		private int moveCol(boolean posDirection){
			// Generate a random step
			int steps = randomStep(this.x,posDirection);

			// Take note of the number of stepsCovered, the direction and the starting position.
			int stepsCovered = 0;
			int sign = posDirection ? 1 : -1;
			int startPos = this.x;

			// Store the previous acquired lock to keep note of how far this piece may travel.
			Semaphore prev = board[this.y][this.x];

			// While the number of steps covered is less than the intended amount and this piece
			// can acquire the next lock (ie. safely move to the next position), continue stepping.
			while(stepsCovered < steps){
				Semaphore l = board[this.y][startPos+sign*(stepsCovered+1)];
				if(l.tryAcquire()){
					stepsCovered++;
					prev = l;
				}else{
					break;
				}
			}

			// Swap the piece with the last locked position and update its x position.
			board[this.y][startPos+sign*(stepsCovered)] = this;
			board[this.y][this.x] = prev;
			this.x = startPos+sign*stepsCovered;

			// Release all locks acquired on the path to its current position.
			for(int i = 0; i < stepsCovered; i++){
				try{
					board[this.y][startPos+sign*i].release();
				}catch(Exception e){
					System.exit(0);
				}
			}

			return stepsCovered;
		}

		/**
		 * Function that generates the Queen's next diagonal move
		 *
		 * @param boolean posX decides which direction the piece will move horizontally (+ = right, - = left).
		 * @param boolean posY decides which direction the piece will move vertically (+ = down, - = up).
		 *
		 * @return the number of steps covered.
		 * */
		private int moveDiag(boolean posX, boolean posY){
			// Generate a random step
			int steps = randomStep(posX,posY);

			// Take note of the number of stepsCovered, the direction and the starting position.
			int stepsCovered = 0;
			int signX = posX ? 1 : -1;
			int signY = posY ? 1 : -1;
			int startPosX = this.x;
			int startPosY = this.y;

			// Store the previous acquired lock to keep note of how far this piece may travel.
			Semaphore prev = board[this.y][this.x];

			// While the number of steps covered is less than the intended amount and this piece
			// can acquire the next lock (ie. safely move to the next position), continue stepping.
			while(stepsCovered < steps){
				Semaphore l = board[startPosY+signY*(stepsCovered+1)][startPosX+signX*(stepsCovered+1)];
				if(l.tryAcquire()){
					stepsCovered++;
					prev = l;
				}else{
					break;
				}
			}

			// Swap the piece with the last locked position and update its x position.
			board[startPosY+signY*(stepsCovered)][startPosX+signX*(stepsCovered)] = this;
			board[this.y][this.x] = prev;
			this.x = startPosX+signX*stepsCovered;
			this.y = startPosY+signY*stepsCovered;

			// Release all locks acquired on the path to its current position.
			for(int i = 0; i < stepsCovered; i++){
				try{
					board[startPosY+signY*i][startPosX+signX*i].release();
				}catch(Exception e){
					System.exit(0);
				}
			}

			return stepsCovered;
		}

		/**
		 * Generate a random number of steps to take in the vertical/horizontal direction
		 *
		 * @param int start is the starting position of the piece.
		 * @param boolean posDirection is the direction the piece will move in.
		 * */
		private int randomStep(int start, boolean posDirection){
			double rand = (new Random()).nextDouble();
			rand = rand == 0.0 ? 1.0 : rand;

			// Generate a number of steps that is within the confines of the board.
			if(posDirection)
				return (int) Math.ceil((BOARD_SIZE-start-1)*rand);
			else
				return (int) Math.ceil(start*rand);
		}

		/**
		 * Generate a random number of steps to take in the diagonal direction
		 *
		 * @param int start is the starting position of the piece.
		 * @param boolean posDirection is the direction the piece will move in.
		 * */
		private int randomStep(boolean posX, boolean posY){
			double rand = (new Random()).nextDouble();
			rand = rand == 0.0 ? 1.0 : rand;

			int posXSteps = (int) Math.ceil((BOARD_SIZE-this.x-1)*rand);
			int posYSteps = (int) Math.ceil((BOARD_SIZE-this.y-1)*rand);
			int negXSteps = (int) Math.ceil(this.x*rand);
			int negYSteps = (int) Math.ceil(this.y*rand);

			// Return a number of steps that is within the confines of the board.
			if(posX && posY)
				return Math.min(posXSteps, posYSteps);
			else if(posX && !posY)
				return Math.min(posXSteps, negYSteps);
			else if(!posX && !posY)
				return Math.min(negXSteps, negYSteps);
			else
				return Math.min(negXSteps, posYSteps);
		}
	}

	/**
	 * Knight ChessPiece
	 * */
	static class Knight extends ChessPiece {

		/**
		 * Constructor
		 *
		 * @param int x is the x position of this piece on the board.
		 * @param int y is the y position of this piece on the board.
		 * */
		public Knight(int x, int y){
			super(x,y);
		}

		@Override
		public void run() {

			Random random = new Random();

			while(run){

				// Check the PrintCount threads intention to print, and wait for permission to continue execution.
				printCount();

				// Generate a random direction for this piece to move in
				int randomDir = random.nextInt(4);

				// Generate a random side of the jump to be on
				boolean side = random.nextDouble() <= 0.5 ? true : false;
				boolean didStep = false;

				// Up
				if(randomDir == 0)
					didStep = moveRow(false, side);

				// Down
				else if(randomDir == 1)
					didStep = moveRow(true, side);

				// Left
				else if(randomDir == 2)
					didStep = moveCol(false, side);

				// Right
				else if(randomDir == 3)
					didStep = moveCol(true, side);

				// If the step could not be covered, generate a new move.
				if(!didStep){
					continue;
				}
				else{
					// Update the count and sleep for 10-30 ms.
					updateCount();
					try {
						Thread.sleep((long) (10+20*random.nextDouble()));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

		/**
		 * Function that generates the Knight's next vertical move
		 *
		 * @param boolean posDirection decides which direction the piece will move vertically (+ = down, - = up).
		 * @param boolean leftSide decides which side of the vertical move to land on (true = left, false = right).
		 *
		 * @return the number of steps covered.
		 * */
		private boolean moveRow(boolean posDirection, boolean leftSide){

			// Decide which direction to move in, and which side of the jump to land on.
			int moveDirection = posDirection ? 3 : -3;
			int side = leftSide ? -1 : 1;

			// If the move is within the bounds of the board, attempt to lock the new position and move there
			if(this.y+moveDirection >= 0 && this.y+moveDirection <= BOARD_SIZE-1 && this.x+side >= 0 && this.x+side <= BOARD_SIZE-1){
				Semaphore l = board[this.y+moveDirection][this.x+side];
				if(l.tryAcquire()){
					board[this.y+moveDirection][this.x+side] = this;
					board[this.y][this.x] = l;
					board[this.y][this.x].release();
					this.y+=moveDirection;
					this.x+=side;
					return true;
				}else
					return false;
			}else
				return false;
		}

		/**
		 * Function that generates the Knight's next horizontal move
		 *
		 * @param boolean posDirection decides which direction the piece will move horizontally (+ = right, - = left).
		 * @param boolean downSide decides which side of the horizontal move to land on (true = below, false = above).
		 *
		 * @return the number of steps covered.
		 * */
		private boolean moveCol(boolean posDirection, boolean downSide){
			// Decide which direction to move in, and on which side of the jump to land on.
			int moveDirection = posDirection ? 3 : -3;
			int side = downSide ? -1 : 1;

			// If the move is within the bounds of the board, attempt to the lock the new position and move there.
			if(this.x+moveDirection >= 0 && this.x+moveDirection <= BOARD_SIZE-1 && this.y+side >= 0 && this.y+side <= BOARD_SIZE-1){
				Semaphore l = board[this.y+side][this.x+moveDirection];
				if(l.tryAcquire()){
					board[this.y+side][this.x+moveDirection] = this;
					board[this.y][this.x] = l;
					board[this.y][this.x].release();
					this.y+=side;
					this.x+=moveDirection;
					return true;
				}else
					return false;
			}else
				return false;
		}
	}
}
