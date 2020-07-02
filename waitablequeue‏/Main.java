package waitablequeue‏;

import java.util.concurrent.TimeUnit;

public class Main {

	public static void main(String[] args) {
		Producer producer1 = new Producer();
		Thread thread1 = new Thread(producer1);

		Consumer consumer1 = new Consumer();
		Thread thread2 = new Thread(consumer1);

		thread1.start();
		thread2.start();
	}
}

class Producer implements Runnable {
	protected static WaitableQueue‏Sem<Integer> testQueue = null;
	private int i = 0;

	public Producer() {
		testQueue = new WaitableQueue‏Sem<Integer>();
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(3);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.err.println("adding node: " + i);
			testQueue.enqueue(i);
			++i;
		}
	}
}

class Consumer implements Runnable {

	public Consumer() {
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			System.err.println("**** dequeue: " + Producer.testQueue.dequeue());

			System.err.println("**** dequeue timeout: " + Producer.testQueue.dequeue(3, TimeUnit.SECONDS));

			System.err.println("**** remove: " + Producer.testQueue.remove(20));

		}
	}
}

//import java.util.Comparator;
//
//public class Main {
//
//	public static void main(String[] args) throws InterruptedException {
//
//		Comparator<Integer> comparator = new Comparator<Integer>() {
//
//			@Override
//			public int compare(Integer o1, Integer o2) {
//				return o1 - o2;
//			}
//		};
//		
//		WaitableQueue‏<Integer> queue = new WaitableQueue‏<>(comparator);
//		
//		queue.enqueue(5);
//		queue.dequeue();
//		
//	}
//
//}
