package A4;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class q1 {
	
	static int maxEdges;
	static double maxRadius;
	static int numNodes;
	static int numThreads;
	
	static AtomicInteger numNodesAdded = new AtomicInteger(0);
	static AtomicInteger numTasksProcessed = new AtomicInteger(0); 
	volatile static boolean maxNodesAdded;
	
	// Different states of a completed task.
	static final int ADDED = 0;
	static final int NOT_ADDED = 1;
	static final int FULL = 2;
	
	static Board board = new Board();
	
	public static void main(String[] args) {
		numThreads = Integer.parseInt(args[0]);
		numNodes = Integer.parseInt(args[1]);
		maxEdges = Integer.parseInt(args[2]);
		maxRadius = Double.parseDouble(args[3]);
		
		// Generate the first node making sure that it is not placed within an obstacle.
		boolean getFirstNode = false;
		Node firstNode;
		do {
			getFirstNode = false;
			firstNode = new Node(Math.random(),Math.random());
			for(Obstacle obstacle : board.obstacles) {
				if(firstNode.x >= obstacle.left && firstNode.x <= obstacle.right && firstNode.y >= obstacle.bottom && firstNode.y <= obstacle.top) {
					getFirstNode = true;
					break;
				}
			}
		} while(getFirstNode); 
		
		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		
		long startTime = System.currentTimeMillis();
		executor.execute(new Task(firstNode,executor));
		
		// Run until the max number of nodes are generated.
		while(!maxNodesAdded);
		executor.shutdownNow();
		while(!executor.isTerminated());
		
		long endTime = System.currentTimeMillis();
		long runTime = endTime - startTime;
		
		// Uncomment the line below to see timing.
//		System.out.println("Time Elapsed: " + runTime);
		System.out.println("Tasks Processed: " + numTasksProcessed.get());
	}

	/**
	 * Task class for adding/growing nodes in the graph.
	 * */
	static class Task implements Runnable{
		
		Node node;
		ExecutorService executor;
		
		public Task(Node node, ExecutorService executor) {
			this.node = node;
			this.executor = executor;
		}

		@Override
		public void run() {
			// If the executor is shutoff/the maximum number of nodes have been created, attempting
			// to execute a new task will throw an error. Catch the error and run no more tasks.
			try {
				
				// Generate new node.
				Node newNode = new Node(Math.random(),Math.random());
				
				// Attempt to add the new node stemming from the current node.
				int addResult = board.addNode(this.node, newNode);
				
				// If the node was successfully added, generate a task for the newly created node.
				if(addResult == ADDED) {
					this.executor.execute(new Task(newNode,this.executor));
				} 
				
				// If the current node is not full, generate a new task and continue growing this node.
				if (addResult != FULL) {
					this.executor.execute(new Task(this.node,this.executor));
				} 
				
				// Increment the number of tasks processed as this task is not finished.
				numTasksProcessed.incrementAndGet();
				
			} catch(RejectedExecutionException e) {}
		}
		
	}
	
	/**
	 * Board class
	 * 
	 * Stores 20 Obstacles and has a list of nodes added to it.
	 * */
	static class Board {
		Obstacle[] obstacles = new Obstacle[20];
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		public Board() {
			generateObstacles();
		}
		
		/**
		 * Function for generating 20 randomly placed Obstacles on the board.
		 * */
		private void generateObstacles() {
			// Generate a random set of obstacles
			for(int i = 0; i < obstacles.length; i++){
				obstacles[i] = new Obstacle(Math.random(),Math.random());
			}
		}
		
		/**
		 * Function for attempting to grow a node with a new node.
		 * 
		 * @param fromNode is the node that this function attempts to grow from.
		 * @param toNode is the node that this function is attempting to add to fromNode.
		 * */
		public int addNode(Node fromNode, Node toNode) {
			
				// If the number of edges stemming from fromNode is at its capacity, tell the 
				// calling function that this node is full.
				if(fromNode.edges.size() == maxEdges)
					return FULL;
				
				// If the generated node does not fall within the radius limit, tell the calling
				// function that it was not able to be added.
				if(!withinRadius(fromNode,toNode)){
					return NOT_ADDED;
				}
				
				// If the generated node interferes with an obstacle on the board OR the edge intersects
				// the board, tell the calling function that it was not able to be added.
				for(Obstacle obstacle : obstacles) {
					if(obstacle.intersects(fromNode, toNode)){
						return NOT_ADDED;
					}
				}
				
				// If the number of nodes added to the board is less than the limit, go ahead and add the node.
				// Otherwise, tell the calling function that it could not be added and send a signal to shut
				// down execution as we have reached the required number of nodes created on the board.
				if(numNodesAdded.incrementAndGet() <= numNodes) {
					fromNode.edges.add(toNode);
					return ADDED;
				} else {
					maxNodesAdded = true;
					return NOT_ADDED;
				}
		}
		
		/**
		 * Function for checking whether the edge between two nodes is within the radius limit.
		 * */
		private boolean withinRadius(Node n1, Node n2) {
			double distance = Math.sqrt(Math.pow(n1.y-n2.y, 2) + Math.pow(n1.x-n2.x, 2));
			return distance <= maxRadius;
		}
	}
	
	/**
	 * Node object for storing x and y position, along with attached edges.
	 * */
	static class Node {
		double x;
		double y;
		ArrayList<Node> edges = new ArrayList<Node>();
		
		public Node(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}
	
	/**
	 * Obstacle object for storing the confines of an obstacle and ultimately determining
	 * where nodes can and cannot be created.
	 * */
	static class Obstacle {
		double left;
		double right;
		double top;
		double bottom;
		
		public Obstacle(double x, double y) {
			this.left = Math.max(x - 0.05,0);	// left
			this.right = Math.min(x + 0.05,1);	// right
			this.top = Math.min(y + 0.05,1);	// top
			this.bottom = Math.max(y - 0.05,0);	// bottom
		}
		
		/**
		 * Function for checking whether an edge between two nodes intersects with this object.
		 * 
		 * @param n1 is one node.
		 * @param n2 is the second node.
		 * */
		public boolean intersects(Node n1, Node n2) {
			
			// If the two nodes are vertically or horizontally aligned, avoid division by zero errors with equation calculations 
			// and check whether they are not intersecting with this object.
			if(n1.x == n2.x){
				if(n1.x >= this.left && n1.x <= this.right && ((n1.y > this.top && n2.y <= this.top) || (n1.y < this.bottom && n2.y >= this.bottom)))
					return true;
			} else if(n1.y == n2.y) {
				if(n1.y >= this.bottom && n1.y <= this.top && ((n1.x < this.left && n2.x >= this.left) || (n1.x > this.right && n2.x <= this.right)))
					return true;
			} else {
				
				// Get the equation of the line formulated by connecting nodes 1 and 2.
				double slope = (n1.y - n2.y)/(n1.x - n2.x);
				double yIntersect = n1.y - slope*n1.x;
				double yLeft = slope*this.left + yIntersect;
				double yRight = slope*this.right + yIntersect;
				double xTop = (this.top-yIntersect)/slope;
				double xBottom = (this.bottom-yIntersect)/slope;
				
				// Check left/right
				if((yLeft >= this.bottom && yLeft <= this.top) && (n1.x < this.left && n2.x >= this.left)) {
					return true;
				} else if((yRight >= this.bottom && yRight <= this.top) && (n1.x > this.right && n2.x <= this.right))
					return true;
				
				// Check top/bottom
				if((xTop >= this.left && xTop <= this.right) && (n1.y > this.top && n2.y <= this.top)) {
						return true;
				} else if((xBottom >= this.left && xBottom <= this.right) && (n1.y < this.bottom && n2.y >= this.bottom)){
					return true;
				}
			}
			
			// There is no intersection if the code reaches this point.
			return false;
		}
	}
}
