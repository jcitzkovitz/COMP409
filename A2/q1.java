package A2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class q1 {

	static final int BOARD_SIZE = 8;
	static int numPieces;
	static ChessPiece[] pieces;
	static volatile Semaphore[][] board;
	static volatile boolean run;
	static volatile int moveCount;

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
				numPieces = Integer.parseInt(args[0]);
				runTime = Double.parseDouble(args[1]);
			} catch (NumberFormatException e){
				System.out.println(invalidArgs);
				System.exit(0);
			}

			board = new Semaphore[BOARD_SIZE][BOARD_SIZE];
			pieces = new ChessPiece[numPieces];
			printLock = new Semaphore(1);
			printReadyLock = new Semaphore(1);

			int xPos = 0;
			int yPos = -1;
			int i = 0;
			while(i < numPieces){
				xPos = i%BOARD_SIZE;
				if(xPos == 0)
					yPos++;
				ChessPiece piece = Math.random() <= 0.5 ? new Queen(xPos,yPos,i) : new Knight(xPos,yPos,i);
				pieces[i] = piece;
				board[yPos][xPos] = piece;
				i++;
			}

			for(i = 0; i < BOARD_SIZE; i++){
				for(int j = 0; j < BOARD_SIZE; j++){
					if(board[i][j] == null){
						board[i][j] = new Semaphore(1);
					}
				}
			}

			printBoard();

			run = true;
			ExecutorService executor = Executors.newFixedThreadPool(numPieces+1);
			for(i = 0; i < numPieces; i++){
				executor.execute(pieces[i]);
			}

			executor.execute(new PrintCount());
			long start = System.currentTimeMillis();
			while(System.currentTimeMillis()-start < 1000*runTime);
			run = false;

			executor.shutdown();
			while (!executor.isTerminated());

			System.out.println("Final moves made: "+getMoveCount());
			printBoard();
		}
	}

	synchronized static void updateCount(){
		moveCount++;
	}

	synchronized static int getMoveCount(){
		return moveCount;
	}

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

	static class PrintCount implements Runnable{
		public void run(){
			while(run){

				synchronized(printReadyLock){
					try {
//						System.out.println("Ready to print...");
						printReady = true;
						printReadyLock.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				synchronized(printLock){}

				System.out.println("Moves made at current stage: "+getMoveCount());
//				printBoard();

				printReady = false;
				synchronized(printLock){
					printLock.notifyAll();
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	static abstract class ChessPiece extends Semaphore implements Runnable{
		protected int x;
		protected int y;
		protected int id;

		public ChessPiece(int x, int y, int id){
			super(1);
			this.x = x;
			this.y = y;
			this.id = id;
			try {
				super.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		public abstract void run();
		volatile int syncCount = 0;
		protected void printCount(){
			if(printReady){
//				System.out.println(++this.syncCount+" waiting at sync...");
				synchronized(printLock){
					try {
						numPrintReady++;
						if(numPrintReady == numPieces){
							synchronized(printReadyLock){
								numPrintReady = 0;
//								System.out.println("Notify printer!");
								printReadyLock.notifyAll();
							}
						}
//						System.out.println(numPrintReady+" waiting for printLock - total: ");
						printLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	static class Queen extends ChessPiece {

		public Queen(int x, int y,int id){
			super(x,y,id);
		}

		@Override
		public void run() {
			while(run){

				printCount();

				int randomDir = (int) Math.floor((4*Math.random()));
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


				if(steps == 0){
					continue;
				}
				else{
					updateCount();
					try {
						Thread.sleep((long) (10+20*Math.random()));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

		}

		private int moveRow(boolean posDirection){
			int steps = randomStep(this.y,posDirection);
			int stepsCovered = 0;
			int sign = posDirection ? 1 : -1;
			int startPos = this.y;
			Semaphore prev = board[this.y][this.x];

			while(stepsCovered < steps){
				Semaphore l = board[startPos+sign*(stepsCovered+1)][this.x];
				if(l.tryAcquire()){
					stepsCovered++;
					prev = l;
				}else{
					break;
				}
			}

			board[startPos+sign*(stepsCovered)][this.x] = this;
			board[this.y][this.x] = prev;
			this.y = startPos+sign*stepsCovered;
			for(int i = 0; i < stepsCovered; i++){
				try{
					board[startPos+sign*i][this.x].release();;
				}catch(Exception e){
					System.out.println("ROW: is queen in T"+this.id+"? "+(board[startPos+sign*i][this.x] instanceof Queen));
					System.exit(0);
				}
			}

			return stepsCovered;
		}

		private int moveCol(boolean posDirection){
			int steps = randomStep(this.x,posDirection);
			int stepsCovered = 0;
			int sign = posDirection ? 1 : -1;
			int startPos = this.x;
			Semaphore prev = board[this.y][this.x];

			while(stepsCovered < steps){
				Semaphore l = board[this.y][startPos+sign*(stepsCovered+1)];
				if(l.tryAcquire()){
					stepsCovered++;
					prev = l;
				}else{
					break;
				}
			}

			board[this.y][startPos+sign*(stepsCovered)] = this;
			board[this.y][this.x] = prev;
			this.x = startPos+sign*stepsCovered;
			for(int i = 0; i < stepsCovered; i++){
				try{
					board[this.y][startPos+sign*i].release();
				}catch(Exception e){
					System.out.println("COL: is queen in T"+this.id+"? "+(board[this.y][startPos+sign*i] instanceof Queen));
					System.exit(0);
				}
			}

			return stepsCovered;
		}

		private int randomStep(int start, boolean posDirection){
			double rand = Math.random();
			rand = rand == 0.0 ? 1.0 : rand;
			if(posDirection)
				return (int) Math.ceil((BOARD_SIZE-start-1)*rand);
			else
				return (int) Math.ceil(start*rand);
		}
	}

	static class Knight extends ChessPiece {

		private int x;
		private int y;

		public Knight(int x, int y, int id){
			super(x,y,id);
		}

		@Override
		public void run() {
			while(run){

				printCount();

				int randomDir = (int) Math.floor((4*Math.random()));
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


				if(!didStep){
					continue;
				}
				else{
					updateCount();
					try {
						Thread.sleep((long) (10+20*Math.random()));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

		private boolean moveRow(boolean posDirection, boolean leftSide){
			int moveDirection = posDirection ? 3 : -3;
			int side = leftSide ? -1 : 1;

			if(this.y+moveDirection >= 0 && this.y+moveDirection <= BOARD_SIZE-1){
				if(this.x+side >= 0 && this.x+side <= BOARD_SIZE-1){
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
			}else
				return false;
		}

		private boolean moveCol(boolean posDirection, boolean downSide){
			int moveDirection = posDirection ? 3 : -3;
			int side = downSide ? -1 : 1;

			if(this.x+moveDirection >= 0 && this.x+moveDirection <= BOARD_SIZE-1){
				if(this.y+side >= 0 && this.y+side <= BOARD_SIZE-1){
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
			}else
				return false;
		}
	}
}
