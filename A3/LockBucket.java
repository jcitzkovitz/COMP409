package A3;

public class LockBucket implements Bucket {

	Node top = new Node(new QueueObject('t',-1,-1,-1));
	
	public LockBucket(){
		this.top.next = null;
	}
	
	@Override
	public QueueObject get() {
		QueueObject nullObject = new QueueObject('-',-1,-1,-1);
		synchronized(top){
			if(top.next == null){
				nullObject.actionTimeStamp = System.nanoTime();
				return nullObject;
			}else{
				Node node = top.next;
				top.next = top.next.next;
				node.item.actionTimeStamp = System.nanoTime();
				return node.item;
			}
		}
	}

	@Override
	public void put(QueueObject item) {
		synchronized(top){
			Node node = new Node(item);
			if(top.next == null){
				top.next = node;
			}else{
				node.next = top.next;
				top.next = node;
			}
			node.item.actionTimeStamp = System.nanoTime();
		}
	}

}
