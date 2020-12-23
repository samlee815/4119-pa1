package pack4;

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

public class ackThread implements Runnable{
   
    HashMap<Integer,Integer> sequenceHash;
	HashMap<Integer,Double> probTable;  
	Queue<ByteBuffer> q;
	DatagramSocket sock;
	int localport;
	InetAddress addr;



    public ackThread(HashMap<Integer,Integer> sequenceHash, HashMap<Integer,Double> probTable, Queue<ByteBuffer> q,DatagramSocket sock,int localport,InetAddress addr){
        this.sequenceHash = sequenceHash;
		this.probTable = probTable;
		this.q = q;
		this.sock = sock;
		this.localport = localport;
		this.addr = addr;
    }


	public boolean prob(double p){
		int i =(int) (p*1000);
        boolean ret[] = new boolean[1000];
        for(int k=0; k <1000 ;k++){
            if(k<i){
                ret[k]=true;
            }else{
                ret[k]=false;
            }

        }
		int index = (int)(Math.random()*1000);
        return ret[index];



	}


	public byte[] formMsg(int localport, int seq){
        int size = 16; 
        ByteBuffer b = ByteBuffer.allocate(size);
        b.putInt(2);//0 represents the mode of sending distance vectore
        b.putInt(size);
        b.putInt(localport);
        b.putInt(seq);
        byte ret[] = b.array();
        return ret;
    }


    @Override
    public void run(){

		while(true){
//			System.out.println("receving thread runs");
			try{
			while(q.size()==0){
			}
			//qsize is not zero
			ByteBuffer b = q.poll();
			int size = b.getInt();
			int port = b.getInt();
			int recSeq = b.getInt();
//			System.out.println("received packets");
			if(prob(probTable.get(port))){
				// discard package
			}else{
//				System.out.println();
				if(recSeq>=sequenceHash.get(port)){
					sequenceHash.replace(port,recSeq);
					byte buf[]=formMsg(localport,recSeq);
					DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,port);
					sock.send(pack);
					Timestamp timestamp = new Timestamp(System.currentTimeMillis());
//                    System.out.printf("%s Ack sent from Node port: %d to Node port: %d %d\n",timestamp.toString(),localport,port,recSeq);
				}else{
					byte buf[]=formMsg(localport,sequenceHash.get(port));
                    DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,port);
                    sock.send(pack);
					Timestamp timestamp = new Timestamp(System.currentTimeMillis());
//                    System.out.printf("%s Ack sent from Node port: %d to Node port: %d %d\n",timestamp.toString(),localport,port,sequenceHash.get(port));

				}

			}
			}catch(Exception e){

			}

		}



    }

}    
