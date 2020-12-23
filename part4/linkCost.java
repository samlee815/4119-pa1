package pack4;
import java.util.TimerTask;
import java.util.LinkedList;
import java.net.DatagramSocket;
import java.io.BufferedWriter;
import java.nio.ByteBuffer;
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



public class linkCost implements Runnable{
	
	List<Integer> sendList;
	HashMap<Integer,Integer> sencount;
	HashMap<Integer,Integer> ackcount;
	DatagramSocket sock;
	InetAddress addr;
	HashMap<Integer,Double> nd;
	int localport;

	public linkCost(List<Integer> sendList,HashMap<Integer,Integer> sencount,HashMap<Integer,Integer> ackcount,DatagramSocket sock,InetAddress addr,HashMap<Integer,Double> nd,int localport){
		this.sendList = sendList;
		this.sencount = sencount;
		this.ackcount = ackcount;
		this.sock = sock;
		this.addr = addr;
		this.nd = nd;
		this.localport = localport;

	}


    public byte[] formMsg(int localport, double rate){
        int size = 24;
        ByteBuffer b = ByteBuffer.allocate(size);
        b.putInt(3);//0 represents the mode of sending distance vectore
        b.putInt(size);
        b.putInt(localport);
        b.putInt(0);
		b.putDouble(rate);
        byte ret[] = b.array();
        return ret;
    }


	public void run(){
		while(true){
			try{
			for(int port:sendList){
				double sent = Double.valueOf(sencount.get(port));
				double ack = Double.valueOf(ackcount.get(port));
				if(sent == 0.0){
					continue;
				}
				double rate = (sent-ack)/sent;
				nd.put(port,rate);
				Timestamp t = new Timestamp(System.currentTimeMillis());
				System.out.printf("[%s] Link to %d: %d packets sent, %d packets lost, loss rate %f\n",t.toString(),port,(int)sent,(int)(sent-ack),rate);
				byte buf[]=formMsg(localport,rate);
                DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,port);
                sock.send(pack);
			}



			TimeUnit.SECONDS.sleep(1);
			}catch(Exception e){}
		}
	}


}
