package A3;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Lock-free bucket implementation
 * 
 * This implementation uses non-blocking techniques to ensure safe operations on a bucket,
 * making use of atomic CAS operations.
 * */
public class LockFreeBucket implements Bucket{

	AtomicReference<Node> top;
	LockFreeExchanger exchanger;
	final Random random;
	final int TIMEOUT = 2;	// Milliseconds
	
	public LockFreeBucket(){
		top =  new AtomicReference<Node>(null);
		exchanger = new LockFreeExchanger();
		random = new Random();
	}
	
	@Override
	/**
	 * Put item into this bucket in a lock-free manner
	 * 
	 * @param item is the item to be inserted into this bucket
	 * */
	public void put(QueueObject item) {
		Node node = new Node(item);
		
		// Try to putting a node into the bucket until successful
		while(true){ 
			
			// First, try putting the node into the bucket normally. If there is contention/another thread has modified the bucket
			// try to make an exchange with another thread.
			if(tryPut(node)){
				node.item.actionTimeStamp = System.nanoTime();
				return;
			}
			
			// Attempt to make an exchange with another thread
			try {
				QueueObject popObject = exchanger.exchange(item, TIMEOUT, TimeUnit.MILLISECONDS);
				
				// If the item this thread attempted pushed was retrieved succesfully, the push operation was succesfull
				if(popObject == null){
					System.out.println("SWAPPED WITH GET");
					node.item.actionTimeStamp = System.nanoTime();
					return;
				}
			} catch(TimeoutException e){
			}
		}
		
	}
	
	/**
	 * Try to put a node into the bucket
	 * 
	 * If the bucket had changed from the time this thread began this operation 
	 * to the time you try to physically add the node, then the operation could 
	 * not be done. Otherwise it is successful. This is done using the atomic CAS 
	 * operation.
	 * 
	 * @return whether the node was entered into the bucket successfully or not
	 * */
	private boolean tryPut(Node node){
		Node oldTop = top.get();
		node.next = oldTop;
		return top.compareAndSet(oldTop, node);
	}

	@Override
	/**
	 * Get the next item from the bucket
	 * 
	 * @return the next item in the bucket
	 * */
	public QueueObject get() {
		QueueObject nullObject = new QueueObject('-',-1,-1);
		
		// Attempt to get the next item in the bucket until successful OR realize that the bucket is empty
		// and leave the operation.
		while(true){
			// First try getting the next item in the bucket. If the bucket is empty, leave the operation.
			// If there is contention/another thread has modified the bucket, attempt to make an exchange
			// with another thread.
			Node returnNode;
			try {
				returnNode = tryGet();
				if(returnNode != null){
					returnNode.item.actionTimeStamp = System.nanoTime();
					return returnNode.item;
				}
			} catch (EmptyBucketException ese) {
				nullObject.actionTimeStamp = System.nanoTime();
				return nullObject;
			}
			
			// Attempt to make an exchange with another thread 
			try {
				QueueObject pushObject = exchanger.exchange(null, TIMEOUT, TimeUnit.MILLISECONDS);
				
				// If this thread retrieved an item from another thread, then the exchange was successful
				// and return this item.
				if(pushObject != null){
					System.out.println("SWAPPED WITH PUT");
					pushObject.actionTimeStamp = System.nanoTime();
					return pushObject;
				}
			} catch(TimeoutException te){
			}
		}
	}
	
	/**
	 * Try to get the next item in the bucket
	 * 
	 * If the bucket had been modified between the beginning of this operation
	 * and the physical time a thread attempts to grab the item, then this
	 * operation fails. This is done using the atomic CAS operation. If the bucket
	 * is empty or had not been modified then return null or an item
	 * respectively.
	 * 
	 * @return the next node in the bucket
	 * */
	private Node tryGet() throws EmptyBucketException {
		Node oldTop = top.get();
		if(oldTop == null){
			throw new EmptyBucketException();
		}
		Node newTop = oldTop.next;
		if(top.compareAndSet(oldTop, newTop)){
			return oldTop;
		}else{
			return null;
		}
	}
	
	/**
	 * Lock-free Exchanger Class
	 * 
	 * This class implements a lock-free exchanger to be used amongst multiple threads. Threads
	 * will attempt to get and put items into the bucket, and if they happen to do get/put
	 * operations simultaneously, then the items might as well be swapped directly from one 
	 * to another without using the bucket for the exchange.
	 * */
	static class LockFreeExchanger {
		
		// Set the three possible states: EMPTY, WAITING, BUSY
		static final int EMPTY = 0, WAITING = 1, BUSY = 2;
		
		// Slot where items will be set and retrieved from
		AtomicStampedReference<QueueObject> slot = new AtomicStampedReference<QueueObject>(null,0);
		
		/**
		 * Lock-free exchange method
		 * 
		 * @param item is the item that a thread is attempting to swap
		 * @param timeout is the amount of time a thread should wait for another thread to perform an exchange
		 * @param unit is the time unit (seconds, milliseconds etc) of the timeout
		 * */
		public QueueObject exchange(QueueObject item, long timeout, TimeUnit unit) throws TimeoutException{
			
			// Set the alloted waiting time and the initial stamp that a thread is trying to receive from to EMPTY
			timeout = unit.toNanos(timeout);
			long timeBound = System.nanoTime() + timeout;
			int[] stampHolder = {EMPTY};
			
			// Attempt to make an exchange between multiple threads for a specified amount of time
			while(true){
				if(System.nanoTime() > timeBound)
					throw new TimeoutException();
				QueueObject exchangeItem = slot.get(stampHolder);
				int stamp = stampHolder[0];
				switch(stamp){
				
				// If the exchanger is in an EMPTY state, attempt to put your item into the slot
				case EMPTY:
					if(slot.compareAndSet(exchangeItem, item, EMPTY, WAITING)){
						
						// Wait a specified amount of time for another thread to exchange their item with you
						while(System.nanoTime() < timeBound){
							exchangeItem = slot.get(stampHolder);
							if(stampHolder[0] == BUSY){
								slot.set(null, EMPTY);
								return exchangeItem;
							}
						}
						
						// If the item you put into the slot is still there, then remove it and express that the exchange
						// did not occur due to a timeout. Otherwise retrieve the item thats in the slot. Set the state
						// to EMPTY either way.
						if(slot.compareAndSet(item, null, WAITING, EMPTY)){
							throw new TimeoutException();
						}else{
							exchangeItem = slot.get(stampHolder);
							slot.set(null, EMPTY);
							return exchangeItem;
						}
					}
					break;
				case WAITING:
					// If the exchanger is in a WAITING STATE attempt to put your item into the slot and retrieve the item that is
					// already there. If successful change the state to BUSY to notify the thread who you are making the exchange with
					// that there is an item waiting for them, and for other threads to not attempt to make an exchange.
					if(slot.compareAndSet(exchangeItem, item, WAITING, BUSY)){
						return exchangeItem;
					}
					break;
				case BUSY:
					// If the exchanger is in the BUSY state, dont attempt to make an exchange.
					break;
				default:
				}
			}
		}
	}
	
	/**
	 * Empty Bucket Exception
	 * 
	 * Should be thrown if the bucket is empty
	 * */
	static class EmptyBucketException extends Exception {}
	
	/**
	 * Timeout Exception
	 * 
	 * Should be thrown if the thread has waited too long for an action to occur
	 * */
	static class TimeoutException extends Exception {}

}
