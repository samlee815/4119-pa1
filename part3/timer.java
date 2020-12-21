package pack3;
import java.util.Timer;
import pack3.printTable;

public class timer{
	int delay = 3000;

	Timer timerer;
	printTable task;

	public timer(printTable task) {
		this.task = task;
	}

	public void start() {
		timerer = new Timer();
		printTable newprint = new printTable(task);
		timerer.schedule(newprint, delay);
	} // start

	public void stop() {
		timerer.cancel();

	} // stop


	public void restart() {
		try{
			stop();
			start();
		}catch (Exception e){}
	} 

}
