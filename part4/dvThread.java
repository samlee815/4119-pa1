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


public class dvThread implements Runnable{
	HashMap<Integer,Double> dv;
    Queue<ByteBuffer> q;
    timer dvtimer;
	int localport;
	HashMap<Integer,HashMap<Integer,Double>> ndv;
	HashMap<Integer,Double> nd;
	HashMap<Integer,Integer> htable;
	boolean updated = false;

	public dvThread(HashMap<Integer,Double> dv,List<Integer> sendList,Queue<ByteBuffer> q,timer dvtimer,int localport,HashMap<Integer,HashMap<Integer,Double>> ndv,HashMap<Integer,Double> nd,HashMap<Integer,Integer> htable){
		this.dv = dv;
		this.q = q;
		this.dvtimer =dvtimer;
		this.localport = localport;
		this.ndv =ndv;
		this.nd = nd;
		this.htable = htable;


    }


	public boolean update(int node){
		double min = 100;
		int minhop = 0;
		double prev = dv.get(node);
		for(int hops:ndv.keySet()){
			HashMap<Integer,Double> temp =ndv.get(hops);
			if(temp.get(node) == null){
				continue;
			}
//			System.out.printf("From %d to %d distance is updated through hop %d: link%f +dis from hop %f\n",localport,node,hops,nd.get(hops),temp.get(node));
			double mid = temp.get(node)+nd.get(hops);
			if(mid<min){
				min = mid;
				minhop = hops;
			}
		}
		dv.put(node,min);
		htable.put(node,minhop);
		if(Math.abs(min - prev)>0.01){
			return true;
		}
		return false;

	}




    @Override
    public void run(){
		dvtimer.start();
		while(true){
			while(q.size()==0){

			}
			updated = false;
			ByteBuffer b = q.poll();
			int msgSize = b.getInt();
            int sourceport = b.getInt();
            int seqNum = b.getInt();
            int size = (msgSize - 16)/12;
			boolean update = false;

            HashMap<Integer,Double> sourceMap = new HashMap<>();
            for(int i =0;i<size;i++){
                int node = b.getInt();
                double distance = b.getDouble();
                sourceMap.put(node,distance);
            }
			sourceMap.put(sourceport,0.0);
			ndv.put(sourceport,sourceMap);



			for(int node :sourceMap.keySet()){
				if(node == localport){
					continue;
				}
				if(dv.containsKey(node)){
					updated=(updated || update(node));
				}else{
//					System.out.printf("From %d to %d distance is updated through hop %d: link%f +dis from hop %f\n",localport,node,sourceport,nd.get(sourceport),sourceMap.get(node));
					dv.put(node,nd.get(sourceport)+sourceMap.get(node));
					htable.put(node,sourceport);
				}
			}
			if(!updated){
				dvtimer.restart();
			}
		}

	}



}





