package A3;

import java.util.ArrayList;

class QueueManipulationThread implements Runnable {
	
	int id;
	QueueObject[] deleted;
	int deletedIndex;
	int deletedSize = 10;
	ArrayList<String> ops;
	
	int n;
	double q;
	long d;
	PriorityQueue pQueue;
	
	public QueueManipulationThread(int id, int n, double q, long d, PriorityQueue pQueue){
		this.id = id;
		this.deleted = new QueueObject[deletedSize];
		this.deletedIndex = 0;
		this.ops = new ArrayList<String>();	
		this.n = n;
		this.q = q;
		this.d = d;
		this.pQueue = pQueue;
	}

	@Override
	public void run() {
		int numOps = 0;
		
		while(numOps < n){
			double prob = Math.random();
			
			if(prob < q){
				
				// Generate character between A and Z
				char c = (char) ((65 + ((int)(26*Math.random()))%26));
				
				// Generate a random priority
				int p = ((int)(11*(Math.random())))%10;
				
				// Either reuse a previously delete object OR create a new one if not possible
				QueueObject item;
				boolean createNew = true;
				int i = 0;
				for(; i < deleted.length; i++){
					if(deleted[i] != null){
						createNew = false;
						break;
					}
				}
				if(createNew)
					item = new QueueObject(c,this.id,p,0);
				else {
					item = deleted[i];
					item.c = c;
					item.priority = p;
					deleted[i] = null;
					item.lockNext = null;
					item.lockFreeNext.set(null);
				}
				
				// Add item to queue
				pQueue.add(item, p);
				
				// Update the ops list
				this.updateOps("add", item);
				
			}else{
				
				// Retrieve and remove highest priority item
				QueueObject item = pQueue.removeMin();
				
				// Update the ops list
				this.updateOps("del", item);
				
				// Update delete list
				if(item.threadId != -1){
					this.deleted[deletedIndex] = item;
					this.updateDeletedIndex();
				}
			}
				
			try {
				Thread.sleep(d);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			numOps++;
		}
	}
	
	private void updateDeletedIndex(){
		deletedIndex = (deletedIndex+1)%this.deletedSize;
	}
	
	private void updateOps(String op, QueueObject item){
		String opString = "";
		
		opString = item.actionTimeStamp + " " + this.id + " " + op + " ";
		if(item.threadId == -1)
			opString+= "*";
		else
			opString+=item.threadId + "" + item.c + " " + item.priority;	
		this.ops.add(opString);
	}
}
