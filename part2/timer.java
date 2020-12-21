package pack2;
import java.util.Timer;
import pack2.resendTask;

public class timer{
	int delay = 500;
	int period = 500;

	Timer timerer;
	resendTask task;

	public timer(resendTask task) {
		this.task = task;
	}

	public void start() {
		timerer = new Timer();
		resendTask newtask = new resendTask(task);
		timerer.schedule(newtask, delay, period);
	} // start

	public void stop() {
		timerer.cancel();

	} // stop


	public void restart() {
		try{
			stop();
			start();
		}catch (NullPointerException e){}
	} 

}
