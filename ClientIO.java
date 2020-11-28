package mypack;

import java.net.*; 
import java.util.*;
import mypack.Entry;
import java.util.concurrent.*;

public class ClientIO implements Runnable{
	private static final String enc = "\u2561";
	//DatagramSocket sock;
	List<String> msg;
	List<String> reg;
	//ConcurrentHashMap<String,Entry> map;	
	String nickname;
	//int clntPort;

    public ClientIO(List<String> msg,List<String> reg,String nickname){
        //this.sock = sock;
		this.msg = msg;
		this.reg = reg; 
		this.nickname = nickname;
    }



    @Override
    public void run(){
		Scanner scanner = new Scanner(System.in);
		while(scanner.hasNextLine()){
			String s = scanner.nextLine();
			String[] inputs = s.split(" ");
			System.out.print(">>>");
			if(inputs.length == 2){
				if(inputs[0].equals("reg")){
					String m = "reg" + enc + inputs[1];
//					System.out.println(m);//enqueue the reg request
					reg.add(m);
					this.nickname = inputs[1];//update the local nicjname
				}
				if(inputs[0].equals("dereg")){
					if(inputs[1].equals(nickname)){
						String m = "der" + enc + nickname;
						//enqueue the dereg request
//						System.out.println(m);
						reg.add(m);
					}
				}
			}
			if(inputs.length == 3){
				if(inputs[0].equals("send")){
					String m = inputs[1] + enc +inputs[2];
					msg.add(m);
//					System.out.println(m);
				}
			}
		}
    }
} 
