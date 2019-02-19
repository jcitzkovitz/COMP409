package A2;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;

class Point {
    double x,y;

    // Set of edges attached to this point
    public ArrayList<Edge> edges;

    public Point(double bx,double by) { x = bx; y = by; }

    // Returns true if the given point is the same as this one.
    // nb: should use machine epsilon.
    public boolean same(Point b) { return (x == b.x && y == b.y); }

    // Add an edge connection if not present; lazily creates the edge array/
    public void addEdge(Edge e) {
        if (edges==null) edges = new ArrayList<Edge>();
        if (!edges.contains(e))
            edges.add(e);
    }

    public String toString() {
        return "("+x+","+y+")";
    }

    /**
     * Get angle between two vectors, including this point
     *
     * @param Point p is a point on one vector
     * @param Point q is a point on the other vector
     *
     * @return the angle between the two vectors
     * */
    public double getAngle(Point p, Point q){
        double uLen = Math.sqrt(Math.pow(p.x-this.x,2) + Math.pow(p.y-this.y, 2));
        double vLen = Math.sqrt(Math.pow(q.x-this.x,2) + Math.pow(q.y-this.y, 2));
        double dotProduct = (p.x-this.x)*(q.x-this.x) + (p.y-this.y)*(q.y-this.y);
        return Math.acos(dotProduct/(uLen*vLen));
      }
}

/**
 * Class for a pair of Points
 * */
class PointPair {
	Point p1;
	Point p2;

	public PointPair(Point p1, Point p2){
		this.p1 = p1;
		this.p2 = p2;
	}

	public String toString(){
		return "<("+p1.x+","+p1.y+"),("+p2.x+","+p2.y+")>";
	}
}

class Edge extends Semaphore{
    Point p,q;

    public Edge(Point p1,Point p2) { super(1); p=p1; q=p2; }
    // Utility routine -- 2d cross-product (signed area of a triangle) test for orientation.
    public int sat(double p0x,double p0y,double p1x,double p1y,double p2x,double p2y) {
        double d = (p1x-p0x)*(p2y-p0y)-(p2x-p0x)*(p1y-p0y);
        if (d<0) return -1;
        if (d>0) return 1;
        return 0;
    }

    // Returns true if the given edge intersects this edge.
    public boolean intersects(Edge e) {
        int s1 = sat(p.x,p.y, q.x,q.y, e.p.x,e.p.y);
        int s2 = sat(p.x,p.y, q.x,q.y, e.q.x,e.q.y);
        if (s1==s2 || (s1==0 && s2!=0) || (s2==0 && s1!=0)) return false;
        s1 = sat(e.p.x,e.p.y, e.q.x,e.q.y, p.x,p.y);
        s2 = sat(e.p.x,e.p.y, e.q.x,e.q.y, q.x,q.y);
        if (s1==s2 || (s1==0 && s2!=0) || (s2==0 && s1!=0)) return false;
        return true;
    }

    public String toString() {
        return "<"+p+","+q+">";
    }

    public boolean equals(Edge f){
    	return this.p.same(f.p) && this.q.same(f.q);
    }

    /**
     * Function that chooses the point on an edge that is not the one specified
     *
     * @param Point p is the point
     * */
    public Point getOppositePoint(Point p){
    	return this.p.same(p) ? this.q : this.p;
    }
}


public class q2 {

    public static int n,t; // constants
    public static volatile int flipCount = 0;
    public static Point[] points;
    public static volatile ArrayList<Edge> edges = new ArrayList<Edge>();

    // Use a lock for any thread that is attempting to flip an edge
    public static Semaphore flipLock = new Semaphore(1);

    // Maintain a list of removed edges so that threads that are looking at a removed edge
    // realize that it has in fact been removed.
    public static ArrayList<Edge> removedEdges = new ArrayList<Edge>();

