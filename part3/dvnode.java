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
import pack3.printTable;
import pack3.timer;


public class dvnode{


	public static void invalidArgs(){
        System.out.println("Usage : java dnode <local-port> <neighbor-port> <loss-rate> [ -d <value-of-n> j -p <value-of-p>]");
        System.exit(0);
    }

	public static int checkPort(int i){
		if((i<1024 || i > 65353)){
                System.out.println("Invalid port number");
                invalidArgs();
        }
		return i;
	}

	public static double checkProb(double i){
        if((i<0.0 || i > 1.0)){
                System.out.println("Invalid loss rate"+Double.toString(i));
                invalidArgs();
        }
        return i;
    }


	public static byte[] formMsg(HashMap<Integer,Double> table, int localport, int seq){
		int size = 12 + 12*table.size();
		ByteBuffer b = ByteBuffer.allocate(size);
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
	//udp packet format packet size|source|seqnumber|node 1|distance 1|...|node n|distance n|

	public static boolean decodeMsg(ByteBuffer b,Timestamp t,int localport, HashMap<Integer,Double> dt, HashMap<Integer,Integer> ht,List<Integer> nlist,List<Double> ndlist){
		boolean ret = false;
		try{
			int msgSize = b.getInt();
			int sourceport = b.getInt();
			int seqNum = b.getInt();
			double sourcedistance = ndlist.get(nlist.indexOf(sourceport));
			System.out.printf("%s Msg received at Node: %d from Node: %d\n",t.toString(),localport,sourceport);
			int size = (msgSize - 12)/12;
			for(int i =0;i<size;i++){
				int node = b.getInt();
				double distance = b.getDouble();
				if(dt.containsKey(node)){
					double prevDis = dt.get(node);
					if(prevDis>(distance+sourcedistance)){
						dt.replace(node,distance+sourcedistance);
						ht.replace(node,sourceport);
						Timestamp time = new Timestamp(System.currentTimeMillis());
						System.out.printf("%s Updating distance to %d ,distance %f\n",time.toString(),node,sourcedistance+distance);
						ret = true;
					}else{
						//update does not happen, do nothing
					}
				}else{
					// formerly unreachable node
					dt.put(node,sourcedistance+distance);
					ht.put(node,sourceport);
					Timestamp time = new Timestamp(System.currentTimeMillis());
					System.out.printf("%s adding to %d reachable nodes,distance %f\n",time.toString(),node,sourcedistance+distance);
					ret = true;
				}

		
			}
			return ret;
		}catch(Exception e){
			System.out.println("Exception catched");
			return ret;
		}
	}




	public static void main (String[] args) throws Exception{
		int seqNum = 0;
		try{
			boolean init = true;
			int localport;
			timer t;
			if(args.length<4){
				invalidArgs();
			}
			localport = checkPort(Integer.parseInt(args[0]));
			boolean last = false;
			ArrayList<Integer> neighborList = new ArrayList<>();
			ArrayList<Double> neiDistanceList = new ArrayList<>();
			for(int i = 1 ; i <args.length; i++){
				if(((i%2)== 1) && ((i==args.length-1) && args[i].equals("last"))){
					last = true;
					break;
				}
				if((i%2)==1){
					neighborList.add(checkPort(Integer.parseInt(args[i])));
				}else if((i%2) == 0){
					neiDistanceList.add(checkProb(Double.parseDouble(args[i])));
				}
			}
			neighborList.add(localport);
			neiDistanceList.add(0.0);



			//parsed command line input
			HashMap<Integer,Double> disTable = new HashMap<>();
			HashMap<Integer,Integer> hopTable = new HashMap<>();
			for(int i=0; i <neighborList.size();i++){
				disTable.put(neighborList.get(i),neiDistanceList.get(i));
				hopTable.put(neighborList.get(i),neighborList.get(i));
			}
			InetAddress addr = InetAddress.getByName("localhost");
			DatagramSocket sock = new DatagramSocket(localport);

			printTable printTask = new printTable(disTable,hopTable,localport);
            timer tm = new timer(printTask);

			if(last){
				byte buf[] = formMsg(disTable,localport,seqNum);
				for(int i : neighborList){
					if(i == localport){
						continue;
					}
					DatagramPacket pack = new DatagramPacket(buf,buf.length,addr,i);
					sock.send(pack);
					Timestamp timestamp = new Timestamp(System.currentTimeMillis());
					System.out.printf("%s Message sent from Node port: %d to Node port: %d\n",timestamp.toString(),localport,i);
				}
				init = false;
				tm.start();
			}


			while(true){
				byte buf[] = new byte[200];//8+12*16 which is the max number of nodes supported by us
				DatagramPacket request = new DatagramPacket(buf,buf.length);
				sock.receive(request);
				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				ByteBuffer b = ByteBuffer.allocate(200);
				b.put(buf);
				b.rewind();
				boolean update = decodeMsg(b,timestamp,localport,disTable,hopTable,neighborList,neiDistanceList);
				if(update || init ){
					byte b2[] = formMsg(disTable,localport,seqNum);
	                for(int i : neighborList){
                    	if(i == localport){
                        	continue;
                    	}
                    	DatagramPacket pack2 = new DatagramPacket(b2,b2.length,addr,i);
                    	sock.send(pack2);
                    	timestamp = new Timestamp(System.currentTimeMillis());
                    	System.out.printf("%s Message sent from Node port: %d to Node port: %d\n",timestamp.toString(),localport,i);
                	}
					if(init){
						tm.start();
					}else{
						tm.restart();
					}
                	init = false;
				}
			}




		}catch(NumberFormatException n){
			invalidArgs();
		}catch(Exception e){
			System.out.println("Binding port failed,please try another port number");
			System.exit(0);
		}
	}











}

