package A3;

/**
 * Node object that holds a QueueObject and a next Node pointer
 * 
 * Nodes will be used to structure the buckets
 * */
public class Node {
	QueueObject item;
	Node next;
	
	public Node(QueueObject item){
		this.item = item;
	}
}
