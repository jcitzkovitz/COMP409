package A3;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicStampedReference;

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
		QueueObject item = null;
		for( int i = 0; i < range; i++){
			item = bucket[i].get();
			if(item.threadId != -1)
				return item;
		}
		return item;
	}
	
}


