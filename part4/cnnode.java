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
import pack4.probeThread;
import pack4.ackThread;
import pack4.dvThread;
import pack4.dvSend;
import pack4.resendProbe;
import pack4.linkCost;
import pack4.linkUpdate;
import pack4.timer;


public class cnnode{


	public static void invalidArgs(){
        System.out.println("Usage : java cnnode <local-port> receive [<neighbor-port> <loss-rate>] send [<neighbor-port> <loss-rate>][last]");
        System.exit(0);
    }

    public static int checkPort(int i){
        if((i<1024 || i > 65353)){
                System.out.println("Invalid port number");
                invalidArgs();
        }
        return i;
    }


	public static byte[] formMsg(HashMap<Integer,Double> table, int localport, int seq){
        int size = 16 + 12*table.size();
        ByteBuffer b = ByteBuffer.allocate(size);
        b.putInt(0);//0 represents the mode of sending distance vectore
		b.putInt(size);
        b.putInt(localport);
        b.putInt(seq);
        for(int k:table.keySet()){
            b.putInt(k);
            b.putDouble(table.get(k));
        }
        byte ret[] = b.array();
        return ret;

    }




    public static double checkProb(double i){
        if((i<0.0 || i > 1.0)){
                System.out.println("Invalid loss rate"+Double.toString(i));
                invalidArgs();
        }
        return i;
    }


