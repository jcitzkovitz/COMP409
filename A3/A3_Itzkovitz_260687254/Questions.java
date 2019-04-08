package A3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Driver class for both q1 and q2 as they contain many similar operations
 * */
public class Questions {

	static private PriorityQueue pQueue;
	
	public static long runQuestions(String tString, String qString, String dString, String nString, boolean lockFree, boolean print){
		
		// Number of threads to be used
		int t = Math.max(1, Integer.parseInt(tString));
		
		// Probability of item deletion
		double q = Double.parseDouble(qString);
		if(q < 0 || q > 1){
			System.out.println("You must enter a q value between 1 and 0");
			System.exit(0);
		}
		
		// Random time delay between operations in ms
		long d = Long.parseLong(dString);
		
		// Number of total operations
		int n = Integer.parseInt(nString);
		
		// Initialize the Priority Queue
		pQueue = new PriorityQueue(10,lockFree);
		
		// Create a thread pool with t threads
		ExecutorService executor = Executors.newFixedThreadPool(t);
		QueueManipulationThread[] threads = new QueueManipulationThread[t];
		
		for(int i = 0; i < t; i++){
			threads[i] = new QueueManipulationThread(i,n,q,d,pQueue);
		}
		
		// Run all threads and record the time of execution
		long startTime = System.currentTimeMillis();
		for(int i = 0; i < t; i++){
			executor.execute(threads[i]);
		}
		
		executor.shutdown();
		while (!executor.isTerminated());
		
		long runTime = System.currentTimeMillis() - startTime;
		
		if(print)
			printList(threads,t,n);
		
		return runTime;
	}
	
	/**
	 * Print a sequence of the first 1000 operations done by all threads combines
	 * 
	 * @param threads are the threads that were run
	 * @param t is the number of threads
	 * @param n is the number of operations done by each thread
	 * */
	public static void printList(QueueManipulationThread[] threads, int t, int n){
		int[] indexTracker = new int[t];
		for(int i = 0; i < n; i++){
			
			if(i >= 1000)
				break;
			
			int min = 0;
			long minTime = Long.parseLong(threads[0].ops.get(indexTracker[0]).split(" ")[0]);
			
			// Chose which thread performed the earliest operation and print their operation string
			for(int j = 0; j < t; j++){
				String currentOp[] = threads[j].ops.get(indexTracker[j]).split(" ");
				long time = Long.parseLong(currentOp[0]);
				if(time < minTime){
					min = j;
					minTime = time;
				}
			}
			
			indexTracker[min]++;
			
			System.out.println(threads[min].ops.get(indexTracker[min]-1));
		}
	}
}
