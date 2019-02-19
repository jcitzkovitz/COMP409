import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class q1 {

    //Volatile variable for number of moves.
    public static volatile long numMoves;
    //volatile Board.
    public static volatile Board board;
    //Array to store all the threads per piece.
    public static Thread[] allThreads;
    public static long startTime;
    public static int simLength;

    public static void main(String[] args) throws InterruptedException {

        //Check and store arguments.
        if (args.length != 2) {
            System.out.println("Please provide two arguments.");
            System.exit(0);
        }

        //Get the arguments.
        int numPieces = Integer.parseInt(args[0]);

        //Check that number of pieces argument is between 1 and 64 inclusively.
        if (numPieces < 1 || numPieces > 64) {
            System.out.println("Invalid number of pieces (first argument).");
            System.exit(0);
        }


        simLength = Integer.parseInt(args[1]);

        //Initialize board.
        board = new Board();

        //Place all the initial pieces;
        placeInitialPieces(numPieces);

        startTime = System.currentTimeMillis();

        for (int i = 0; i < allThreads.length; i++) {
            allThreads[i].start();
        }

        int printCounter = 0;
        while(printCounter < simLength) {
            int currentSecond = (int) (System.currentTimeMillis() - startTime) / 1000;
            if (currentSecond > printCounter) {
                System.out.println("Number of moves at " + currentSecond + " is " + numMoves);
                printCounter = currentSecond;
            }
        }
        System.out.println("Total number of moves : " + numMoves);


	}

	//Method to place the initial pieces on the board and put each piece in the thread array.
	private static void placeInitialPieces(int numberOfPieces) {
        Piece piece;
        //Initialize data structure for threads.
        allThreads = new Thread[numberOfPieces];
        for (int i = 0; i < numberOfPieces; i++) {
            double probability = Math.random();
            //50% change of creating a knight or a queen.
            if (probability >= 0.5) {
                piece = new Piece(true, i);
            } else {
                piece = new Piece(false, i);
            }

            //initialize placement to false.
            boolean isPlaced = false;

            //Until we find an unoccupied tile.
            while (!isPlaced) {
                //Generate random x and y positions.
                Random rand = new Random();
                //Generate a random position on the board.
                int xPos = rand.nextInt(8);
                int yPos = rand.nextInt(8);
                //If it's not occupied we go there.
                boolean status = board.tiles[xPos][yPos].goTo();
                if (status) {
                    piece.setCurrentPosition(board.tiles[xPos][yPos]);
                    board.piecesList.add(piece);
                    isPlaced = true;
                    allThreads[i] = new Thread(piece);
                }

            }
        }
    }

    //Method to increment number of moves atomically.
	synchronized public static void incNumMoves() {
        numMoves++;
    }

    //Class to represents chess square.
	static class Tile {

        private boolean isOccupied;
        private final int x;
        private final int y;

        public Tile(int x, int y) {
            isOccupied = false;
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public boolean getIsOccupied() {
            return isOccupied;
        }

        //Synchronized method to allow a piece to travel to this cell.
        synchronized public boolean goTo() {
            boolean status;
            if (isOccupied) {
                status = false;
            }
            else {
                isOccupied = true;
                status = true;
            }
            return status;
        }

        synchronized public void leave() {
            isOccupied = false;
        }
    }

    //Class to organize Tiles into a board. Has a double array of Tiles and a List of all pieces on the board.
	static class Board {
        List<Piece> piecesList;
        Tile[][] tiles;

        //Board constructor.
        public Board() {
            tiles = new Tile[8][8];
            piecesList = new ArrayList<Piece>();
            for (int i = 0; i < tiles.length; i++) {
                for (int j = 0; j < tiles[0].length; j++) {
                    tiles[i][j] = new Tile(i,j);
                }
            }
        }
    }

    //Runnable class representing the piece. Can be a Knight or a queen.
    //The run() method will be the one displacing the pieces all across the board.
    static class Piece implements Runnable {
        private boolean isKnight;
        private int id;
        private Tile currentPosition;

        public int getId() {
            return id;
        }

        //Position initialized to null.
        public Piece(boolean isKnight, int id) {
            this.isKnight = isKnight;
            this.currentPosition = null;
            this.id = id;
        }

        public boolean getIsKnight() {
            return this.isKnight;
        }

        public void setCurrentPosition(Tile tile) {
            currentPosition = tile;
        }

        public Tile getCurrentPosition() {
            return currentPosition;
        }

        public int[] getKnightMove(int curX,int curY) {
            //This array will store the result.
            int[] result = new int[2];

            //Knight has a maximum of 8 moves at all times.
            int[][]  allMoves = {{-2,1}, {-1,2}, {1,2}, {2,1}, {2, -1}, {1, -2}, {-1,-2}, {-2, -1}};

            //Store only moves within legal bounds in the ArrayList legalMoves.
            ArrayList<int[]> legalMoves = filterOutOfBounds(curX, curY, allMoves);

            //Get the next move randomly.
            Random rand = new Random();
            int nextMoveIndex = rand.nextInt(legalMoves.size());
            result[0] = legalMoves.get(nextMoveIndex)[0];
            result[1] = legalMoves.get(nextMoveIndex)[1];

            return result;
        }

        //Returns the target space and the direction in which the Queen is going.
        public int[][] getQueenMove(int curX, int curY) {
            int[][] result = new int[2][2];
            Random rand = new Random();
            int[][] allDirections = {{1,0},{1,1},{0,1},{-1,-1},{-1,0},{-1,-1},{0,-1},{1,-1}};
            ArrayList<int[]> legalDirections = filterOutOfBounds(curX, curY, allDirections);

            //Pick a random direction.
            int index = rand.nextInt(legalDirections.size());
            int[] direction = legalDirections.get(index);
            result[1] = direction;
            int steps = rand.nextInt(7) + 1;
            int nextX = direction[0] * steps;
            int nextY = direction[1] * steps;

            //Generate new step number until we get a location that is on the board.
            while (!(nextX < 8 && nextX > -1 && nextY < 8 && nextY < -1)) {
                steps = rand.nextInt(7) + 1;
                nextX = direction[0] * steps;
                nextY = direction[1] * steps;
            }
            result[0][0] = nextX;
            result[0][1] = nextY;

            return result;
        }

        //Function that filters out all out of bounds moves from random move options.
        public static ArrayList<int[]> filterOutOfBounds(int x, int y, int[][] moves) {
            ArrayList<int[]> results = new ArrayList<>();
            for (int i = 0; i<moves.length; i++) {
                int nextX = x + moves[i][0];
                int nextY = y + moves[i][1];

                if(nextX < 8 && nextX > -1  && nextY < 8 && nextY > -1) {
                    results.add(new int[]{nextX, nextY});
                }
            }
            return results;
        }

        public boolean move() {
            boolean result = false;
            //Knight scenario. 
            if (this.isKnight) {
                int currentX = this.getCurrentPosition().getX();
                int currentY = this.getCurrentPosition().getY();
                int[] nextPosition = getKnightMove(currentX, currentY);
                int targetX = nextPosition[0];
                int targetY = nextPosition[1];
                boolean status = board.tiles[targetX][targetY].goTo();
                if (status) {
                    this.setCurrentPosition(board.tiles[targetX][targetY]);
                    board.tiles[currentX][currentY].leave();
                    incNumMoves();
                    result = true;
                }
                else {
                    result = false;
                }
            }
            //Queen scenario.
            else {
                int currentX = this.getCurrentPosition().getX();
                int currentY = this.getCurrentPosition().getY();
                int[][] nextPosition = getQueenMove(currentX, currentY);
                int targetX = nextPosition[0][0];
                int targetY = nextPosition[0][1];
                int[] direction = nextPosition[1];
               // while (currentX != targetX && currentY != targetY) {

                }

                
            return result;
        }


        @Override
        public void run() {
            while (System.currentTimeMillis() < startTime + (simLength * 1000)) {
                while (!move()) {
                    if (System.currentTimeMillis() > startTime + (simLength * 1000)) {
                        return;
                    }
                }

                //Generate random int between 10 and 30.
                Random rand = new Random();
                int sleepProb =  rand.nextInt(20) + 10;
                try {
                    Thread.sleep(sleepProb);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }
    }
}