package pack2;

import pack2.resendTask;
import pack2.timer;
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

public class SenderSide implements Runnable{
	int seqNum;
	List<Character> msg;
	InetAddress addr;
	int selfPort;
	int peerPort;
	int packetSize;
	int winSize;
	int mode;
	List<Boolean> bools;
	timer t;
	DatagramSocket sock;
	char ack = '\u2561';

	int lastAckRec = -1;
	int lastSentStart = -1;
	int lastAckSender = - 1;
	int lastWindowStart = 0;
	int newWindowStart = 0;
	int dvalue = 0;
	double pvalue = 0.0;
	boolean parray[];
	int recCount = 0;
	int disCount = 0;



	public SenderSide(DatagramSocket sock,timer t,List<Character> msg,InetAddress addr,int selfPort,int peerPort,int winSize, int mode, List<Boolean> bools,

int dvalue,double pvalue){
		this.t = t;
        this.msg = msg;
        this.addr = addr;
		this.sock = sock;
        this.selfPort = selfPort;
		this.peerPort = peerPort;
	 	this.winSize = winSize;
		this.mode = mode;
		this.bools = bools;
		this.dvalue = dvalue;
		this.pvalue = pvalue;
		this.parray = prob(pvalue);
	}



	public boolean[] prob(double p){
		int i =(int) (p*1000);
		boolean ret[] = new boolean[1000];
		for(int k=0; k <1000 ;k++){
			if(k<i){
				ret[k]=true;
			}else{
				ret[k]=false;
			}

		}
		return ret;
	}




	public int findIndex(List<Boolean> bools){
        for(int i = 0;i <bools.size();i++){
            if(bools.get(i)==false){
                return i;
            }
        }
        return bools.size();
    }

	public void markBools(int lastAck){
		for(int i = 0; i <= lastAck; i++){
			bools.set(i,true);
		}
	}
	