	public static void initDv(HashMap<Integer,HashMap<Integer,Double>> ndv,HashMap<Integer,Double> dvector,HashMap<Integer,Integer> htable,ByteBuffer b,List<Integer> sendList,List<Integer> neighbor,List<Boolean> sendAwake,InetAddress addr,DatagramSocket sock,int localport){
		//first packet on 
		try{
			int msgSize = b.getInt();
            int sourceport = b.getInt();
            int seqNum = b.getInt();
			int size = (msgSize - 16)/12;
			sendAwake.set(sendList.indexOf(sourceport),true);
			double sourcedistance = dvector.get(sourceport);
			HashMap<Integer,Double> sourceMap = new HashMap<>();
			for(int i =0;i<size;i++){
                int node = b.getInt();
                double distance = b.getDouble();
				if(node == localport){
					continue;
				}
				if(dvector.containsKey(node)){
					dvector.replace(node,0.0);
				}else{
					dvector.put(node,0.0);
				}
				htable.put(node,sourceport);
				sourceMap.put(node,0.0);
			System.out.printf("%d %f\n",node,distance+sourcedistance);
           	}
			sourceMap.put(sourceport,0.0);
			ndv.put(sourceport,sourceMap);
			byte[] buf = formMsg(dvector,localport,0);
			for(int dest:neighbor){
				DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,dest);
                sock.send(pack);
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
//                System.out.printf("%s Message sent from Node port: %d to Node port: %d\n",timestamp.toString(),localport,dest);

			}
		}catch(Exception e){

		}
	}




	public static void main (String[] args){
		try{
			boolean last = false;
			boolean init = true;
			ArrayList<Integer> recList = new ArrayList<>();
			ArrayList<Double> recProb = new ArrayList<>();
			ArrayList<Integer> neighbor = new ArrayList<>();
			HashMap<Integer,Double> nd = new HashMap<>();
			ArrayList<Integer> sendList = new ArrayList<>();
			ArrayList<Boolean> sendAwake = new ArrayList<>();
			HashMap<Integer,Double> recMap = new HashMap<>();//the <receive from, discard probability> pair hashmap
			HashMap<Integer,Double> dvector = new HashMap<>();
			HashMap<Integer,Integer> hvector = new HashMap<>();
			Queue<ByteBuffer> ackmsg = new ConcurrentLinkedDeque<>();
			Queue<ByteBuffer> dvmsg = new ConcurrentLinkedDeque<>();
			Queue<ByteBuffer> pbmsg = new ConcurrentLinkedDeque<>();
			Queue<ByteBuffer> linkmsg = new ConcurrentLinkedDeque<>();
			HashMap<Integer,Integer> sencount = new HashMap<>();
			HashMap<Integer,Integer> ackcount = new HashMap<>();
			HashMap<Integer,Integer> ackSeq = new HashMap<>();
			HashMap<Integer,Integer> probSeq = new HashMap<>(); 
			HashMap<Integer,timer> timerTable = new HashMap<>();
			HashMap<Integer,HashMap<Integer,Double>> dv = new HashMap<>();




			if(args.length<3){
				invalidArgs();
			}
			int localport = checkPort(Integer.parseInt(args[0]));
			if(!args[1].equals("receive")){
				invalidArgs();
			}
			int sendArgIndex = 0;
			for(int i =2 ;i<args.length;i++){
				if(args[i].equals("send")){
					sendArgIndex = i;
					break;
				}
				if((i%2) == 0){
					recList.add(checkPort(Integer.parseInt(args[i])));
					neighbor.add(checkPort(Integer.parseInt(args[i])));
					nd.put(checkPort(Integer.parseInt(args[i])),0.0);
					ackSeq.put(checkPort(Integer.parseInt(args[i])),0);
				}else{
					recProb.add(checkProb(Double.parseDouble(args[i])));
				}
			}
			for(int i =1 ;i<(args.length -sendArgIndex);i++){
				if(args[i+sendArgIndex].equals("last")){
					last = true;
					break;
				}
				sendList.add(checkPort(Integer.parseInt(args[i+sendArgIndex])));
				sendAwake.add(false);
				neighbor.add(checkPort(Integer.parseInt(args[i+sendArgIndex])));
				nd.put(checkPort(Integer.parseInt(args[i+sendArgIndex])),0.0);
				probSeq.put(checkPort(Integer.parseInt(args[i+sendArgIndex])),0);
			}

			for(int i:recList){
				recMap.put(i,recProb.get(recList.indexOf(i)));
			}
			for(int i:neighbor){
				dvector.put(i,0.0);
			}

			ArrayList<resendProbe> probelist = new ArrayList<>();
			InetAddress addr = InetAddress.getByName("localhost");
        	DatagramSocket sock = new DatagramSocket(localport);
			ScheduledThreadPoolExecutor sched = new ScheduledThreadPoolExecutor(200);
			sched.setRemoveOnCancelPolicy(true);
			for(int port:sendList){
                resendProbe temp = new resendProbe(probSeq,sencount,localport,port,sock,addr);
                probelist.add(temp);
                timer t = new timer(temp,sched,500,500);
                timerTable.put(port,t);
            }


			





		//if we are the last thread specified, we initate the network by first send out the dv of our selves
			if(last){
				   	byte buf[] = formMsg(dvector,localport,0);
                	for(int i : neighbor){
                    	if(i == localport){
                        	continue;
	                    }
    	                DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,i);
        	            sock.send(pack);
            	        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
//                	    System.out.printf("%s Message sent from Node port: %d to Node port: %d\n",timestamp.toString(),localport,i);
              		}
                	//init = false;
        //        tm.start();
			}

			probeThread probe = new probeThread(timerTable,probSeq,sencount,ackcount,ackmsg,sendList,sendAwake,sock,addr,localport);
			Thread t1 = new Thread(probe);
			ackThread ackt = new ackThread(ackSeq,recMap,pbmsg,sock,localport,addr);
            Thread t2 = new Thread(ackt);
			dvSend dvs = new dvSend(hvector,neighbor,dvector,localport,sock,addr);//TODO
			timer dvtimer = new timer(dvs,sched,500,500);
			dvThread dvt = new dvThread(dvector,sendList,dvmsg,dvtimer,localport,dv,nd,hvector);
			Thread t3 = new Thread(dvt);
			linkCost linkCost = new linkCost(sendList,sencount,ackcount,sock,addr,nd,localport);
			Thread t4 = new Thread(linkCost);
			linkUpdate l2 = new linkUpdate(nd,linkmsg);
			Thread t5 = new Thread(l2);



			while(true){
            	    byte buf[] = new byte[512];//8+12*16 which is the max number of nodes supported by us
                	DatagramPacket request = new DatagramPacket(buf,buf.length);
                	sock.receive(request);
                	Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                	ByteBuffer b = ByteBuffer.allocate(512);
                	b.put(buf);
                	b.rewind();
					int mode = b.getInt();
					if(init && (mode == 0)){
						if(!last){
							initDv(dv,dvector,hvector,b,sendList,neighbor,sendAwake,addr,sock,localport);
						}
						init = false;
						t1.start();
						t2.start();
						t3.start();
						t4.start();
						t5.start();
						continue;
					}
					if(mode == 0){
//						System.out.println("Queueing distance update ");
						dvmsg.offer(b);
						continue;//rely on dvupdate thread to deal dv update msg
					}
					if(mode == 1){
//						System.out.println("Queueing probing mesage");
						pbmsg.offer(b);// rely on acking thread to deal with probing packets
						continue;
					}
					if(mode == 2){
//						System.out.println("Queueing acks");
						ackmsg.offer(b);//rely on probing thread to deal with counting and distance vector counting
						continue;
					}
					if(mode == 3){
//						System.out.println("Queueing link updates");
						linkmsg.offer(b);
					}
			}

		}catch(Exception e){
			System.out.println("Invalid port or command line argument");
			System.exit(0);
		}
	}

}
