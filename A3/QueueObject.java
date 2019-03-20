package A3;

import java.util.concurrent.atomic.AtomicReference;

/**
 * QueueObject Class
 * 
 * Objects that will be stored in the Priority Queue containing pertinent information
 * */
public class QueueObject {
	char c;
	int threadId;
	int priority;
	long actionTimeStamp;
	
	/**
	 * Constructor
	 * 
	 * @param c is a character
	 * @param threadId is the ID of the thread that has created the object
	 * @param priority is the priority of the object in priority queue
	 * */
	public QueueObject(char c, int threadId, int priority){
		this.c = c;
		this.threadId = threadId;
		this.priority = priority;
	}
	
	/**
	 * Object comparator function
	 * 
	 * @param item is the item that this will be compared to
	 * */
	public boolean equals(QueueObject item){
		return this.c == item.c && this.threadId == item.threadId && this.priority == item.priority;
	}
}
