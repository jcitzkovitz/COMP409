package A3;

public class QueueObject {
	char c;
	int threadId;
	int priority;
	
	QueueObject next;
	String actionTimeStamp;
	
	public QueueObject(char c, int threadId, int priority){
		this.c = c;
		this.threadId = threadId;
		this.priority = priority;
	}
	
	public boolean equals(QueueObject item){
		return this.c == item.c && this.threadId == item.threadId && this.priority == item.priority;
	}
}
