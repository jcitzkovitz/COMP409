class q2{

	static CircularLinkedList list;
	static volatile boolean finish = false;

  public static void main(String[] args){

	  // Initialize the list to have nodes A, B and C, such that A is the head and C is the tail.
	  list = new CircularLinkedList();
	  list.head = new Node('A');
	  list.head.next = new Node('B');
	  list.head.next.next = new Node('C');
	  list.tail = list.head.next.next;
	  list.tail.next = list.head;

	  // Create the task threads: PrintNodes, AddNodes and RemoveNodes.
	  Thread thread0 = new Thread(new PrintNodes());
	  Thread thread1 = new Thread(new AddNodes());
	  Thread thread2 = new Thread(new RemoveNodes());

	  // Start each thread;
	  thread0.start();
	  thread1.start();
	  thread2.start();

	  // Let the three threads run for five seconds.
	  long startTime = System.currentTimeMillis();
	  while(System.currentTimeMillis()-startTime < 5000);
	  finish = true;

	  // Print out the final list.
	  int i = -1;
	  Node curNode = list.head;
	  System.out.println("\nFinal List:");
	  while(curNode.c != 'A' || i != 1){
		  System.out.print(curNode.c+" ");
		  curNode = curNode.next;
		  i = 1;
	  }
  }

  /**
	* Thread class that will run through the list and print each node it visits.
	*/
  static class PrintNodes implements Runnable{

	public void run() {
		Node curNode = list.head;
		while(!finish){
			System.out.print(curNode.c+" ");
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			curNode = curNode.next;
		}
	}
  }

  /**
	* Thread class that will run through the list and add a node with a 1/10 chance.
	*/
  static class AddNodes implements Runnable{

		public void run() {
			Node curNode = list.head;
			char c = 'D';
			while(!finish){
				// Add a node to the list with a 1/10 chance.
				if(Math.random() <= 0.1){

					// Create a new node with the next character in line and hold the current nodes next neighbour.
					Node n = new Node(c);
					Node temp = curNode.next;

					// Set the current nodes next neighbour to the new Node, and make the new node point to the current
					// nodes old neighbour.
				    curNode.next = n;
				    n.next = temp;
				    c++;
				}
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// Move on to the next node in the list.
				curNode = curNode.next;
			}
		}

  }

  /**
	* Thread class that will run through the list and delete a node with a 1/10 chance.
	*/
  static class RemoveNodes implements Runnable{

		public void run() {
			Node curNode = list.head;
			Node prevNode = list.tail;

			while(!finish){

				// Delete a node with a 1/10 chance AND if it is not one of the A, B or C nodes.
				if(Math.random() <= 0.1 && curNode.c != 'A' && curNode.c != 'B' && curNode.c != 'C'){
						// Set the previous node's next pointer to the current nodes next pointers.
						// The current node should then have no reference to it and be forgotten in the list
				   	prevNode.next = curNode.next;
				}else{
					prevNode = curNode;
				}
				curNode = curNode.next;
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
  }


  /**
	* Circularly Linked List class implementation that maintains pointers to the list's head and tail.
	* In this implementation however, these are just constructs for initialization and to begin
	* traversal on the list - these will not be updated as items get added/deleted, and hold a pointer
	* to the beginning of the list (head) for printing the final list at the end of the run.
	*/
  static class CircularLinkedList{
    Node head;
    Node tail;
  }

  /**
	* Node class that holds a character c and a next Node pointer.
	*/
  static class Node {
    volatile char c;
    volatile Node next;

    public Node(char c){
      this.c = c;
    }
  }

}
