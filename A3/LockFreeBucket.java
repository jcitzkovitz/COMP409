package A3;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class LockFreeBucket implements Bucket{

	AtomicReference<QueueObject> top = new AtomicReference<QueueObject>();
	LockFreeExchanger exchanger = new LockFreeExchanger();
	final Random random;
	final int MAX_BACKOFF = 500;
	
	public LockFreeBucket(){
		random = new Random();
	}
	
	@Override
	public void put(QueueObject item) {
		int i = 1;
		while(true){
			if(tryPut(item))
				return;
			else
				backoff(i);
			i++;
		}
	}
	
	private boolean tryPut(QueueObject item){
		QueueObject oldTop = top.get();
		item.lockFreeNext.set(oldTop);
		boolean success = top.compareAndSet(oldTop, item);
		item.actionTimeStamp = System.nanoTime();
		return success;
	}

	@Override
	public QueueObject get() {
		boolean[] tryAgain = {false};
		QueueObject nullObject = new QueueObject('-',-1,-1,-1);
		int i = 1;
		do{
			tryAgain[0] = false;
			QueueObject returnObject = tryGet(tryAgain,nullObject);
			if(returnObject.threadId != -1)
				return returnObject;
			else
				backoff(i);
		}while(tryAgain[0]);
		return nullObject;
	}
	
	private QueueObject tryGet(boolean[] tryAgain, QueueObject nullObject) {
		QueueObject oldTop = top.get();
		if(oldTop == null){
			nullObject.actionTimeStamp = System.nanoTime();
			return nullObject;
		}
		
		QueueObject newTop = oldTop.lockFreeNext.get();
		
		if(top.compareAndSet(oldTop, newTop)){
			oldTop.actionTimeStamp = System.nanoTime();
			return oldTop;
		}else{
			tryAgain[0] = true;
			return nullObject;
		}
	}
	
	private void backoff(int i){
		int delay = Math.min(random.nextInt(5)*i, MAX_BACKOFF);
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static class LockFreeExchanger {
		static final int EMPTY = 0, WAITING = 1, BUSY = 2;
		AtomicStampedReference<QueueObject> slot = new AtomicStampedReference<QueueObject>(null,0);
		
		public QueueObject exchange(QueueObject item, long timeout, TimeUnit unit){
			long nanos = unit.toNanos(timeout);
			long timeBound = System.nanoTime() + nanos;
			int[] stampHolder = {EMPTY};
			while(true){
				if(System.nanoTime() > timeBound)
					return null;
				QueueObject exchangeItem = slot.get(stampHolder);
				int stamp = stampHolder[0];
				
				switch(stamp){
				case EMPTY:
					if(slot.compareAndSet(exchangeItem, item, EMPTY, WAITING)){
						while(System.nanoTime() < timeBound){
							exchangeItem = slot.get(stampHolder);
							if(stampHolder[0] == BUSY){
								slot.set(null, BUSY);
								return exchangeItem;
							}
						}
						if(slot.compareAndSet(item, null, WAITING, EMPTY)){
							return null;
						}else{
							exchangeItem = slot.get(stampHolder);
							slot.set(null, EMPTY);
							return exchangeItem;
						}
					}
					break;
				case WAITING:
					if(slot.compareAndSet(exchangeItem, item, WAITING, BUSY))
						return exchangeItem;
					break;
				case BUSY:
					break;
				default:
				}
			}
		}
	}

}
