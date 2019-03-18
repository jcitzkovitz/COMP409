package A3;

public class LockBucket implements Bucket {

	QueueObject head;
	QueueObject tail;
	QueueObject nullObject = new QueueObject('-',-1,-1);
	
	public LockBucket(){
		this.head = nullObject;
		this.tail = nullObject;
	}
	
	@Override
	public QueueObject get() {
		synchronized(this.head){
			if(head.threadId == -1){
				return null;
			}else{
				QueueObject item = head;
				if(head.equals(tail)){
					head = nullObject;
					tail = nullObject;
					
				}else
					head = head.next;
				item.actionTimeStamp = System.currentTimeMillis() + "";
				return item;
			}
		}
	}

	@Override
	public void put(QueueObject item) {
		synchronized(this.tail){
			if(head.threadId == -1){
				head = item;
				tail = item;
			}else{
				tail.next = item;
				tail = tail.next;
			}
			item.actionTimeStamp = System.currentTimeMillis() + "";
		}
	}

}
