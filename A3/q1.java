package A3;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicStampedReference;

public class q1 {

	static PriorityQueue pQueue;
	
	public static void main(String[] args){
		
		// Number of threads to be used
		int t = Math.max(1, Integer.parseInt(args[0]));
		double q;
		long d;
		int n;
		
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
		pQueue = new PriorityQueue(10,true);
		
		// Create a thread pool of size t
		ExecutorService executor = Executors.newFixedThreadPool(t);
		QueueManipulationThread[] threads = new QueueManipulationThread[t];
		
		for(int i = 0; i < t; i++){
			threads[i] = new QueueManipulationThread(i,n,q,d,pQueue);
		}
		
		for(int i = 0; i < t; i++){
			executor.execute(threads[i]);
		}
		
		executor.shutdown();
		while (!executor.isTerminated());
		
		printList(threads,t,n);
	}
	
	private static void printList(QueueManipulationThread[] threads, int t, int n){
		int[] indexTracker = new int[t];
		for(int i = 0; i < n; i++){
			int min = 0;
			long minTime = Long.parseLong(threads[0].ops.get(indexTracker[0]).split(" ")[0]);
			for(int j = 0; j < t; j++){
				String currentOp[] = threads[j].ops.get(indexTracker[j]).split(" ");
				long time = Long.parseLong(currentOp[0]);
				if(time < minTime){
					min = j;
					minTime = time;
				}
			}
			indexTracker[min]++;
			System.out.println(i+": "+threads[min].ops.get(indexTracker[min]-1));
		}
	}
	
}