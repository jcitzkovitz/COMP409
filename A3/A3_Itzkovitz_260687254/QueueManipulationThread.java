package A3;

import java.util.ArrayList;

/**
 * Thread class that will randomly manipulate a Priority Queue
 * */
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
		
		// Randomly manipulate the priority queue until you have performed n operations.
		while(numOps < n){
			double prob = Math.random();
			
			// If prob is greater than q add an item to the priority q, and otherwise remove one.
			if(prob > q){
				
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
					item = new QueueObject(c,this.id,p);
				else {
					item = deleted[i];
					item.c = c;
					item.priority = p;
					deleted[i] = null;
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
					deletedIndex = (deletedIndex+1)%this.deletedSize;
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
	
	/**
	 * Update the list of operations done by a thread
	 * 
	 * @param op is the type of operation
	 * @param item is the item that was operated on
	 * */
	private void updateOps(String op, QueueObject item){
		String opString = "";
		opString = item.actionTimeStamp + " " + this.id + " " + op + " ";
		
		if(item.threadId == -1)
			// The item is a null object (meanining that a remove occured on an empty stack)
			opString+= "*";
		else
			opString+=item.threadId + "" + item.c + " " + item.priority;	
		this.ops.add(opString);
	}
}
