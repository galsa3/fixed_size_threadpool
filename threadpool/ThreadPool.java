package threadpool;


import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import waitablequeue‏.*;

public class ThreadPool implements Executor {	
	private static final String INVALID_MESSAGE = "Number of threads must be greater than zero: ";
	private WaitableQueue‏Sem<ThreadPoolTask<?>> tasksQueue = new WaitableQueue‏Sem<>();
	private final static int DEAFULT_NUM_THREADS = Runtime.getRuntime().availableProcessors();
	private static final int VIP_PRIORITY = 10;
	private static final int LOWEST_PRIORITY = -1;
	private int numOfThreads = 0;
	private boolean isShutdown = false;	
	private Semaphore pauseTaskSem = new Semaphore(0);
	private Semaphore shutdownSem = new Semaphore(0);
	
	public enum TaskPriority {
		MIN,
		NORM,
		MAX,		
	}
	
	public ThreadPool() {
		this(DEAFULT_NUM_THREADS);
	}
	
	public ThreadPool(int newNumOfThreads) {
		if (newNumOfThreads <= 0){
	        throw new IllegalArgumentException(INVALID_MESSAGE + numOfThreads);
	    }
		createAndStartThreads(newNumOfThreads);
		numOfThreads = newNumOfThreads;
	}
	
	public <T> Future<T> submitTask(Callable<T> callable) {
		return submitTask(callable, TaskPriority.NORM.ordinal());
	}
	
	public <T> Future<T> submitTask(Callable<T> callable, TaskPriority taskPriority) {		
		return submitTask(callable, taskPriority.ordinal());
	}
	
	public Future<Void> submitTask(Runnable runnable, TaskPriority taskPriority) {
		Callable<Void> callable = convertRunnableToCallable(runnable, null);
		
		return submitTask(callable, taskPriority.ordinal());
	}
	
	public <T> Future<T> submitTask(Runnable runnable, TaskPriority taskPriority, T returnVal) {		
		Callable<T> callable = convertRunnableToCallable(runnable, returnVal);
		
		return submitTask(callable, taskPriority.ordinal());
	}
		
	public void setNumberOfThread(int newNumOfThreads) {		
		if(newNumOfThreads <= 0){
	        throw new IllegalArgumentException(INVALID_MESSAGE + numOfThreads);
	    }		
		if(numOfThreads <= newNumOfThreads) {			
			createAndStartThreads(newNumOfThreads - numOfThreads);		
		} 
		else {
			insertTasksToQueue(new KillThreadsTask(), VIP_PRIORITY, numOfThreads - newNumOfThreads);
		}
		
		numOfThreads = newNumOfThreads;		
	}

	@Override
	public void execute(Runnable runnable) {
		submitTask(runnable, TaskPriority.NORM);
	}
	
	public void pause() {		
		insertTasksToQueue(new PauseTask(), VIP_PRIORITY, numOfThreads);
	}
	
	public void resume() {
		pauseTaskSem.release(numOfThreads);
	}
	
	public void shutdown() {
		insertTasksToQueue(new ShutdownTask(), LOWEST_PRIORITY, numOfThreads);
		isShutdown = true;	
	}	

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {		
		if(shutdownSem.tryAcquire(numOfThreads, timeout, unit)) {
			return true;
		}				
		
		return false;		
	}	

	private void submitTask(Runnable runnable, int taskPriority) {
		Callable<Void> callable = convertRunnableToCallable(runnable, null);		
		submitTask(callable, taskPriority);
	}
	
	private <T> Future<T> submitTask(Callable<T> callable, int taskPriority) {
		if(isShutdown) {
			throw new SuttingDownException();
		}
		ThreadPoolTask<T> poolTask = new ThreadPoolTask<T>(taskPriority, callable);
		tasksQueue.enqueue(poolTask);
		
		return poolTask.getFuture();
	}	
	
	private <T> Callable<T> convertRunnableToCallable(Runnable runnable, T returnVal) {
		Callable<T> callable = Executors.callable(runnable, returnVal);
		
		return callable;
	}	

	private void createAndStartThreads(int howMany) {
		for(int i = 0; i < howMany; ++i) {
			new WorkerThread().start();
		}					
	}	
	
	private void insertTasksToQueue(Runnable task, int priority, int howManyTasks) {
		for(int i = 0; i < howManyTasks; ++i) {				
			submitTask(task, priority);
		}		
	}
	
	private class PauseTask implements Runnable {
		@Override
		public void run() {
			try {
				pauseTaskSem.acquire();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}				
	}
	
	private class KillThreadsTask implements Runnable {
		@Override
		public void run() {
			WorkerThread currentThread = (WorkerThread)Thread.currentThread();
			currentThread.isRunning = false;		
		}				
	}
	
	private class ShutdownTask implements Runnable {
		@Override
		public void run() {
			WorkerThread currentThread = (WorkerThread)Thread.currentThread();
			currentThread.isRunning = false;
			shutdownSem.release();
		}				
	}
	
	private class WorkerThread extends Thread {
		private boolean isRunning = true;
		
		@Override
		public void run() {
			ThreadPoolTask<?> currTask = null;
			
			while(isRunning) {
				currTask = tasksQueue.dequeue();
				currTask.runTask();							
			}			
		}
	}
		
	private class ThreadPoolTask<T> implements Comparable<ThreadPoolTask<T>> {	
		private int taskPriority;
		private Callable<T> callable;
		private TaskFuture taskFuture = new TaskFuture();
		private Semaphore getFutureSem = new Semaphore(0);		
		
		private ThreadPoolTask(int taskPriority, Callable<T> callable) {
			this.taskPriority = taskPriority;
			this.callable = callable;
		}
		
		private TaskFuture getFuture() {
			return taskFuture;
		}

		@Override
		public int compareTo(ThreadPoolTask<T> other) {			
			return other.taskPriority - this.taskPriority;
		}
		
		private void runTask() {
			try {
				taskFuture.returnObj = callable.call();
			} catch (Exception e) {
				taskFuture.exception = new ExecutionException(e);
			}
			taskFuture.isDone = true;
			getFutureSem.release();					
		}
		
		private class TaskFuture implements Future<T> {
			private boolean isDone = false;
			private boolean isCanceled = false;
			private ExecutionException exception = null;
			private T returnObj;
			
			@Override
			public boolean cancel(boolean canBeInterrupted) {
				if(tasksQueue.remove(ThreadPoolTask.this)) {
					isCanceled = true;
					getFutureSem.release();
				}

				return isCanceled;
			}

			@Override
			public T get() throws InterruptedException, ExecutionException {				
				T future = null;
				
				try {
					future = get(Long.MAX_VALUE, TimeUnit.DAYS);
				} catch (TimeoutException e) {
					e.printStackTrace();
				}
				
				return future;
			}

			@Override
			public T get(long timeout, TimeUnit timeUnit) 
					throws InterruptedException, ExecutionException, TimeoutException {
				
				getFutureSem.tryAcquire(timeout, timeUnit);				
				
				if(null != exception) {
					throw exception;
				}												
				
				return returnObj;
			}

			@Override
			public boolean isCancelled() {
				return isCanceled;
			}

			@Override
			public boolean isDone() {
				return isDone;
			}			
		}
	}
	
	private class SuttingDownException extends RuntimeException {

		private static final long serialVersionUID = 1L;	}	
}