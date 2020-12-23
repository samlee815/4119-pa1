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


public class resendProbe extends TimerTask{

   

	HashMap<Integer,Integer> seqTable;
	HashMap<Integer,Integer> sencount;
    int localport;
	int clnt;
	final int windowSize = 5;
    DatagramSocket sock;
	InetAddress addr;


    //constructor
    public resendProbe(HashMap<Integer,Integer> seqTable,HashMap<Integer,Integer> sencount,int localport,int clnt,DatagramSocket sock,InetAddress addr){
        this.seqTable = seqTable;
        this.sencount = sencount;
        this.localport = localport;
		this.clnt = clnt;
		this.sock = sock;
		this.addr = addr;
    }

    //copy constructor
    public resendProbe(resendProbe other){
    	this.seqTable = other.seqTable;
        this.sencount = other.sencount;
        this.localport = other.localport;
        this.clnt = other.clnt;
        this.sock = other.sock;
        this.addr = other.addr;


	}


	public static byte[] formMsg(int localport, int seq){
        int size = 16;
        ByteBuffer b = ByteBuffer.allocate(size);
        b.putInt(1);//0 represents the mode of sending distance vectore
        b.putInt(size);
        b.putInt(localport);
        b.putInt(seq);
        byte ret[] = b.array();
        return ret;

    }



    public void run(){
        try{

			int k = seqTable.get(clnt);
			for(int i=0;i<windowSize;i++){
					byte buf[] = formMsg(localport,i+k);
					DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,clnt);
                    sock.send(pack);
//                    System.out.printf("Timeout packet sequence %d sent to port %d",i+k,clnt);
                    int count = sencount.getOrDefault(clnt,0);
                    sencount.put(clnt,count+1);
             }
        }catch(Exception e){
			System.out.println("error msg");
        }
    }

}