    // Returns true if any existing edge intersects this one
    public static boolean intersection(Edge f) {
        for (Edge e : edges) {
            if (f.intersects(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     *  Returns true if the number of edges this edge crosses is exactly equal to 1 and is the edge that is to be removed
     *
     *  @param Edge f is the edge that we would like to know if it crosses exactly 1 edge and the edge to be removed
     *  @param Edge rem is the edge that is to be removed
     * */
    public static boolean isCandidatePair(Edge f, Edge rem) {
    	if(f.intersects(rem)){
    		int count = 0;
            for (int i = 0; i < edges.size(); i++) {
                if (f.intersects(edges.get(i))) {
                    count++;
                    if(count > 1)
                    	return false;
                }
            }

            if(count == 1)
            	return true;
    	}
    	return false;
    }

    /**
     * Synchronized function for updating the flipCount
     * */
    public static synchronized void updateFlipCount(){
    	flipCount++;
    }

    public static void main(String[] args) {
        try {
            Random r;
            n = Integer.parseInt(args[0]);
            t = Integer.parseInt(args[1]);
            if (args.length>2) {
                r = new Random(Integer.parseInt(args[2]));
            } else {
                r = new Random();
            }
            points = new Point[n];

            // First, create a set of unique points
            // Our first 4 points are the outer corners.  This is not really necessary, but is
            // intended to give us a fixed convex hull so it's easier to see if the alg is working.
            points[0] = new Point(0.0,0.0);
            points[1] = new Point(0.0,1.0);
            points[2] = new Point(1.0,1.0);
            points[3] = new Point(1.0,0.0);
            for (int i=4;i<n;i++) {
                boolean repeat;
                Point np = null;
                do {
                    repeat = false;
                    np = new Point(r.nextDouble(),r.nextDouble());
                    // Verify it is a distinct point.
                    for (int j=0;j<i;j++) {
                        if (np.same(points[j])) {
                            repeat = true;
                            break;
                        }
                    }
                } while(repeat);
                points[i] = np;
            }

            System.out.println("Generated points");

            // Triangulate

            for (int i=0;i<n;i++) {
                for (int j=i+1;j<n;j++) {
                    Edge e = new Edge(points[i],points[j]);
                    if (!intersection(e)) {
                        edges.add(e);
                        e.p.addEdge(e);
                        e.q.addEdge(e);
                    }
                }
            }
            System.out.println("Triangulated: "+n+" points, "+edges.size()+" edges");

            ExecutorService executor = Executors.newFixedThreadPool(t);

            // Execute the Delaunay threads.
 			for(int i = 0; i < t; i++){
 				executor.execute(new Delaunay());
 			}

 			executor.shutdown();
 			while (!executor.isTerminated());

            System.out.println("Flips: "+flipCount);

        } catch (Exception e) {
            System.out.println("ERROR " +e);
            e.printStackTrace();
        }
    }

    /**
     * Thread class that runs Delaunay triangulation on the given set of edges
     * */
    static class Delaunay implements Runnable{

    	 /**
    	  * Get all pair of points that may be used for flipping associated with Edge e
    	  *
    	  * @param Edge e is the edge that is being inspected for flip candidates.
    	  * */
    	 private ArrayList<PointPair> getFlipCandidates(Edge e){

    	    	ArrayList<PointPair> candidates = new ArrayList<PointPair>();
    	    	ArrayList<Point> connectingPoints = new ArrayList<Point>();

    	    	// Retrieve all points that connect point p and q by an edge other than their respective edge.
    	    	for(int i = 0; i < e.p.edges.size(); i++){
    	    		Edge pe = e.p.edges.get(i);
    	    			for(int j = 0; j < e.q.edges.size(); j++){
    	        			Edge qe = e.q.edges.get(j);
	        				if(!qe.equals(pe)){
	            				Point pePoint = pe.getOppositePoint(e.p);
	            				Point qePoint = qe.getOppositePoint(e.q);
	            				if(pePoint.same(qePoint))
	            					connectingPoints.add(pePoint);
	            			}
    	    		}
    	    	}

    	    	// Permute the points retrieved from the previous step and test id they are real candidates for flipping.
    	    	// A candidate pair creates an edge that only crosses the edge that is to be removed.
    	    	for(int i = 0; i < connectingPoints.size(); i++){
    	    		for(int j = i+1; j < connectingPoints.size(); j++){
    	    			PointPair pair = new PointPair(connectingPoints.get(i),connectingPoints.get(j));
    	    			Edge f = new Edge(pair.p1,pair.p2);
    	    			if(isCandidatePair(f,e))
    	    				candidates.add(pair);
    	    		}
    	    	}
    	    	return candidates;
    	}

    	/**
    	 * Checks whether a pair of points is attached to a specific edge by an edge that has been removed from the graph
    	 *
    	 * @param Edge e is the edge to be examined.
    	 * @param PointPair p is the pair of points to be examined.
    	 * */
    	private boolean isStillFlippable(Edge e, PointPair pair){
    		Edge test1 = new Edge(e.p,pair.p1);
    		Edge test2 = new Edge(e.q,pair.p1);
    		Edge test3 = new Edge(e.p,pair.p2);
    		Edge test4 = new Edge(e.q, pair.p2);

    		// Check if the edges are in the removedEdges list, and if so return false.
    		for(int i = 0; i < removedEdges.size(); i++){
    			Edge r = removedEdges.get(i);
    			if(r.equals(test1) || r.equals(test2) || r.equals(test3) || r.equals(test4))
    				return false;
    		}
    		return true;
    	}

    	/**
    	 * Function that does all necessary actions for flipping an edge
    	 *
    	 * @param Edge remEdge is the edge to be removed
    	 * @param PointPair is the pair of points that are to form a new Edge in the graph.
    	 * */
        private void flipEdge(Edge remEdge, PointPair addPair){

        	// Remove the edge to be flipped from the graph and add it to the list of removed edges.
        	edges.remove(remEdge);
        	removedEdges.add(remEdge);

        	// Create a new edge between the two given points and add it to the graph
    		Edge newEdge = new Edge(addPair.p1,addPair.p2);
    		edges.add(newEdge);

    		// Remove the flipped Edge from the two points and add their new edges.
    		addPair.p1.edges.remove(remEdge);
    		addPair.p2.edges.remove(remEdge);
    		addPair.p1.edges.add(newEdge);
    		addPair.p2.edges.add(newEdge);
    		System.out.println("Flipped: "+newEdge.toString());

    		// Update the flip count
    		updateFlipCount();
        }


    	public void run(){

    		// While there remain flips to be made, continue traversing all edges.
    		boolean flips = false;
            do{
            	flips = false;
            	for(int i = 0; i < edges.size(); i++){

            		Edge e = edges.get(i);

            		// Attempt to acquire the lock on e. If not, move onto the next edge.
            		if(e.tryAcquire()){

            			// Get the potential candidate points for flipping Edge e and check if they should be flipped.
            			ArrayList<PointPair> candidates = getFlipCandidates(e);
        				for(int j = 0; j < candidates.size(); j++){
            				PointPair pair = candidates.get(j);

            				// Calculate the angles created from the two point connecting to e.p and e.q.
                        	double angle1 = pair.p1.getAngle(e.p,e.q);
                        	double angle2 = pair.p2.getAngle(e.p,e.q);

                        	// If the angle is greater than PI radians, attempt to flip the edge.
                        	if(angle1+angle2 > Math.PI){

                        		// Obtain the flipLock
                        		synchronized(flipLock){

                        			// If the edge is still flippable (ie a connecting edge to e still exists) then flip it.
                        			// Otherwise, move onto the next edge.
                        			if(isStillFlippable(e,pair)){
                        				flipEdge(e,pair);
                        				flips = true;
                        			}else{
                        				continue;
                        			}
                        		}
                        	}
                        }

        				// Release the lock on e
        				e.release();
            		}else
            			continue;
                }
            } while(flips);
    	}
    }
}
