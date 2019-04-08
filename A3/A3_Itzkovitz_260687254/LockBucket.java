package A3;

/**
 * Lock bucket implementation
 * 
 * This implementation uses blocking techniques to ensure safe operations on a bucket
 * by allowing only one thread to enter it at a time.
 * */
public class LockBucket implements Bucket {

	
	Node top;
	
	public LockBucket(){
		
		// Set the top node pointer to be a physical node that does not change. 
		// The top node object will be synchronized on, and its next pointer 
		// will act as the main top pointer.
		top = new Node(new QueueObject('t',-1,-1));
		this.top.next = null;
	}
	
	@Override
	/**
	 * Get next item in the bucket
	 * 
	 * @return the next item in the bucket
	 * */
	public QueueObject get() {
		QueueObject nullObject = new QueueObject('-',-1,-1);
		
		// When a thread gets the lock, check if the bucket is empty. If yes, then
		// return a null node, otherwise return the next node and change the top
		// of the bucket to its successor.
		synchronized(top){
			if(top.next == null){
				nullObject.actionTimeStamp = System.nanoTime();
				return nullObject;
			}else{
				Node node = top.next;
				top.next = top.next.next;
				node.item.actionTimeStamp = System.nanoTime();
				return node.item;
			}
		}
	}

	@Override
	/**
	 * Put an item into the bucket
	 * */
	public void put(QueueObject item) {
		// When a thread gets the lock, change the top pointer to be the given item and its
		// next pointer to the previous top item.
		synchronized(top){
			Node node = new Node(item);
			node.next = top.next;
			top.next = node;
			node.item.actionTimeStamp = System.nanoTime();
		}
	}

}
