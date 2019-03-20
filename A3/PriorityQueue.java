package A3;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Priority Queue Class
 * 
 * */
public class PriorityQueue {
	// Range of buckets
	int range;
	
	// Bucket array - bucket for each priority
	Bucket[] bucket;
	
	public PriorityQueue(int range, boolean lockFree){
		this.range = range;
		bucket = (Bucket[]) new Bucket[range];
		
		// Initialize bucket array to be of type lock or lock free
		for(int i = 0; i < bucket.length; i++)
			bucket[i] = lockFree ? new LockFreeBucket() : new LockBucket();
	}	
	
	/**
	 * Add item to the queue
	 * 
	 * @param item is the item to be added
	 * @param key is the bucket priority to be added in the queue
	 * */
	public void add(QueueObject item, int key){
		bucket[key].put(item);
	}
	
	/**
	 * Remove highest priority item
	 * 
	 * @return the highest priority item in the queue
	 * */
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


