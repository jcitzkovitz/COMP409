import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class q1 {
	
	static final int BOARD_SIZE = 8;
	static ChessPiece[] pieces;
	static volatile ReentrantLock[][] board;
	static volatile boolean run;
	static volatile int moveCount;

	public static void main(String[] args){
		String invalidArgs = "You must enter 2 integer arguments:\n1) Number of pieces \n2) Simulation time in seconds";
		if(args.length < 2){
			System.out.println(invalidArgs);
		}else{
			int numPieces = 0;
			int runTime = 0;
			try{
				numPieces = Integer.parseInt(args[0]);
				runTime = Integer.parseInt(args[1]);
			} catch (NumberFormatException e){
				System.out.println(invalidArgs);
				System.exit(0);
			}

			board = new ReentrantLock[BOARD_SIZE][BOARD_SIZE];
			pieces = new ChessPiece[numPieces];

			int i = 0;
			while(i < numPieces){
				int x = randomPos();
				int y = randomPos();
				if(board[y][x] == null){
					ChessPiece piece = Math.random() <= 0.5 ? new Queen(x,y) : new Knight(x,y);
					pieces[i] = piece;
					board[y][x] = piece;
					i++;
				}
			}

			for(i = 0; i < BOARD_SIZE; i++){
				for(int j = 0; j < BOARD_SIZE; j++){
					if(board[i][j] != null)
						board[i][j] = new ReentrantLock();
				}
			}

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

			System.out.println("Final moves made: "+getCount());
		}
	}

	private static int randomPos(){
		return (int) ((BOARD_SIZE-1)*Math.random());
	}

	synchronized static void updateCount(){
		moveCount++;
	}

	static int getCount(){
		return moveCount;
	}

	static class PrintCount implements Runnable{
		public void run(){
			while(run){
				for(int i = 0; i < pieces.length; i++)
					try {
						pieces[i].wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				System.out.println("Moves made: "+getCount());

				for(int i = 0; i < pieces.length; i++)
					pieces[i].notify();

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	static abstract class ChessPiece extends ReentrantLock implements Runnable{
		protected int x;
		protected int y;

		public ChessPiece(int x, int y){
			this.x = x;
			this.y = y;
			this.lock();
		}

		public abstract void run();
	}

	static class Queen extends ChessPiece {

		public Queen(int x, int y){
			super(x,y);
		}

		@Override
		public void run() {

			while(run){
				int randomDir = (int) (1.0+Math.ceil(3*Math.random()));
				int steps = 0;

				// Up
				if(randomDir == 1)
					steps = moveUp();
				// Down
				else if(randomDir == 2)
					steps = moveDown();
				// Left
				else if(randomDir == 3)
					steps = moveLeft();
				// Right
				else if(randomDir == 4)
					steps = moveRight();

				if(steps == 0){
					continue;
				}
				else{
					updateCount();
					try {
						Thread.sleep((long) (10+20*Math.random()));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		}

		private int moveUp(){
			int steps = randomStep(y,false);
			int stepsCovered = 0;

			while(stepsCovered+1 < steps && board[y-(stepsCovered+1)][x].tryLock())
				stepsCovered++;

			ReentrantLock temp = board[y-stepsCovered][x];
			board[y-stepsCovered][x] = this;
			board[y][x] = temp;

			for(int i = 0; i < stepsCovered; i++)
				board[y-i][x].unlock();

			this.y = y-stepsCovered;

			return stepsCovered;
		}

		private int moveDown(){
			int steps = randomStep(y,true);
			int stepsCovered = 0;

			while(stepsCovered+1 < steps && board[y+(stepsCovered+1)][x].tryLock())
				stepsCovered++;

			ReentrantLock temp = board[y+stepsCovered][x];
			board[y+stepsCovered][x] = this;
			board[y][x] = temp;

			for(int i = 0; i < stepsCovered; i++)
				board[y+i][x].unlock();

			this.y = y+stepsCovered;

			return stepsCovered;
		}

		private int moveLeft(){
			int steps = randomStep(x,false);
			int stepsCovered = 0;

			while(stepsCovered+1 < steps && board[y][x-(stepsCovered+1)].tryLock())
				stepsCovered++;

			ReentrantLock temp = board[y][x-stepsCovered];
			board[y][x-stepsCovered] = this;
			board[x][y] = temp;

			for(int i = 0; i < stepsCovered; i++)
				board[y][x-i].unlock();

			this.x = x-stepsCovered;

			return stepsCovered;
		}

		private int moveRight(){
			int steps = randomStep(x,true);
			int stepsCovered = 0;

			while(stepsCovered+1 < steps && board[y][x+(stepsCovered+1)].tryLock())
				stepsCovered++;

			ReentrantLock temp = board[y][x+stepsCovered];
			board[y][x+stepsCovered] = this;
			board[y][x] = temp;

			for(int i = 0; i < stepsCovered; i++)
				board[y][x+i].unlock();

			this.x = x+stepsCovered;

			return stepsCovered;
		}

		private int randomStep(int start, boolean posDirection){
			double rand = Math.random();
			rand = rand == 0.0 ? 1 : rand;
			if(posDirection)
				return (int) Math.ceil((BOARD_SIZE-start-1)*rand);
			else
				return (int) Math.ceil(start*rand);
		}
	}

	static class Knight extends ChessPiece {

		private int x;
		private int y;

		public Knight(int x, int y){
			super(x,y);
		}

		@Override
		public void run() {

		}

	}

}
