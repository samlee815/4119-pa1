import java.io.*;
import java.lang.*;
import java.util.*;
import java.net.*;
import java.nio.channels.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;
import java.util.concurrent.*;
import pack2.SenderSide;
import pack2.resendTask;
import pack2.GbnIO;
import pack2.timer;

public class gbnnode{
	public static void invalidArgs(){
		System.out.println("Usage : java gbnnode <self-port> <peer-port> <window-size> [ -d <value-of-n> j -p <value-of-p>]");
 		System.exit(0);
    }
	public static void main (String[] args) {
		int mode = 1;
		int selfPort = 0;
		int peerPort = 0;
		int windowSize = 0;
		int dVal = 0; 
		double pVal = 0.0;
		ArrayList<Character> msg = new ArrayList<>();
        List<Character> sendMsg = Collections.synchronizedList(msg);
		List<Boolean> bools=new ArrayList<Boolean>();

		if(!(args.length == 3 || args.length == 5)){
            invalidArgs();
        }
		if(args.length == 3){
            try{
				selfPort = Integer.parseInt(args[0]);
                peerPort = Integer.parseInt(args[1]);
                windowSize = Integer.parseInt(args[2]);
            }catch (NumberFormatException e) {
				invalidArgs();
            }
        }
		if(args.length == 5){
            try{
                selfPort = Integer.parseInt(args[0]);
                peerPort = Integer.parseInt(args[1]);
                windowSize = Integer.parseInt(args[2]);
				if(args[3].equals("-d")){
					mode = 2;
					dVal = Integer.parseInt(args[4]);
				}else if(args[3].equals("-p")){
					mode = 3;
					pVal = Double.parseDouble(args[4]);
				}else{
					invalidArgs();
				}
			}catch (NumberFormatException e) {
				invalidArgs();
            }
        }
		if((selfPort<1024 || selfPort>65353)){
                System.out.println("Invalid self port number");
                invalidArgs();
        }
		if((peerPort<1024 || peerPort>65353)){
                System.out.println("Invalid peer port number");
                invalidArgs();
        }

		try{
			InetAddress addr = InetAddress.getByName("localhost");
			        //parsed input
			System.out.print("node > ");
			//we try to create 


			GbnIO io = new GbnIO(sendMsg,bools);
			Thread h = new Thread(io);
			h.start();

			DatagramSocket sock = new DatagramSocket(selfPort);
			resendTask task = new resendTask(sock,sendMsg,addr,peerPort,windowSize,bools);
			ScheduledThreadPoolExecutor sched = new ScheduledThreadPoolExecutor(10);
			timer tm = new timer(task,sched);

			SenderSide sendRec = new SenderSide(sock,tm,sendMsg,addr,selfPort,peerPort,windowSize,mode,bools,dVal,pVal);//senderside receiving thread
            Thread k = new Thread(sendRec);
            k.start();


			SenderSide send = new SenderSide(sock,tm,sendMsg,addr,selfPort,peerPort,windowSize,0,bools,dVal,pVal);//senderside send thread
			Thread t = new Thread(send);
			t.start();

			//now we have parsed valid message packet to send
		}catch(Exception e){
			System.out.println("Get localhost address failed");
			System.exit(1);
		}

		

	}
	
}
