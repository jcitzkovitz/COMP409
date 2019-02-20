import java.util.Random;
import java.util.ArrayList;

class Point {
    double x,y;
    
    // Set of edges attached to this point
    public volatile ArrayList<Edge> edges;


    private boolean isLocked;

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


    public synchronized boolean isUnlocked(){
        if (isLocked){
            return false;
        }
        else {
            isLocked= true;
            return true;
        }
    }

    public synchronized void unlock() {
        isLocked = false;
        //Send a signal to all threads that they can now acquire this edge.
        notifyAll();
    }

    public synchronized void waitPoint() {
        //While the point is locked, we wait.
        try {
            while (isLocked) {
                wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    synchronized public ArrayList<Edge> getPointEdges() {
        ArrayList<Edge> edgeHolder = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            edgeHolder.add(edge);
        }

        return edgeHolder;
    }

}



class Edge {
    Point p,q;
    
    public Edge(Point p1,Point p2) { p=p1; q=p2; }
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

    public synchronized void lockEdge() throws InterruptedException {
        boolean pIsUnlocked = p.isUnlocked();
        boolean qIsUnlocked = q.isUnlocked();

        //While both are locked, we release one and wait for the other.
        while (!(pIsUnlocked && qIsUnlocked)) {

            if (pIsUnlocked) {
                //p.isUnlocked locks the point. We release it while we don't have q.
                p.unlock();
                q.waitPoint();
            }

            else if (qIsUnlocked) {
                q.unlock();
                p.waitPoint();
            }
            else {
                p.waitPoint();
                q.waitPoint();
            }

            pIsUnlocked = p.isUnlocked();
            qIsUnlocked = q.isUnlocked();
        }
    }
    
    public String toString() {
        return "<"+p+","+q+">";
    }



}


public class q2 {


    public static int n,t; // constants
    public static Point[] points; 
    public static ArrayList<Edge> edges = new ArrayList<Edge>();
    public static int sumFlips;
    // Returns true if any existing edge intersects this one
    public static boolean intersection(Edge f) {
        for (Edge e : edges) {
            if (f.intersects(e)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized  void incSumFlips() {
        sumFlips++;
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

            // Now your code is required!
            sumFlips = 0;

            Thread[] allThreads = new Thread[t];
            long startTime = System.currentTimeMillis();

            for (int j = 0; j < t; j++) {
                allThreads[j] = new Thread(new delaunayTriangle());
                allThreads[j].start();
            }

            for (int k = 0; k < allThreads.length; k++) {
                allThreads[k].join();
            }
            long runTime = System.currentTimeMillis() - startTime;
            System.out.println("Delaunay triangualtion time to convergence :" + runTime + " ms.");
            System.out.println("Total number of flips: " + sumFlips );


        } catch (Exception e) {
            System.out.println("ERROR " +e);
            e.printStackTrace();
        }


    }

    static class delaunayTriangle implements Runnable {

        //Returns Area of triangle formed by this, p, and q.
        public double getArea(Point a, Point b, Point c) {
            double area = Math.abs(((a.x * (b.y - c.y)) + (b.x * (c.y - a.y)) +  (c.x * (a.y - b.y))) / 2);
            return area;
        }

       //Returns angle formed by three points. B is assumed to be the middle point i.e. point at which angle is formed.
        public double getAngle(Point a, Point b, Point c) {
            double uLenght = Math.pow(b.x - a.x, 2) + Math.pow(b.y - a.y, 2);
            double vLength = Math.pow(b.x - c.x, 2) + Math.pow(b.y - c.y, 2);
            double wLength = Math.pow(c.x - a.x, 2) + Math.pow(c.y - a.y, 2);
            double angle = Math.toDegrees(Math.acos((uLenght + vLength - wLength)/ Math.sqrt(4 * uLenght * vLength)));
            return angle;

        }




        @Override
        public void run() {
            //Pick a random edge to begin with.
            Random random = new Random();
            int index = random.nextInt(edges.size());
            ArrayList<Edge> edgesToCheck = new ArrayList<Edge>();
            Edge startEdge = edges.get(index);
            edgesToCheck.add(startEdge);

            //Loop all edges in order, stop when we come back to start edge.
            for (int i = (index + 1) % edges.size(); i != index; i = (i + 1) % edges.size()) {
                edgesToCheck.add(edges.get(i));
            }


            //Loop until termination: all edges have been viewed without having been modified.
            boolean hasConverged = false;

            while(!hasConverged) {
                hasConverged = true;
                for (Edge e : edgesToCheck) {

                    try {
                        e.lockEdge();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                    Point uPoint = e.p;
                    Point vPoint = e.q;
                    Point adjacentPointU = null;
                    Point adjacentPointV= null;

                    ArrayList<Edge> uPointEdges = uPoint.getPointEdges();

                    for (Edge edge : uPointEdges) {
                        //Get the point the opposite point of u forming edge.
                        Point potential;


                        if (uPoint.same(edge.p)) {
                            potential = edge.q;

                        }
                        else {
                            potential = edge.p;
                        }
                        ArrayList<Edge> potentialEdges = potential.getPointEdges();
                        boolean isTriangle = false;
                        //Get all edges from potential point and find out if it's connected to v.

                        for (Edge edge1 : potentialEdges) {
                            Point u1 = edge1.p;
                            Point v1 = edge1.q;
                            if(vPoint.same(u1) || vPoint.same(v1)) {
                                isTriangle = true;
                                break;
                            }
                        }

                        if (isTriangle) {

                            //Determine size of potential point.
                            int side = e.sat(uPoint.x, uPoint.y, vPoint.x, vPoint.y, potential.x, potential.y);
                            if (side < 0) {
                                //If no other points were found
                                if(adjacentPointU == null) {
                                    adjacentPointU = potential;
                                }
                                //If another point was found, keep the one with the smaller area.
                                else {
                                    double areaPrevious = getArea(uPoint, vPoint, adjacentPointU);
                                    double areaNew = getArea(uPoint, vPoint, adjacentPointU);
                                    if (areaNew < areaPrevious) {
                                        adjacentPointU = potential;
                                    }
                                }
                            }
                            else if (side > 0) {
                                if(adjacentPointV == null) {
                                    adjacentPointV = potential;
                                }
                                else {
                                    double areaPrevious = getArea(uPoint, vPoint, adjacentPointV);
                                    double areaNew = getArea(uPoint, vPoint, potential);
                                    if (areaNew < areaPrevious) {
                                        adjacentPointV = potential;
                                    }
                                }
                            }
                        }

                        //Determine if quadrilateral is convex, if 2 valid points were found.

                        if (!(adjacentPointU == null) && !(adjacentPointV == null)) {

                            //Get all 4 angles

                            //All angles.
                            double uPoint_adjacentPointU_vPoint = getAngle(uPoint, adjacentPointU, vPoint);
                            double adjacentPointU_vPoint_adjacentPointV = getAngle(adjacentPointU, vPoint, adjacentPointV);
                            double vPoint_adjacentPointV_uPoint = getAngle(vPoint, adjacentPointV, uPoint);
                            double adjacentPointV_uPoint_adjacentPointU = getAngle(adjacentPointV,uPoint, adjacentPointU);

                            //Check if all angles are less than 180 to determine if convex.
                            if (uPoint_adjacentPointU_vPoint < 180 & adjacentPointU_vPoint_adjacentPointV < 180) {
                                if (vPoint_adjacentPointV_uPoint < 180 && adjacentPointV_uPoint_adjacentPointU < 180) {

                                    //Check the opposite angles and if they are > 180, flip the edge.
                                    if(uPoint_adjacentPointU_vPoint + vPoint_adjacentPointV_uPoint > 180) {
                                        e.p = adjacentPointU;
                                        e.q = adjacentPointV;

                                        //Add new edge.
                                        adjacentPointU.addEdge(e);
                                        adjacentPointV.addEdge(e);

                                        //Remove old edge.
                                        if (uPoint.edges.contains(e)) {
                                            uPoint.edges.remove(e);
                                        }
                                        if (vPoint.edges.contains(e)){
                                            vPoint.edges.remove(e);
                                        }

                                        hasConverged = false;
                                        incSumFlips();

                                    }
                                }
                            }

                        }
                        //Unlock end points
                        uPoint.unlock();
                        vPoint.unlock();
                    }


                }
            }
        }
    }
}
