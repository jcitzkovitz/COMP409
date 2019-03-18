package A3;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class q1 {

	static double q;
	static long d;
	static int n;
	static PriorityQueue pQueue;
	
	public static void main(String[] args){
		
		// Number of threads to be used
		int t = Math.max(1, Integer.parseInt(args[0]));
		
		// Probability of item deletion
		q = Double.parseDouble(args[1]);
		if(q < 0 || q > 1){
			System.out.println("You must enter a q value between 1 and 0");
			System.exit(0);
		}
		
		// Random time delay between operations in ms
		d = Long.parseLong(args[2]);
		
		// Number of total operations
		n = Integer.parseInt(args[3]);
		
		// Initialize the Priority Queue
		pQueue = new PriorityQueue(10,false);
		
		// Create a thread pool of size t
		ExecutorService executor = Executors.newFixedThreadPool(t);
		QueueManipulationThread[] threads = new QueueManipulationThread[t];
		
		for(int i = 0; i < t; i++){
			threads[i] = new QueueManipulationThread(i);
		}
		
		for(int i = 0; i < t; i++){
			executor.execute(threads[i]);
		}
		
		executor.shutdown();
		while (!executor.isTerminated());
		
		printList(threads,t);
		
	}
	
	private static void printList(QueueManipulationThread[] threads, int t){
		int[] indexTracker = new int[t];
		for(int i = 0; i < n; i++){
			int min = 0;
			String minTime = threads[0].ops.get(indexTracker[0]).split(" ")[0];
			for(int j = 0; j < t; j++){
				String currentOp[] = threads[j].ops.get(indexTracker[j]).split(" ");
				String time = currentOp[0];
				int comparison = time.compareTo(minTime);
				if(comparison < 0){
					min = j;
					minTime = time;
				}
			}
			indexTracker[min]++;
			System.out.println(threads[min].ops.get(indexTracker[min]-1));
		}
	}
	
	static class QueueManipulationThread implements Runnable {
		
		int id;
		QueueObject[] deleted;
		int deletedIndex;
		int deletedSize = 10;
		ArrayList<String> ops;
		
		public QueueManipulationThread(int id){
			this.id = id;
			this.deleted = new QueueObject[deletedSize];
			this.deletedIndex = 0;
			this.ops = new ArrayList<String>();	
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
					if(item != null){
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
			long timeStamp = System.currentTimeMillis();
			if(item == null)
				opString = timeStamp + " " + this.id + " " + op + " *";
			else
				opString = item.actionTimeStamp + " " + this.id + " " + op + " " + item.threadId + "" + item.c + " " + item.priority;	
			this.ops.add(opString);
		}
	}
}