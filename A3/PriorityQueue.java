package A3;

public class PriorityQueue {
	int range;
	Bucket[] bucket;
	
	public PriorityQueue(int range, boolean lockFree){
		this.range = range;
		bucket = (Bucket[]) new Bucket[range];
		for(int i = 0; i < bucket.length; i++)
			bucket[i] = lockFree ? new LockFreeBucket() : new LockBucket();
	}	
	
	public void add(QueueObject item, int key){
		bucket[key].put(item);
	}
	public QueueObject removeMin() {
		for( int i = 0; i < range; i++){
			QueueObject item = bucket[i].get();
			if(item != null)
				return item;
		}
		return null;
	}
}
