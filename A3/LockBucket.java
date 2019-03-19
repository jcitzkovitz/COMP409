package A3;

public class LockBucket implements Bucket {

	QueueObject top = new QueueObject('t',-1,-1,-1);
	
	public LockBucket(){
		this.top.lockNext = null;
	}
	
	@Override
	public QueueObject get() {
		QueueObject nullObject = new QueueObject('-',-1,-1,-1);
		synchronized(top){
			if(top.lockNext == null){
				nullObject.actionTimeStamp = System.nanoTime();
				return nullObject;
			}else{
				QueueObject item = top.lockNext;
				top.lockNext = top.lockNext.lockNext;
				item.actionTimeStamp = System.nanoTime();
				return item;
			}
		}
	}

	@Override
	public void put(QueueObject item) {
		synchronized(top){
			if(top.lockNext == null){
				top.lockNext = item;
			}else{
				item.lockNext = top.lockNext;
				top.lockNext = item;
			}
			item.actionTimeStamp = System.nanoTime();
		}
	}

}