	@Override
	public void run(){
		try{
		ByteBuffer b = ByteBuffer.allocate(6);
		boolean init = true;
		boolean sizechange = false;
		int msgSize = msg.size();
		if(mode == 0){
			//sending thread in the sender side
			for (;;){
				newWindowStart = findIndex(bools);
				while((newWindowStart <= lastWindowStart) && (!init)){
					newWindowStart = findIndex(bools);
					//if current acknowledged message is  
				}

				if(msgSize<msg.size()){
					msgSize = msg.size();
					sizechange = true;
				}
				if(((newWindowStart>=msgSize) && (msgSize!=0)) && (!sizechange) ){
					//if first to send
					break;
				}
				int i = 0;
				if(init){
					i = newWindowStart;
				}else{
					i = lastWindowStart + winSize; 
				}
				if(!init){
//					System.out.println(Integer.toString(lastWindowStart)+" "+Integer.toString(newWindowStart));
				}
				lastWindowStart = newWindowStart;
				for(; (i < (newWindowStart +winSize)) && i < msg.size() ; i++){
					b.clear();
					b.putInt(i);
					b.putChar(msg.get(i));
					byte[] buf = b.array();
					DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,peerPort);
					sock.send(pack);
					Timestamp timestamp = new Timestamp(System.currentTimeMillis());
					System.out.println(timestamp.toString()+" packet "+Integer.toString(i)+Character.toString(msg.get(i))+" sent");
					if(i==newWindowStart && init){
						t.start();
					}else if(i==newWindowStart){
						t.restart();
					}
					init = false;
            	}
				sizechange = false;
			}
			b.clear();
			b.putInt(-2);
			b.putChar('a');
			byte[] buf = b.array();
			for(;;){
				DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,peerPort);
				sock.send(pack);
			}



		}else if(mode == 1){
			//receving thread in the sending side
			//neither -d or -p is specified
			//this thread is responsible for both receiving in the sender and receiver
			int lastAck = -1;//variable
			while(true){
				byte buf[] = new byte[6];
            	DatagramPacket request = new DatagramPacket(buf,buf.length);
            	sock.receive(request);
				recCount = recCount + 1;
            	Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				b.clear();
				b.put(buf);
				b.rewind();
				int seq = b.getInt();
				char a = b.getChar(); 
				if(a == ack){
					// we are in the sender side;receiving acknowledgement
					if (seq >= lastAckSender){
						//if the ack sequence is larger than the current 
						markBools(seq);
						lastAckSender = seq;
						System.out.println(timestamp.toString()+" Ack "+Integer.toString(seq)+" received, window moves to "+Integer.toString(seq+1));
						t.restart();
						//we move the window size and then restart the timer	
					}
					if(seq == -2){
						System.out.println("[Summary] : No packets discarded");
						System.exit(0);
					}
					
				}else{
					//we are on the receiver side
					ByteBuffer b2 = ByteBuffer.allocate(6);
					if (seq == (lastAckSender+1)){
						System.out.println(timestamp.toString()+ " packet "+Integer.toString(seq)+Character.toString(a)+" received");
						b2.clear();
						b2.putInt(seq);
						b2.putChar(ack);
						b2.rewind();
						byte sendbuf[] = b2.array();
						DatagramPacket pack = new DatagramPacket(sendbuf,sendbuf.length,addr,peerPort);
						sock.send(pack);
						timestamp = new Timestamp(System.currentTimeMillis());
						System.out.println(timestamp.toString()+" Ack "+Integer.toString(seq)+" sent, expecting packet"+Integer.toString(seq+1));
						lastAckSender = seq;
					}else if(seq == -2){
						//we have received all the packets, terminate the program 
						b2.clear();
                        b2.putInt(-2);
                        b2.putChar(ack);
                        b2.rewind();
                        byte sendbuf[] = b2.array();
                        for(int j=0;j < 10;j++){
							DatagramPacket pack = new DatagramPacket(sendbuf,sendbuf.length,addr,peerPort);
                        	sock.send(pack);
						}
                        System.out.println("[Summary] :No packets discarded");//we know if we do not run -d -p we donot discard packets
						System.exit(0);
					}else{
						System.out.println(timestamp.toString()+ " packet "+Integer.toString(seq)+Character.toString(a)+" received");
						b2.clear();
                        b2.putInt(lastAckSender);
                        b2.putChar(ack);
                        b2.rewind();
                        byte sendbuf[] = b2.array();
                        DatagramPacket pack = new DatagramPacket(sendbuf,sendbuf.length,addr,peerPort);
                        sock.send(pack);
						timestamp = new Timestamp(System.currentTimeMillis());
						System.out.println(timestamp.toString()+" Ack "+Integer.toString(lastAckSender)+" sent, expecting packet"+Integer.toString(lastAckSender+1));
					}
				}


			}

		}else if(mode == 2){
            int lastAck = -1;//variable
			int count = 1;	
			while(true){
                byte buf[] = new byte[6];
                DatagramPacket request = new DatagramPacket(buf,buf.length);
                sock.receive(request);
				recCount = recCount+1;
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                b.clear();
                b.put(buf);
                b.rewind();
                int seq = b.getInt();
                char a = b.getChar();
				if((count % dvalue)== 0){
					if(a == ack){
						System.out.println(timestamp.toString()+" Ack "+Integer.toString(seq)+" discarded");
					}else{
						System.out.println(timestamp.toString()+" packet "+Integer.toString(seq)+Character.toString(a)+" discarded");
					}
					disCount = disCount+1;
				}
				else{
                if(a == ack){
                    // we are in the sender side;receiving acknowledgement
                    if (seq >= lastAckSender){
                        //if the ack sequence is larger than the current 
                        markBools(seq);
                        lastAckSender = seq;
                        System.out.println(timestamp.toString()+" Ack "+Integer.toString(seq)+" received, window moves to "+Integer.toString(seq+1));
						t.restart();
                        //we move the window size and then restart the timer    
                    }
                    if(seq == -2){
						Double ione = Double.valueOf(recCount);
						Double itwo = Double.valueOf(disCount);
						Double ratio = itwo/ione;
                        System.out.println("[Summary] :"+Integer.toString(disCount)+"/"+Integer.toString(recCount)+"packets discarded, loss rate = "+ratio.toString());
                        System.exit(0);
                    }
                }else{
                    //we are on the receiver side
                    ByteBuffer b2 = ByteBuffer.allocate(6);
                    if (seq == (lastAckSender+1)){
						System.out.println(timestamp.toString()+ " packet "+Integer.toString(seq)+Character.toString(a)+" received");
                        b2.clear();
                        b2.putInt(seq);
                        b2.putChar(ack);
                        b2.rewind();
                        byte sendbuf[] = b2.array();
                        DatagramPacket pack = new DatagramPacket(sendbuf,sendbuf.length,addr,peerPort);
                        sock.send(pack);
						timestamp = new Timestamp(System.currentTimeMillis());
                        System.out.println(timestamp.toString()+" Ack "+Integer.toString(seq)+" sent, expecting packet"+Integer.toString(seq+1));
                        lastAckSender = seq;
                    }else if(seq == -2){
                        //we have received all the packets, terminate the program 
                        b2.clear();
                        b2.putInt(-2);
                        b2.putChar(ack);
                        b2.rewind();
                        byte sendbuf[] = b2.array();
                        for(int j=0;j < 10;j++){
                            DatagramPacket pack = new DatagramPacket(sendbuf,sendbuf.length,addr,peerPort);
                            sock.send(pack);
                        }
                       	Double ione = Double.valueOf(recCount);
                        Double itwo = Double.valueOf(disCount);
                        Double ratio = itwo/ione;
                        System.out.println("[Summary] :"+Integer.toString(disCount)+"/"+Integer.toString(recCount)+" packets discarded, loss rate = "+ratio.toString());
                        System.exit(0);
                    }else{
						System.out.println(timestamp.toString()+ " packet "+Integer.toString(seq)+Character.toString(a)+" received");
                        b2.clear();
                        b2.putInt(lastAckSender);
                        b2.putChar(ack);
                        b2.rewind();
                        byte sendbuf[] = b2.array();
                        DatagramPacket pack = new DatagramPacket(sendbuf,sendbuf.length,addr,peerPort);
                        sock.send(pack);
                      	timestamp = new Timestamp(System.currentTimeMillis());
                        System.out.println(timestamp.toString()+" Ack "+Integer.toString(lastAckSender)+" sent, expecting packet"+Integer.toString(lastAckSender+1));
                    }
                }}
			count = count + 1;
			}

		}else if(mode == 3){
			//probability mode
			while(true){
                byte buf[] = new byte[6];
                DatagramPacket request = new DatagramPacket(buf,buf.length);
                sock.receive(request);
				recCount = recCount+1;
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                b.clear();
                b.put(buf);
                b.rewind();
                int seq = b.getInt();
                char a = b.getChar();
				int index = (int)(Math.random()*1000);
                if(parray[index]){
                    if(a == ack){
                    	System.out.println(timestamp.toString()+" Ack "+Integer.toString(seq)+" discarded");
					}else{
                    	System.out.println(timestamp.toString()+" packet "+Integer.toString(seq)+Character.toString(a)+" discarded");
					}
					disCount = disCount+1;
                }
				else{
                if(a == ack){
                    // we are in the sender side;receiving acknowledgement
                    if (seq >= lastAckSender){
                        //if the ack sequence is larger than the current 
                        markBools(seq);
                        lastAckSender = seq;
                        System.out.println(timestamp.toString()+" Ack "+Integer.toString(seq)+" received, window moves to "+Integer.toString(seq+1));
						t.restart();
                        //we move the window size and then restart the timer    
                    }
                    if(seq == -2){
                        Double ione = Double.valueOf(recCount);
                        Double itwo = Double.valueOf(disCount);
                        Double ratio = itwo/ione;
                        System.out.println("[Summary] :"+Integer.toString(disCount)+"/"+Integer.toString(recCount)+"packets discarded, loss rate = "+ratio.toString());

						System.exit(0);
                    }
				}else{
                    //we are on the receiver side
                    ByteBuffer b2 = ByteBuffer.allocate(6);
                    if (seq == (lastAckSender+1)){
						System.out.println(timestamp.toString()+ " packet "+Integer.toString(seq)+Character.toString(a)+" received");
                        b2.clear();
                        b2.putInt(seq);
                        b2.putChar(ack);
                        b2.rewind();
                        byte sendbuf[] = b2.array();
                        DatagramPacket pack = new DatagramPacket(sendbuf,sendbuf.length,addr,peerPort);
                        sock.send(pack);
                        timestamp = new Timestamp(System.currentTimeMillis());
                        System.out.println(timestamp.toString()+" Ack "+Integer.toString(seq)+" sent, expecting packet"+Integer.toString(seq+1));
						lastAckSender = seq;
                    }else if(seq == -2){
                        //we have received all the packets, terminate the program 
                        b2.clear();
                        b2.putInt(-2);
                        b2.putChar(ack);
                        b2.rewind();
                        byte sendbuf[] = b2.array();
                        for(int j=0;j < 10;j++){
                            DatagramPacket pack = new DatagramPacket(sendbuf,sendbuf.length,addr,peerPort);
                            sock.send(pack);
                        }
                        Double ione = Double.valueOf(recCount);
                        Double itwo = Double.valueOf(disCount);
                        Double ratio = itwo/ione;
                        System.out.println("[Summary] :"+Integer.toString(disCount)+"/"+Integer.toString(recCount)+" packets discarded, loss rate = "+ratio.toString());
						System.exit(0);
                    }else{
						System.out.println(timestamp.toString()+ " packet "+Integer.toString(seq)+Character.toString(a)+" received");
                        b2.clear();
                        b2.putInt(lastAckSender);
                        b2.putChar(ack);
                        b2.rewind();
                        byte sendbuf[] = b2.array();
                        DatagramPacket pack = new DatagramPacket(sendbuf,sendbuf.length,addr,peerPort);
                        sock.send(pack);
						timestamp = new Timestamp(System.currentTimeMillis());
                        System.out.println(timestamp.toString()+" Ack "+Integer.toString(lastAckSender)+" sent, expecting packet"+Integer.toString(lastAckSender+1));
                    }
                }}
            }
		}

		}catch(IOException e){
		}
	}




}
