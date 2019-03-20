package A3;

/**
 * Bucket interface
 * */
interface Bucket {
	
	/**
	 * Put an item into the bucket
	 * 
	 * @param item is the item that is to be inserted into the bucket
	 * */
	public abstract void put(QueueObject item);
	
	/**
	 * Retrieve the next item from the bucket
	 * */
	public abstract QueueObject get();
}
