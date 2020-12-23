package pack4;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.*;

public class timer{
	int delay;
	int period;

	ScheduledThreadPoolExecutor sched;
	TimerTask ttask;
	ReadWriteLock lock;
	ScheduledFuture<?> task;

	public timer(TimerTask ttask, ScheduledThreadPoolExecutor sched, int delay, int period){
		this.ttask = ttask;
		this.sched = sched;
		this.lock = new ReentrantReadWriteLock();
		this.delay = delay;
		this.period = period;
//		System.out.printf("timer of created\n");

	}

	public void start() {
		try{
		lock.writeLock().lock();
		task = sched.scheduleWithFixedDelay(ttask, delay, period,TimeUnit.MILLISECONDS);
		lock.writeLock().unlock();
//		System.out.println("timer fired");
		}catch(Exception e){
			lock.writeLock().unlock();
		}
	} // start

	public void stop() {
		try{
			lock.writeLock().lock();
			if(task == null){
				//do nothing
			}else{
				if(task.isCancelled() || task.isDone()){

				}else{
					task.cancel(true);
				}
				task = null;
			}
//			System.out.println("timer fired");
			lock.writeLock().unlock();
		}catch(Exception e){
			lock.writeLock().unlock();
//			System.out.println("Exception in stop call");
		}
	}

	public void restart() {
		try{
			stop();
			start();
		}catch (NullPointerException e){}
	} 

}
