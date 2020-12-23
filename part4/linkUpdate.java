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

public class linkUpdate implements Runnable{
    HashMap<Integer,Double> nd;
	Queue<ByteBuffer> q;

	public linkUpdate(HashMap<Integer,Double> nd,Queue<ByteBuffer> q){
		this.nd = nd;
		this.q = q;
	}



	public void run(){
		while(true){
			while(q.size()==0){
			}
			ByteBuffer b = q.poll();
			int size = b.getInt();
            int port = b.getInt();
            int recSeq = b.getInt();
			double link = b.getDouble();
			nd.put(port,link);
//			System.out.printf("linke to %d updated to %f\n",port,link);
		}
	}

}
