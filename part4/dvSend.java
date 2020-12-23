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


public class dvSend extends TimerTask implements Runnable{
	List<Integer> n;
	HashMap<Integer,Double> dvector;
	int localport;
    DatagramSocket sock;
    InetAddress addr;
	HashMap<Integer,Integer> htable;

	public dvSend(HashMap<Integer,Integer> htable,List<Integer> n, HashMap<Integer,Double> dvector,int localport,DatagramSocket sock,InetAddress addr){
		this.n = n;
		this.dvector = dvector;
		this.localport = localport;
		this.sock = sock;
		this.addr = addr;
		this.htable = htable;
	} 

	public dvSend(dvSend other){
		this.n = other.n;
        this.dvector = other.dvector;
        this.localport = other.localport;
        this.sock = other.sock;
        this.addr = other.addr;
		this.htable = other.htable;
	}


	public byte[] formMsg(HashMap<Integer,Double> table, int localport, int seq){
        int size = 16 + 12*table.size();
        ByteBuffer b = ByteBuffer.allocate(size);
        b.putInt(0);//0 represents the mode of sending distance vectore
        b.putInt(size);
        b.putInt(localport);
        b.putInt(seq);
        for(int k:table.keySet()){
            b.putInt(k);
            b.putDouble(table.get(k));
//			System.out.printf("The message formed %d %f\n",k,table.get(k));


        }
        byte ret[] = b.array();
        return ret;
    }

	@Override
	public void run(){
		try{
            for(int i : n){
                    byte buf[] = formMsg(dvector,localport,0);
                    DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,i);
                    sock.send(pack);
//            		System.out.printf("Sending distance update to node %d\n",i);
			}
			Timestamp t = new Timestamp(System.currentTimeMillis());
			System.out.printf("\n[%s] Node %d Routing Table\n",t.toString(),localport);
			for(int port:dvector.keySet()){
				System.out.printf("- (%.3f) -> Node %d; Next hop -> Node %d\n",dvector.get(port),port,htable.get(port));

			}
			System.out.println();
        }catch(Exception e){
            System.out.println("error msg");
        }
	}

}
