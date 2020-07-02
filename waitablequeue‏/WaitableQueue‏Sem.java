package waitablequeue‏;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WaitableQueue‏Sem<T> {
	private PriorityQueue<T> queue = null;
	private Semaphore sem = new Semaphore(0);
	private Lock lock = new ReentrantLock();

	public WaitableQueue‏Sem() {
		this(null);
	}
		   
	public WaitableQueue‏Sem(Comparator<? super T> comparator) {
		queue = new PriorityQueue<>(comparator);
	}

	public void enqueue(T elem) {
		lock.lock();
		queue.add(elem);
		lock.unlock();
		sem.release();
	}
	
	public T dequeue() {
		try {
			sem.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		lock.lock();
		T dequeuedElement = queue.poll();
		lock.unlock();

		return dequeuedElement;
	}

	public T dequeue(long timeout, TimeUnit timeunit) {
		T dequeuedElement = null;

		try {
			if(sem.tryAcquire(timeout, timeunit)) {
				lock.lock();
				dequeuedElement = queue.poll();
				lock.unlock();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return dequeuedElement;
	}

	public boolean remove(T elem) {
		boolean isRemoved = false;
		
		if(sem.tryAcquire()) {
			if(lock.tryLock()) {
				isRemoved = queue.remove(elem);
				if(!isRemoved) { sem.release(); }
				lock.unlock();
			} else {
				sem.release();
			}			
		}
		
		return isRemoved;
	}
}