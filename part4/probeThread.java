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
import pack4.timer;



public class probeThread implements Runnable{
	
	HashMap<Integer,Integer> seqTable;
	HashMap<Integer,Integer> sencount;
	HashMap<Integer,Integer> ackcount;
	Queue<ByteBuffer> q;
	final int windowSize = 5;
	boolean init = true;
	List<Integer> sendList;
	List<Boolean> sendAwake;
	DatagramSocket sock;
	InetAddress addr;
	int localport;
	HashMap<Integer,timer> timerTable;



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


	public probeThread(HashMap<Integer,timer> timerTable,HashMap<Integer,Integer> seqTable,HashMap<Integer,Integer> sencount,HashMap<Integer,Integer> ackcount,Queue<ByteBuffer> q,List<Integer> sendList,List<Boolean> sendAwake,DatagramSocket sock,InetAddress addr,int localport){
		this.seqTable = seqTable;
		this.sencount = sencount;
		this.ackcount = ackcount;
		this.q = q;
		this.sendList = sendList;
		this.sendAwake = sendAwake;
		this.sock = sock;
		this.addr = addr;
		this.localport = localport;
		this.timerTable = timerTable;
	}

	@Override
	public void run(){
		if(init){
			try{
				for(int client : sendList){
						for(int i=0;i<windowSize;i++){
							byte buf[] = formMsg(localport,i);
							DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,client);
                    		sock.send(pack);
//							System.out.printf("packet sequence %d sent to port %d",i,client);
							int count = sencount.getOrDefault(client,0);
							sencount.put(client,count+1);
							timerTable.get(client).start();
						}	

				}
			}catch(IOException e){
//				System.out.println(" init wrong catched");
			}
		}
		init = false;
		while(true){
			try{
				while(q.size()==0){
	
				}
//				System.out.println("Parsing input");
				ByteBuffer b = q.poll();
				int size = b.getInt();
            	int port = b.getInt();
            	int recSeq = b.getInt();
				int count = ackcount.getOrDefault(port,0);
//				System.out.printf("Ack %d received from port %d\n",recSeq,port);
				ackcount.put(port,count+1);
//				System.out.printf("sent :%d acked:%d\n",sencount.get(port),ackcount.get(port));
				if(recSeq>seqTable.get(port)){
					int prev = seqTable.get(port);
					seqTable.replace(port,recSeq);
					for(int i = 0;i<(recSeq-prev+1);i++){
						byte buf[] = formMsg(localport,prev+windowSize+i);
						DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,port);
                        sock.send(pack);
//						System.out.printf("packet sequence %d sent to port %d\n",prev+windowSize+i,port);
						int scount = sencount.getOrDefault(port,0);
                        sencount.put(port,scount+1);
						if(i == 0){
							timerTable.get(port).restart();
						}
					}
				}
				if((recSeq == seqTable.get(port)) &&(recSeq == 0)){
                    int prev = seqTable.get(port);
                    seqTable.put(port,recSeq);
                    for(int i = 0;i<(recSeq-prev+1);i++){
                        byte buf[] = formMsg(localport,prev+windowSize+i);
                        DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,port);
                        sock.send(pack);
//						System.out.printf("packet sequence %d sent to port %d",prev+windowSize+i,port);
                        int scount = sencount.getOrDefault(port,0);
                        sencount.put(port,scount+1);

						if(i == 0){
                            timerTable.get(port).restart();//
                        }
                    }
                }
			}catch(Exception e){


				System.out.println("Exception handled");
			}
		}
	}

}
