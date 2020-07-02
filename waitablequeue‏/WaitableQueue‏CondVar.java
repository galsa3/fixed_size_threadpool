package waitablequeue‏;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WaitableQueue‏CondVar<T> {

	private PriorityQueue<T> queue = null;
	private Lock lock = new ReentrantLock();
	private Condition condition = lock.newCondition();

	public WaitableQueue‏CondVar() {
		this(null);
	}

	public WaitableQueue‏CondVar(Comparator<? super T> comparator) {
		queue = new PriorityQueue<>(comparator);
	}
	
	public void enqueue(T elem) throws InterruptedException {
		lock.lock();
		queue.add(elem);
		condition.signalAll();
		lock.unlock();
	}

	public T dequeue() throws InterruptedException {
		lock.lock();		
		while(queue.isEmpty()) {
			condition.await();
		}

		T dequeuedElement = queue.poll();
		lock.unlock();

		return dequeuedElement;
	}

	public T dequeue(long timeout, TimeUnit timeunit) throws InterruptedException {
		lock.lock();		
		while(queue.isEmpty()) {
			if(false == condition.await(timeout, timeunit)) {
				lock.unlock();
				
				return null;
			}
		}		
		T dequeuedElement = queue.poll();
		lock.unlock();

		return dequeuedElement;
	}

	public boolean remove(T elem) throws InterruptedException {
		boolean isRemoved = false;

		if(lock.tryLock()) {
			isRemoved = queue.remove(elem);			
			lock.unlock();	
		}		

		return isRemoved;
	}
}