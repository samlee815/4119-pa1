package pack2;
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






public class resendTask extends TimerTask implements Runnable{
    List<Character> msg;
    InetAddress addr;
    int selfPort;
    int peerPort;
    int winSize;
    List<Boolean> bools;
	DatagramSocket sock;



	public resendTask(DatagramSocket sock,List<Character> msg,InetAddress addr ,int peerPort, int winSize ,List<Boolean> bools){
		this.sock = sock;
		this.bools = bools;
		this.winSize = winSize;
		this.peerPort = peerPort;
//		this.curSeq = 0;
		this.msg = msg;
		this.addr = addr;
	}



	public resendTask(resendTask other){
		this.sock = other.sock;
		this.bools = other.bools;
		this.winSize = other.winSize;
		this.peerPort = other.peerPort;
		this.msg = other.msg;
		this.addr = other.addr;
	}

	public int findIndex(List<Boolean> bools){
		for(int i = 0;i <bools.size();i++){
			if(bools.get(i)==false){
				return i;
			}

		}
		return bools.size();
	}



	


	public void run(){
		try{
			int toSend = findIndex(bools);
			int sendSize = Math.min(winSize,(msg.size()-toSend));
//			System.out.println(Integer.toString(sendSize));
			ByteBuffer b =ByteBuffer.allocate(6);
			//System.out.println("resending packet");
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			System.out.println(timestamp.toString()+" Packet "+Integer.toString(toSend)+" timeout");
			for(int i=0;i<sendSize;i++){
//				System.out.println("resending packet");
				b.clear();
				char temp = msg.get(toSend+i);
				b.putInt(toSend+i);
				b.putChar(temp);
				byte buf[] = b.array();
				DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,peerPort);
				sock.send(pack);
				timestamp = new Timestamp(System.currentTimeMillis());
				System.out.println(timestamp.toString()+" packet "+Integer.toString(toSend+i)+Character.toString(temp)+" sent");
			}
		}
		catch(Exception e){

	
		}
	}

}
