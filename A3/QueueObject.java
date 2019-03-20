package A3;

import java.util.concurrent.atomic.AtomicReference;

public class QueueObject {
	char c;
	int threadId;
	int priority;
	int deleted;	
	long actionTimeStamp;
	
	public QueueObject(char c, int threadId, int priority, int deleted){
		this.c = c;
		this.threadId = threadId;
		this.priority = priority;
		this.deleted = deleted;
	}
	
	public boolean equals(QueueObject item){
		return this.c == item.c && this.threadId == item.threadId && this.priority == item.priority;
	}
}
