package pack2;
import java.util.Timer;
import pack2.resendTask;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.*;

public class timer{
	int delay = 500;
	int period = 500;

	ScheduledThreadPoolExecutor sched;
	resendTask resend;
	ReadWriteLock lock;
	ScheduledFuture<?> task;

	public timer(resendTask resend, ScheduledThreadPoolExecutor sched){
		this.resend = resend;
		this.sched = sched;
		this.lock = new ReentrantReadWriteLock();
	}

	public void start() {
		try{
		lock.writeLock().lock();
		task = sched.scheduleWithFixedDelay(resend, delay, period,TimeUnit.MILLISECONDS);
		lock.writeLock().unlock();
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
			lock.writeLock().unlock();
		}catch(Exception e){
			lock.writeLock().unlock();
			System.out.println("Exception in stop call");
		}
	}

	public void restart() {
		try{
			stop();
			start();
		}catch (NullPointerException e){}
	} 

}
