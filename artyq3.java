
import static java.lang.Thread.sleep;

import java.util.Random;

public class q3 {

    //We make the timer volatile so that every thread can check if they should continue to execute.
    volatile static long startTime;

    public static void main(String[] args) {
        try {
            //Instantiate circular list. Contains circular list of 'A', 'B', and 'C' right away.
            CircularLinkedList list = new CircularLinkedList();

            //This will be executed by Thread 0: the printing thread.
            Runnable printTask = new Runnable() {

                @Override
                public void run() {
                    //Must be put inside a try catch block to use sleep method.
                    try {

                        //We start list traversal.
                        Node current = list.getHead();

                        //While execution time did not last 5 seconds, we print, sleep, get next node and repeat.
                        while (System.currentTimeMillis() < (startTime + 5000)) {

                            System.out.print(current.getData() + " ");
                            sleep(100);
                            current = current.getNext();
                        }
                    } catch (Exception e) {
                        System.out.println("ERROR " + e);
                        e.printStackTrace();

                    }
                }

            };

            //This will be run by Thread 1: the removing thread.
            Runnable removeTask = new Runnable() {
                @Override
                public void run() {

                    try {

                        //Keep track of previous node. We will start scanning the list from the second element.
                        Node previous = list.getHead();
                        Node current = previous.getNext();


                        Random rand = new Random();


                        while (System.currentTimeMillis() < (startTime + 5000)) {

                            //Gets a number between 1 and 10.
                            int probability = rand.nextInt(9) + 1;

                            //If we get our probability is 1 in 10.
                            if (probability == 1) {

                                //Only delete if it's not A, B, or C and sleep 20 ms.
                                if (current.getData() != 'A' && current.getData() != 'B' && current.getData() != 'C') {
                                    list.deleteNode(previous, current);
                                    System.out.println("Deleted :" + current.getData() + " ");
                                    sleep(20);
                                }

                            }
                            //Advance the pointers.
                            current = previous.getNext();
                            previous = previous.getNext();


                        }

                    } catch (Exception e) {
                        System.out.println("ERROR " + e);
                        e.printStackTrace();
                    }

                }
            };


            Runnable insertTask = new Runnable() {

                @Override
                public void run() {
                    try {

                        //Create Random object.
                        Random rand = new Random();

                        //Get node to start.
                        Node current = list.getHead();

                        //While there is still time for execution.
                        while (System.currentTimeMillis() < (startTime + 5000)) {

                            //Get number between 1 and 10.
                            int probability = rand.nextInt(9) + 1;

                            //If we hit our probability, generate random visible character.
                            if (probability == 1) {
                                // 33 is ascii for first visible character and 126 is the last visible ascii.
                                int character = rand.nextInt(126 - 33) + 33;

                                //Keep generating new letter if we get 'A', 'B', or 'C'.
                                while (character == 65 || character == 66 || character == 67) {
                                    character = rand.nextInt(122 - 33) + 33;
                                }

                                //Cast to char and insert.
                                char toInsert = (char) character;
                                list.insertNode(current, toInsert);
                                System.out.println("Inserted " + toInsert);
                                sleep(20);
                                current = current.getNext();
                            }

                        }
                    } catch (Exception e) {
                        System.out.println("ERROR " + e);
                        e.printStackTrace();
                    }
                }
            };

        Thread[] allThreads = new Thread[3];

        allThreads[0] = new Thread(printTask);
        allThreads[1] = new Thread(removeTask);
        allThreads[2] = new Thread(insertTask);

        startTime = System.currentTimeMillis();

        //Start all threads.
        for (int i = 0; i < allThreads.length; i++) {
            allThreads[i].start();
        }
        //Join them before continuing main.
        for (int i = 0; i < allThreads.length; i++) {
            allThreads[i].join();
        }
        System.out.println("Done");
        list.printList();


        } catch (Exception e) {
            System.out.println("ERROR " + e);
            e.printStackTrace();
        }
    }

    //Class to consolidate Node information for list.
    static class Node {
        //The next is volatile as all threads need access to this information to avoid data race.
        volatile private Node next;
        private char data;

        //Constructor. Assign next Node through setNext(Node node).
        public Node(char data) {
            this.next = null;
            this.data = data;
        }


        public Node getNext() {
            return this.next;
        }

        public char getData() {
            return this.data;
        }

        public void setNext(Node node) {
            this.next = node;
        }
    }

    //Class represent CircularLinkedList Object. Only necessary methods for this question have been implemented.
    static class CircularLinkedList {
        private Node head = null;

        //Constructor : creates A, B, C nodes right away to always have a list with 3 elements.
        public CircularLinkedList() {

            Node a = new Node('A');
            Node b = new Node('B');
            Node c = new Node('C');

            a.setNext(b);
            b.setNext(c);
            c.setNext(a);

            this.head = a;
        }

        public Node getHead() {
            return this.head;
        }


        public void insertNode(Node current, char data) {
            Node newNode = new Node(data);
            newNode.setNext(current.getNext());
            current.setNext(newNode);
        }

        public void deleteNode(Node previousNode, Node currentNode) {
            previousNode.setNext(currentNode.getNext());
        }


        //Method to print the list.
        public void printList() {
            Node curr = this.getHead();
            System.out.print(curr.getData() + " ");
            curr = curr.getNext();
            while(curr != this.head) {
                System.out.print((curr.data) + " ");
                curr = curr.getNext();
            }
            System.out.print(curr.getData());
        }
    }

}