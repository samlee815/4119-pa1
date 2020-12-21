package pack3;
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

public class printTable extends TimerTask{

	HashMap<Integer,Double> dt;
	HashMap<Integer,Integer> ht;
	int localport;

	//constructor
	public printTable(HashMap<Integer,Double> dt,HashMap<Integer,Integer> ht,int localport){
		this.dt = dt;
		this.ht = ht;
		this.localport = localport;
	}

	//copy constructor
	public printTable(printTable other){
		this.dt = other.dt;
		this.ht = other.ht;
		this.localport = other.localport;
	}


	public void run(){
		try{
			Timestamp t = new Timestamp(System.currentTimeMillis());
			System.out.printf("\n[%s] Node %d Routing table\n",t.toString(),localport);
			for(int i:dt.keySet()){
				double distance = dt.get(i);
				int nexthop = ht.get(i);
				System.out.printf("- (%f) -> Node %d; Next hop -> Node %d\n",distance,i,nexthop);
			}
					
		}catch(Exception e){

		}
	}

}
