package A2;

import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class q1 {

	// Board size of 8x8.
	static final int BOARD_SIZE = 8;

	// Global variables regarding chess pieces, the board and the move count.
	static int numPieces;
	static volatile ChessPiece[] pieces;
	static volatile ReentrantLock[][] board;
	static volatile boolean run;
	static volatile Integer moveCount;

	// Global variables regarding synchronization for instantaneously accurate printing.
	static volatile boolean printReady;
	static volatile Integer numPrintReady;
	static volatile int numStartReady;
	static ReentrantLock printLock;
	static ReentrantLock printReadyLock;
	static ReentrantLock startLock;

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
			board = new ReentrantLock[BOARD_SIZE][BOARD_SIZE];
			pieces = new ChessPiece[numPieces];
			printLock = new ReentrantLock();
			printReadyLock = new ReentrantLock();
			numPrintReady = 0;
			moveCount = 0;
			startLock = new ReentrantLock();

			int pieceCount = 0;

			for(int i = 0; i < BOARD_SIZE; i++){
				for(int j = 0; j < BOARD_SIZE; j++){
					board[i][j] = new ReentrantLock();
					if(pieceCount < numPieces){

						// Randomly select whether to place a Queen or a Knight at the (xPos,yPos)th position on the board.
						ChessPiece piece = Math.random() <= 0.5 ? new Queen(i,j,pieceCount) : new Knight(i,j,pieceCount);
						pieces[pieceCount] = piece;
						pieceCount++;
					}
				}
			}

			run = true;

			// Set the number of threads to run equal to the number of pieces, plus another thread for the printing mechanism.
			ExecutorService executor = Executors.newFixedThreadPool(numPieces+1);

			// Execute the print mechanism thread.
			executor.execute(new PrintCount());

			// Execute the piece threads.
			for(int i = 0; i < numPieces; i++){
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
				if(board[i][j].isLocked())
					print = 'p';
				System.out.print(print+" ");
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

				synchronized(moveCount){
					System.out.println("Moves made at current stage: "+getMoveCount());
				}
//				// Set the the threads intention to print, and wait for all threads to tell this thread that they've paused execution.
//				synchronized(printReadyLock){
//					try {
//						printReady = true;
//						System.out.println("Waiting on printReadyLock...");
//						printReadyLock.wait();
//						System.out.println("May continue!");
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//
//				// Obtain the printLock, print, and then notify all threads that they may continue execution.
//				synchronized(printLock){
//					System.out.println("Moves made at current stage: "+getMoveCount());
//					printReady = false;
//					printLock.notifyAll();
//				}

				// Sleep this thread for 1 second.
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
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
	static abstract class ChessPiece implements Runnable{
		// X and y position of the piece on the board
		protected volatile int x;
		protected volatile int y;
		protected String id;

		/**
		 * Constructor
		 *
		 * @param int x is the x position of the piece on the board.
		 * @param int y is the y position of the piece on the board.
		 * */
		public ChessPiece(int x, int y, String id){
			// Set itself to be a Semaphore with 1 resource.
			super();
			this.x = x;
			this.y = y;
			this.id = id;
		}

		public abstract void run();

		/**
		 * Function for setting the threads intent to start running.
		 * Waits for all boards to lock their initial spots for beggining execution.
		 * */
		protected void startReady(){
			board[this.y][this.x].lock();
			synchronized(startLock){
				numStartReady++;
				if(numStartReady == numPieces){
					startLock.notifyAll();
				}else{
					try {
						startLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

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
						System.out.println(numPrintReady);
						try {
							synchronized(numPrintReady){
								numPrintReady++;
								if(numPrintReady == numPieces){

									// If all threads are now waiting on the PrintCount thread, notify PrintCount to perform its
									// execution.
									numPrintReady = 0;
									synchronized(printReadyLock){
										printReadyLock.notify();
									}
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
		public Queen(int x, int y,int id){
			super(x,y,"Q"+id);
		}

		@Override
		public void run() {
			startReady();
			while(run){

				// Check the PrintCount threads intention to print, and wait for permission to continue execution.
//				printCount();

				// Generate a random number to decide what move this Piece will make.
				int randomDir = (int) Math.floor(Math.random()*8);
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
					try {
						Thread.sleep((long) (10+20*Math.random()));
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

			// Hold a stack of spaces tha this thrwad has locked
			Stack<ReentrantLock> locked = new Stack<ReentrantLock>();

			// While the number of steps covered is less than the intended amount and this piece
			// can acquire the next lock (ie. safely move to the next position), continue stepping.
			while(stepsCovered < steps){
				ReentrantLock l = board[startPos+sign*(stepsCovered+1)][this.x];
				if(l.tryLock()){
					locked.push(l);
					stepsCovered++;
				}else{
					break;
				}
			}
			if(stepsCovered > 0){
				synchronized(moveCount){
					locked.pop();
					this.y = this.y+sign*stepsCovered;
					board[startPos][this.x].unlock();
					while(!locked.isEmpty())
						locked.pop().unlock();
					moveCount++;
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

			// Hold a stack of spaces tha this thrwad has locked
			Stack<ReentrantLock> locked = new Stack<ReentrantLock>();

			// While the number of steps covered is less than the intended amount and this piece
			// can acquire the next lock (ie. safely move to the next position), continue stepping.
			while(stepsCovered < steps){
				ReentrantLock l = board[this.y][startPos+sign*(stepsCovered+1)];
				if(l.tryLock()){
					locked.push(l);
					stepsCovered++;
				}else{
					break;
				}
			}

			if(stepsCovered > 0){
				synchronized(moveCount){
					locked.pop();
					this.x = this.x+sign*stepsCovered;
					board[this.y][startPos].unlock();
					while(!locked.isEmpty())
						locked.pop().unlock();
					moveCount++;
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

			// Hold a stack of spaces that this thread has locked
			Stack<ReentrantLock> locked = new Stack<ReentrantLock>();

			// While the number of steps covered is less than the intended amount and this piece
			// can acquire the next lock (ie. safely move to the next position), continue stepping.
			while(stepsCovered < steps){
				ReentrantLock l = board[startPosY+signY*(stepsCovered+1)][startPosX+signX*(stepsCovered+1)];
				if(l.tryLock()){
					locked.push(l);
					stepsCovered++;
				}else{
					break;
				}
			}
			if(stepsCovered > 0){
				synchronized(moveCount){
					locked.pop();
					this.x = this.x+signX*stepsCovered;
					this.y = this.y+signY*stepsCovered;
					board[startPosY][startPosX].unlock();
					while(!locked.isEmpty())
						locked.pop().unlock();
					moveCount++;
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
			double rand = Math.random();
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
			double rand = Math.random();
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
		public Knight(int x, int y, int id){
			super(x,y,"K"+id);
		}

		@Override
		public void run() {
			startReady();
			while(run){

				// Check the PrintCount threads intention to print, and wait for permission to continue execution.
				printCount();

				// Generate a random direction for this piece to move in
				int randomDir = (int) (Math.floor(Math.random()*4)%4);

				// Generate a random side of the jump to be on
				boolean side = Math.random() <= 0.5 ? true : false;
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
					try {
						Thread.sleep((long) (10+20*Math.random()));
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
			int startX = this.x;
			int startY = this.y;

			// If the move is within the bounds of the board, attempt to lock the new position and move there
			if(this.y+moveDirection >= 0 && this.y+moveDirection <= BOARD_SIZE-1 && this.x+side >= 0 && this.x+side <= BOARD_SIZE-1){
				ReentrantLock l = board[this.y+moveDirection][this.x+side];
				if(l.tryLock()){
					synchronized(moveCount){
						this.y+=moveDirection;
						this.x+=side;
						board[startY][startX].unlock();
						moveCount++;
					}
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
			int startX = this.x;
			int startY = this.y;

			// If the move is within the bounds of the board, attempt to the lock the new position and move there.
			if(this.x+moveDirection >= 0 && this.x+moveDirection <= BOARD_SIZE-1 && this.y+side >= 0 && this.y+side <= BOARD_SIZE-1){
				ReentrantLock l = board[this.y+side][this.x+moveDirection];
				if(l.tryLock()){
					synchronized(moveCount){
						this.y+=side;
						this.x+=moveDirection;
						board[startY][startX].unlock();
						moveCount++;
					}
					return true;
				}else
					return false;
			}else
				return false;
		}
	}
}
