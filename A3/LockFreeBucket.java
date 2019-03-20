package A3;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class LockFreeBucket implements Bucket{

	AtomicReference<Node> top = new AtomicReference<Node>(null);
	LockFreeExchanger exchanger = new LockFreeExchanger();
	final Random random;
	final int MAX_BACKOFF = 20;
	
	public LockFreeBucket(){
		random = new Random();
	}
	
	@Override
	public void put(QueueObject item) {
		Node node = new Node(item);
		while(true){ 
			try {
				QueueObject popObject = exchanger.exchange(item, MAX_BACKOFF, TimeUnit.MILLISECONDS);
				if(popObject == null){
					node.item.actionTimeStamp = System.nanoTime();
					return;
				}
			} catch(TimeoutException e){
			}
			if(tryPut(node)){
				node.item.actionTimeStamp = System.nanoTime();
				return;
			}
		}
		
	}
	
	private boolean tryPut(Node node){
		Node oldTop = top.get();
		node.next = oldTop;
		boolean success = top.compareAndSet(oldTop, node);
		return success;
	}

	@Override
	public QueueObject get() {
		QueueObject nullObject = new QueueObject('-',-1,-1,-1);
		while(true){
			try {
				QueueObject pushObject = exchanger.exchange(null, MAX_BACKOFF, TimeUnit.MILLISECONDS);
				if(pushObject != null){
					pushObject.actionTimeStamp = System.nanoTime();
					return pushObject;
				}
			} catch(TimeoutException te){
			}
			Node returnNode;
			try {
				returnNode = tryGet();
				if(returnNode != null){
					returnNode.item.actionTimeStamp = System.nanoTime();
					return returnNode.item;
				}
			} catch (EmptyStackException ese) {
				nullObject.actionTimeStamp = System.nanoTime();
				return nullObject;
			}
		}
	}
	
	private Node tryGet() throws EmptyStackException {
		Node oldTop = top.get();
		if(oldTop == null){
			throw new EmptyStackException();
		}
		Node newTop = oldTop.next;
		if(top.compareAndSet(oldTop, newTop)){
			return oldTop;
		}else{
			return null;
		}
	}
	
	static class LockFreeExchanger {
		static final int EMPTY = 0, WAITING = 1, BUSY = 2;
		AtomicStampedReference<QueueObject> slot = new AtomicStampedReference<QueueObject>(null,0);
		public QueueObject exchange(QueueObject item, long timeout, TimeUnit unit) throws TimeoutException{
			long nanos = unit.toNanos(timeout);
			long timeBound = System.nanoTime() + nanos;
			int[] stampHolder = {EMPTY};
			while(true){
				if(System.nanoTime() > timeBound)
					throw new TimeoutException();
				QueueObject exchangeItem = slot.get(stampHolder);
				int stamp = stampHolder[0];
				switch(stamp){
				case EMPTY:
					if(slot.compareAndSet(exchangeItem, item, EMPTY, WAITING)){
						while(System.nanoTime() < timeBound){
							exchangeItem = slot.get(stampHolder);
							if(stampHolder[0] == BUSY){
								slot.set(null, EMPTY);
								return exchangeItem;
							}
						}
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
					if(slot.compareAndSet(exchangeItem, item, WAITING, BUSY)){
						return exchangeItem;
					}
					break;
				case BUSY:
					break;
				default:
				}
			}
		}
	}
	
	static class EmptyStackException extends Exception {}
	
	static class TimeoutException extends Exception {}

}
