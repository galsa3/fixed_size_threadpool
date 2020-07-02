package threadpool;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import threadpool.ThreadPool.TaskPriority;

public class StartThreadPool {

	public static void main(String[] args) {
		
		try {
			testPause();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		//testCancel();
		//testSetNumThreadsInc();
		//testSetNumThreadsDec();
		//testException();
	}
	
	private static void testPause() throws InterruptedException {
		Executor ex = new ThreadPool();
		ex.execute(new RunnableTask());
		
		ThreadPool pool = new ThreadPool(5);
		pool.submitTask(callStrHigh);
		pool.submitTask(callStrHigh);
		pool.submitTask(callStrHigh);
		pool.submitTask(callStrHigh);
		pool.submitTask(callStrHigh);
		pool.submitTask(callStrHigh);
		pool.submitTask(callStrHigh);
		pool.submitTask(callStrHigh);
		
		Thread.sleep(2000);
		
		pool.pause();
		
		Thread.sleep(3000);
		
		pool.resume();
		pool.shutdown();
		System.exit(0);
	}

//	private static void testException() {
//		ThreadPool pool = new ThreadPool(1);
//		Future<String> future = pool.submitTask(callException);
//
//		try {
//			future.get();
//		} catch (InterruptedException | ExecutionException e) {
//			System.err.println(e.getCause());
//		}
//		
//		pool.shutdown();		
//	}

//	private static void testSetNumThreadsDec() throws InterruptedException {
//		ThreadPool pool = new ThreadPool(5);
//		System.out.println("remove");
//		pool.setNumberOfThread(3);		
//		Thread.sleep(5000);
//		System.out.println("sutthing down next");
//
//		pool.shutdown();
//	}	
	
//	private static void testSetNumThreadsInc() {
//		ThreadPool pool = new ThreadPool(1);
//		System.out.println("add");
//		pool.setNumberOfThread(5);		
//		
//		System.out.println("sutthing down next");
//
//		pool.shutdown();
//	}

	static void testCancel() throws InterruptedException {
		ThreadPool pool = new ThreadPool(1);

		//Future<String> future = pool.submitTask(callStrLow, TaskPriority.MIN);
		Future<String> future2 = pool.submitTask(callStrLow, TaskPriority.MIN);
		System.out.println("is canceled: " + future2.isCancelled());
		System.out.println("try cancel: " + future2.cancel(true));
		
		Thread.sleep(100);
		System.out.println("is canceled: " + future2.isCancelled());	
		
		pool.shutdown();
		//next line is throwing exception, not allowed to submit after shutdown
		//Future<String> future1 = pool.submitTask(callStrHigh, TaskPriority.MAX);
		
	}
	
	static Callable<String> callStrLow = new Callable<String>() {

		@Override
		public String call() throws Exception {
			System.out.println("hello from call str LOW priority");
			System.out.println(Thread.currentThread().getName());
			Thread.sleep(2000);
			
			return "return from low";
		}
	};
	
	static Callable<String> callStrMid = new Callable<String>() {

		@Override
		public String call() throws Exception {
			Thread.sleep(100);
			
			return null;
		}
	};
	
	static Callable<String> callStrHigh = new Callable<String>() {

		@Override
		public String call() throws Exception {
			System.out.println("hello from call str HIGH priority");
			System.out.println(Thread.currentThread().getName());
			
			Thread.sleep(5000);
			
			return "return from high";
//			while(true) {
//				System.out.println("hi");
//				Thread.sleep(2000);
//			}		
		}			
	};
	
	static Callable<String> callException = new Callable<String>() {
		@Override
		public String call() throws Exception {
			throw new NullPointerException();
		}
	};
	
	static class RunnableTask implements Runnable {
		@Override
		public void run() {
			System.out.println("hello from A runnable");
			System.out.println(Thread.currentThread().getName());
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}				
	}
}
