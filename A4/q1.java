package A4;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class q1 {
	
	static int maxEdges;
	static double maxRadius;
	static int numNodes;
	static int numThreads;
	
	volatile static Integer numNodesAdded = 0;
	volatile static int numTasksProcessed; 
	volatile static boolean maxNodesAdded;
	
	static final int ADDED = 0;
	static final int NOT_ADDED = 1;
	static final int FULL = 2;
	
	static Board board = new Board();
	
	public static void main(String[] args) {
		numThreads = Integer.parseInt(args[0]);
		numNodes = Integer.parseInt(args[1]);
		maxEdges = Integer.parseInt(args[2]);
		maxRadius = Double.parseDouble(args[3]);
		
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
		
		while(!maxNodesAdded);
		executor.shutdown();
		while(!executor.isTerminated());
		
		long endTime = System.currentTimeMillis();
		long runTime = endTime - startTime;
		
		System.out.println("Time Elapsed: " + runTime);
		System.out.println("Tasks Processed: " + numTasksProcessed);
	}
	
//	static synchronized boolean updateNodesAdded() {
//		if(numNodesAdded != numNodes) {
//			numNodesAdded++;
//			return true;
//		} else {
//			maxNodesAdded = true;
//			return false;
//		}
//	}
	
	static synchronized void updateTasksProcessed() {
		numTasksProcessed++;
	}
	
	static class Task implements Runnable{
		
		Node node;
		ExecutorService executor;
		
		public Task(Node node, ExecutorService executor) {
			this.node = node;
			this.executor = executor;
		}

		@Override
		public void run() {
			try {
					
				// Generate new node
				Node newNode = new Node(Math.random(),Math.random());
				int addResult = board.addNode(this.node, newNode);
				
				if(addResult == ADDED) {
					this.executor.execute(new Task(newNode,this.executor));
				} 
				
				if (addResult != FULL) {
					this.executor.execute(new Task(this.node,this.executor));
				} 
				
				updateTasksProcessed();
				
			} catch(RejectedExecutionException e) {}
		}
		
	}
	
	static class Board {
		Obstacle[] obstacles = new Obstacle[20];
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		public Board() {
			generateObstacles();
		}
		
		private void generateObstacles() {
			// Generate a random set of obstacles
			for(int i = 0; i < obstacles.length; i++){
				obstacles[i] = new Obstacle(Math.random(),Math.random());
			}
		}
		
		public int addNode(Node fromNode, Node toNode) {
				if(fromNode.edges.size() == maxEdges)
					return FULL;
				
				if(!withinRadius(fromNode,toNode)){
					return NOT_ADDED;
				}
				
				for(Obstacle obstacle : obstacles) {
					if(obstacle.intersects(fromNode, toNode)){
						return NOT_ADDED;
					}
				}
				
				synchronized(numNodesAdded) {
					numNodesAdded++;
					if(numNodesAdded <= numNodes) {
						fromNode.edges.add(toNode);
						return ADDED;
					} else {
						maxNodesAdded = true;
						return NOT_ADDED;
					}
				}
		}
		
		private boolean withinRadius(Node n1, Node n2) {
			double distance = Math.sqrt(Math.pow(n1.y-n2.y, 2) + Math.pow(n1.x-n2.x, 2));
			return distance <= maxRadius;
		}
	}
	
	static class Node {
		double x;
		double y;
		ArrayList<Node> edges = new ArrayList<Node>();
		
		public Node(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}
	
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
		
		public boolean intersects(Node n1, Node n2) {
			if(n1.x == n2.x){
				if(n1.x >= this.left && n1.x <= this.right && ((n1.y > this.top && n2.y <= this.top) || (n1.y < this.bottom && n2.y >= this.bottom)))
					return true;
			} else if(n1.y == n2.y) {
				if(n1.y >= this.bottom && n1.y <= this.top && ((n1.x < this.left && n2.x >= this.left) || (n1.x > this.right && n2.x <= this.right)))
					return true;
			} else {
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
			
			return false;
		}
	}
}
