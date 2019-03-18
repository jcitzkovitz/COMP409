package A3;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeBucket implements Bucket{

	AtomicReference<QueueObject> head = new AtomicReference<QueueObject>(null);
	AtomicReference<QueueObject> tail = new AtomicReference<QueueObject>(null);
	
	@Override
	public void put(QueueObject item) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public QueueObject get() {
		// TODO Auto-generated method stub
		return null;
	}

}
