package A3;

import java.util.concurrent.atomic.AtomicReference;

public class QueueObject {
	char c;
	int threadId;
	int priority;
	int deleted;	
	QueueObject lockNext;
	AtomicReference<QueueObject> lockFreeNext = new AtomicReference<QueueObject>();
	long actionTimeStamp;
	
	public QueueObject(char c, int threadId, int priority, int deleted){
		this.c = c;
		this.threadId = threadId;
		this.priority = priority;
		this.deleted = deleted;
		this.lockFreeNext.set(null);
	}
	
	public boolean equals(QueueObject item){
		return this.c == item.c && this.threadId == item.threadId && this.priority == item.priority;
	}
}
