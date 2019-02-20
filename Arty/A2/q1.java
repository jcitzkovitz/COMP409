import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class q1 {

    //Volatile variable for number of moves.
    public static long numMoves;
    //volatile Board.
    public static volatile Board board;
    //Array to store all the threads per piece.
    public static Thread[] allThreads;

    //Global variables for beginning of program run and simulation length.
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

        //Parse simulation length.
        simLength = Integer.parseInt(args[1]);

        //Initialize board.
        board = new Board();

        //Place all the initial pieces and prepare threads.
        placeInitialPieces(numPieces);

        //Start timer.
        startTime = System.currentTimeMillis();

        //Start all threads.
        for (int i = 0; i < allThreads.length; i++) {
            allThreads[i].start();
        }

        //Print counter procedure every second.
        int printCounter = 0;
        while(printCounter < simLength) {
            int currentSecond = (int) (System.currentTimeMillis() - startTime) / 1000;
            if (currentSecond > printCounter) {
                System.out.println("Number of moves at " + currentSecond + " is " + numMoves);
                printCounter = currentSecond;
            }
        }
        //Final number of moves print.
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

            //Initialize placement to false.
            boolean isPlaced = false;

            //Until we find an unoccupied tile.
            while (!isPlaced) {

                //Generate random x and y positions.
                Random rand = new Random();

                //Generate a random position on the board.
                int xPos = rand.nextInt(8);
                int yPos = rand.nextInt(8);

                //If it's not occupied we go there and set it to occupy.
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

    //Class to represents Chess square.
	static class Tile {

        //Atributes. Square locations are final.
        private boolean isOccupied;
        private final int x;
        private final int y;

        //Tile constructor.
        public Tile(int x, int y) {
            isOccupied = false;
            this.x = x;
            this.y = y;
        }

        //Getter methods for coordinates.
        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        //Getter method for occupancy.
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

        //Keep tile information volatile
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
        private final int id;
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

        //Setter for Piece position.
        public void setCurrentPosition(Tile tile) {
            currentPosition = tile;
        }

        //Gets current Position of piece.
        public Tile getCurrentPosition() {
            return currentPosition;
        }

        //Returns a legal knight move from provided coordinates.
        public int[] getKnightMove(int curX,int curY) {
            //This array will store the result.
            int[] result = new int[2];

            //Knight has a maximum of 8 moves at all times.
            int[][]  allMoves = {{-2,1}, {-1,2}, {1,2}, {2,1}, {2, -1}, {1, -2}, {-1,-2}, {-2, -1}};

            //Store only moves within legal bounds in the ArrayList legalMoves.
            ArrayList<ArrayList<int[]>> movesAndDirections = filterOutOfBounds(curX, curY, allMoves);
            ArrayList<int[]> legalMoves = movesAndDirections.get(0);



            //Get the next move randomly.
            Random rand = new Random();
            int nextMoveIndex = rand.nextInt(legalMoves.size());
            result[0] = legalMoves.get(nextMoveIndex)[0];
            result[1] = legalMoves.get(nextMoveIndex)[1];

            return result;
        }

        //Returns a legal target space and the direction in which the Queen is going.
        public int[][] getQueenMove(int curX, int curY) {
            //Datastructure to store result.
            int[][] result = new int[2][2];



            //The Queen has 8 possible directions.
            int[][] allDirections = {{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1}};

            //This data structure will return the moves and directions in which the queen can go.
            //It ensures the moves remain within the bounds of the board.
            ArrayList< ArrayList<int[]>> movesAndDirections = filterOutOfBounds(curX, curY, allDirections);
            ArrayList<int[]> legalDirections = movesAndDirections.get(1);

            //Initialize random for random number generator and pick a random valid direction.
            Random rand = new Random();
            int index = rand.nextInt(legalDirections.size());
            int[] direction = legalDirections.get(index);

            //Store the direction.
            result[1] = direction;

            //Get a random number of steps and get target space from that number.
            int steps = rand.nextInt(7) + 1;
            int nextX = curX + (direction[0] * steps);
            int nextY = curY + (direction[1] * steps);

            //Generate new step number until we get a location that is on the board.
            while (!(nextX < 8 && nextX > -1 && nextY < 8 && nextY > -1)) {
                steps = rand.nextInt(7) + 1;
                nextX = curX + (direction[0] * steps);
                nextY = curY + (direction[1] * steps);
            }

            //Store the target space. Direction of that space from current square is already stored.
            result[0][0] = nextX;
            result[0][1] = nextY;

            return result;
        }

        //Function that filters out all out of bounds moves from random move options.
        public static ArrayList<ArrayList<int[]>> filterOutOfBounds(int x, int y, int[][] moves) {

            //Initialize result data structures.
            ArrayList<int[]> results = new ArrayList<>();
            ArrayList<int[]> keptMoves = new ArrayList<>();

            // For each direction provided, make sure there is at least one tile between the current space and the border.
            // If there aren't we do not consider that direction, as we cannot move there.
            for (int i = 0; i < moves.length; i++) {
                int nextX = x + moves[i][0];
                int nextY = y + moves[i][1];

                if(nextX < 8 && nextX > -1  && nextY < 8 && nextY > -1) {
                    results.add(new int[]{nextX, nextY});
                    keptMoves.add(moves[i]);
                }
            }
            //Store all resulting legal target spaces and their directions and return them.
            ArrayList<ArrayList<int[]>> allResults = new ArrayList<>();
            allResults.add(results);
            allResults.add(keptMoves);
            return allResults;
        }

        //This method returns true if the piece succesfully did a random legal move, and false otherwise.
        public boolean move() {


            //get current piece location.
            int currentX = this.getCurrentPosition().getX();
            int currentY = this.getCurrentPosition().getY();

            //Knight scenario. 
            if (this.isKnight) {

                //Get a random target space.
                int[] nextPosition = getKnightMove(currentX, currentY);
                int targetX = nextPosition[0];
                int targetY = nextPosition[1];

                //Attempt to lock the target space. If successful, go there. Else, return false.
                boolean status = board.tiles[targetX][targetY].goTo();
                if (status) {
                    this.setCurrentPosition(board.tiles[targetX][targetY]);
                    board.tiles[currentX][currentY].leave();
                    incNumMoves();
                    return true;
                }
                else {
                    return false;
                }
            }
            //Queen scenario.
            else {
                //Get a legal queen move.
                int[][] nextPosition = getQueenMove(currentX, currentY);

                //Store target space and direction in which it is.
                int targetX = nextPosition[0][0];
                int targetY = nextPosition[0][1];
                int[] direction = nextPosition[1];

                //Initiliaze step counter.
                int stepsCounter = 0;

                //Until we arrive at our target space, lock every square in our path to prevent being blocked.
                while (currentX != targetX && currentY != targetY) {

                    //Get one step closer to target and acquire lock.
                    int nextX = currentX + direction[0];
                    int nextY = currentY + direction[1];
                    boolean status = board.tiles[nextX][nextY].goTo();


                    if (status) {

                        //If successful, we move to target and set our current position to destination.
                        if (nextX == targetX && nextY == targetY) {
                            this.setCurrentPosition(board.tiles[nextX][nextY]);
                            incNumMoves();
                            //We navigate backwards until our start position to release the lock.
                            while (stepsCounter >= 0) {
                                board.tiles[currentX][currentY].leave();
                                currentX = currentX - direction[0];
                                currentY = currentY - direction[1];
                                stepsCounter--;
                            }
                            //Method was successful.
                            return true;

                        }
                        //If we didn't arrive to our destination but the next tile was free, increment step counter and
                        //update tile location.
                        else {
                            stepsCounter++;
                            currentX = currentX + direction[0];
                            currentY = currentY + direction[1];
                        }
                    }

                    //IF we did not acquire the lock.
                    else {

                        //If we already moved but did not get to our initally target position, travel to here
                        // and count as successful move. This still counts as a random move by the queen.
                        if (stepsCounter > 0) {

                            //Update current position.
                            this.setCurrentPosition(board.tiles[currentX][currentY]);
                            incNumMoves();

                            //Unlock all previous tiles on our path.
                            while (stepsCounter >= 0) {
                                board.tiles[currentX][currentY].leave();
                                currentX = currentX - direction[0];
                                currentY = currentY - direction[1];
                                stepsCounter--;
                            }
                            return true;
                        }
                        //Return false if we are directly blocked by another piece.
                        else {
                            return false;
                        }
                    }


                }

            }
            //Return false by default
            return false;
        }


        @Override
        public void run() {
            //While there is time to execute, try to move this piece.
            while (System.currentTimeMillis() < startTime + (simLength * 1000)) {
                while (!move()) {
                    //If we have entered this thread but have failed to move the piece, check timer again.
                    //This prevents threads from looping forever after thread entered but started executing.
                    if (System.currentTimeMillis() > startTime + (simLength * 1000)) {
                        return;
                    }
                }

                //Generate random int between 10 and 30 and sleep.
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