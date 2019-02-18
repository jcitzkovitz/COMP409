import java.util.Random;
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
    
    public String toString() {
        return "<"+p+","+q+">";
    }
}


public class q2 {

    public static int n,t; // constants
    public static Point[] points; 
    public static ArrayList<Edge> edges = new ArrayList<Edge>();

    // Returns true if any existing edge intersects this one
    public static boolean intersection(Edge f) {
        for (Edge e : edges) {
            if (f.intersects(e)) {
                return true;
            }
        }
        return false;
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

        } catch (Exception e) {
            System.out.println("ERROR " +e);
            e.printStackTrace();
        }
    }
}
