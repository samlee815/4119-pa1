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



	public static void main (String[] args){
		try{
			int localport;
			if(args.length<4){
				invalidArgs();
			}
			localport = checkPort(Integer.parseInt(args[0]));
			boolean last = false;
			ArrayList<Integer> neighborList = new ArrayList<>();
			ArrayList<Double> distanceList = new ArrayList<>();
			for(int i = 1 ; i <args.length; i++){
				if(((i%2)== 1) && ((i==args.length-1) && args[i].equals("last"))){
					last = true;
					System.out.println("This is the last node");
					break;
				}
				if((i%2)==1){
					neighborList.add(checkPort(Integer.parseInt(args[i])));
				}else if((i%2) == 0){
					distanceList.add(checkProb(Double.parseDouble(args[i])));
				}
			}
			//parsed command line input
			HashMap<Integer,Double> table = new HashMap<>();
			



		}catch(NumberFormatException n){
			invalidArgs();
		}
	
	}











}

