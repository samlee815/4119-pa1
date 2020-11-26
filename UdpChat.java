import java.io.*;
import java.lang.*;
import java.util.*;
import java.net.*;
import java.nio.channels.*;
import mypack.Entry;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UdpChat {
	private static final String enc = "\u2561";	// our special delimiter and encryption of udp message

	public static void invalidArgs(){
		System.out.println("Server mode usage: java UdpChat -s <port>");
		System.out.println("Client mode usage: java UdpChat -c <nickname> <server-ip> <server-port> <client-port>");
		System.exit(0);
	}


	public static void transmitTable(HashMap<String,Entry> map,DatagramSocket servSock)throws Exception{
		for(String key : map.keySet()){
			InetAddress keyAddr = InetAddress.getByName(map.get(key).getIP());
			int keyPort = map.get(key).getPort();
			for(String k : map.keySet()){
				if(!key.equals(k)){
					byte buf[] = new byte[10000];
					System.out.println(map.get(k).format());
					String msg = "ent"+enc+k+enc+map.get(k).format();
					System.out.println(msg);
					buf = msg.getBytes();
					DatagramPacket entry = new DatagramPacket(buf,buf.length,keyAddr,keyPort);
					servSock.send(entry);
				}
			}
		}
	}

	public static void transmitOffmsg(String nickname,HashMap<String,ArrayList<String>> offmsg,DatagramSocket servSock,InetAddress clntAddr,int clntPort) throws Exception{		ArrayList<String> offMsg = offmsg.get(nickname);
		if(offMsg.size()==0){
			//do nothing
		}
		else{
			for(String s:offMsg){
				byte buf[] = new byte[2048];
				buf = s.getBytes();
				DatagramPacket send = new DatagramPacket(buf,buf.length,clntAddr,clntPort);
				servSock.send(send);

			}
			offMsg.clear();

		}
	}
	


	public static void serverLoop(int portNum) throws Exception{
		int recordCount = 0;
		HashMap<String,Entry> map = new HashMap<>();//the hashmap <key=nickname,value = entry>
		DatagramSocket servSock = new DatagramSocket(portNum);//throws Socket Exceptions
		HashMap<String,ArrayList<String>> offmsg = new HashMap<>();
		while(true){
			System.out.print(">>>");
			byte buf[] = new byte[2048];
			DatagramPacket request = new DatagramPacket(buf,buf.length);
			servSock.receive(request);
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			InetAddress reqAddr = request.getAddress();
			String reqAdd = reqAddr.getHostAddress();
			int reqPort = request.getPort();
			String received = new String(request.getData(),0,request.getLength());
			/* now we try to parse the message we receive
			 * we only send acks to three kind of packets: 
			 * 1.registration+nickname or reregistration+nickname
			 * 2.offline message sent to server
			 * 3.deregistration request
			 */
			String[] strings = received.split(enc);
			if(strings.length == 3){
				if(strings[0].equals("reg")){
					//registration request
					boolean rereg = false;
					int clntPort = Integer.parseInt(strings[2]);
					if(map.containsKey(strings[1])){
						map.get(strings[1]).update(reqAdd,clntPort,true);
						rereg = true;
					}
					else{
						map.put(strings[1],new Entry(reqAdd,clntPort));
						ArrayList<String> temp = new ArrayList<>();
						offmsg.put(strings[1],temp);
					}
					String res = "ack"+"\u2561"+"reg";
					buf = new byte[2048];
					buf = res.getBytes();
					DatagramPacket response = new DatagramPacket(buf,buf.length,reqAddr,clntPort);
					servSock.send(response);
					System.out.println("Client "+strings[1]+" is online");
					transmitTable(map,servSock);
					if(rereg){
						transmitOffmsg(strings[1],offmsg,servSock,reqAddr,clntPort);
					}
				}
				else if(strings[0].equals("der")){
					//deregistration request
					if(map.containsKey(strings[1])){
                        int clntPort = Integer.parseInt(strings[2]);
						map.get(strings[1]).update(reqAdd,reqPort,false);
                    	String res = "ack"+"\u2561"+"der";
						buf = new byte[2048];
                    	buf = res.getBytes();
						DatagramPacket response = new DatagramPacket(buf,buf.length,reqAddr,clntPort);
                    	servSock.send(response);
						System.out.println("Client "+strings[1]+" is offline");
						transmitTable(map,servSock);
					}					
				}else if(strings[0].equals("off")){
					if(map.containsKey(strings[1])){
						if(map.get(strings[1]).isonline()){
							//do nothing
						}else{
							//the 
							String temp =timestamp.toString()+strings[2];
							offmsg.get(strings[1]).add(strings[2]);
						}
					}					
				}
				
			}else{
				//Do nothing, discard the message.
			}
		}
	}	
	

	public static void clientLoop(String nickname,String servIP,int servPort,int clntPort)throws Exception {
		int recordCount = 0;
        boolean initReg = false;
		InetAddress servAddr = InetAddress.getByName(servIP);
	    HashMap<String,Entry> map = new HashMap<>();//the hashmap <key=nickname,value = entry>
	    DatagramSocket clntSock = new DatagramSocket(clntPort);//throws Socket Exceptions
		while(!initReg){
			byte buf[] = new byte[2048];
	        String s = "reg"+enc+nickname+enc+Integer.toString(clntPort);
	        buf = s.getBytes();        
	        DatagramPacket request = new DatagramPacket(buf,buf.length,servAddr,servPort);
	        clntSock.send(request);
			buf = new byte[2048];
            DatagramPacket servMes = new DatagramPacket(buf,buf.length);
            clntSock.receive(servMes);
			String received = new String(servMes.getData(),0,servMes.getLength());
            String[] split = received.split(enc);
			if(split[0].equals("ack") && split[1].equals("reg")){
				initReg = true;
			} 

		}//end of initial registration
		System.out.println("Welcome, you are registered");



		/*byte buf[] = new byte[2048];
        InetAddress servAddr = InetAddress.getByName(servIP);
		HashMap<String,Entry> map = new HashMap<>();//the hashmap <key=nickname,value = entry>
        DatagramSocket clntSock = new DatagramSocket(clntPort);//throws Socket Exceptions
		String s = "reg"+enc+nickname+enc+Integer.toString(clntPort);
		buf = s.getBytes();

		DatagramPacket request = new DatagramPacket(buf,buf.length,servAddr,servPort);
		clntSock.send(request);
	*/	
		while(true){
			byte[] buf = new byte[2048];
			DatagramPacket servMes = new DatagramPacket(buf,buf.length);
			clntSock.receive(servMes);

			String received = new String(servMes.getData(),0,servMes.getLength());
			System.out.println(received);
		}
	}	




	public static void main (String[] args) throws Exception{
		boolean servMode= false;
		int servPort = 0;
		String servIP = "";
		String nickname = "";
		int clntPort = 0;

		/* error checking of command line arguments
		 * also parse the command line argument 
		 */

		if(!(args.length == 2 || args.length == 5)){
			invalidArgs();
		}
		if(args.length == 2){
			try{
				if(!args[0].equals("-s")){
					invalidArgs();
				}
				servPort=Integer.parseInt(args[1]);
				servMode = true;
			}catch(NumberFormatException e){
					invalidArgs();
			}
		}
		if(args.length == 5){
            try{
                if(!args[0].equals("-c")){
                    invalidArgs();
                }
                nickname = args[1];
				servIP = args[2];
				servPort = Integer.parseInt(args[3]);
				clntPort = Integer.parseInt(args[4]);
				/* check if the servIP is valid IP address format*/
            	String[] nums=args[2].split("\\.");
				if(nums.length!=4){
					System.out.println("Invalid Server IP address format");
					throw new NumberFormatException();
				}
				for(String s:nums){
					int n =Integer.parseInt(s);
					if(n<0 || n> 255){
						System.out.println("Invalid server IP address format");
						throw new NumberFormatException();
					}
				}
			}catch(NumberFormatException e){
                    invalidArgs();
            }
        }
		if((servPort<1024 || servPort>65353)){
				System.out.println("Invalid server port number");
				invalidArgs();
		}
		if((!servMode) && ((clntPort<1024 || clntPort>65353))){
                System.out.println("Invalid client port number");
                invalidArgs();
        }
		/* now we have valid arguments parsed for both serMode and clntMode */
	
		if(servMode){
			try{
				serverLoop(servPort);

			}catch(Exception e){
				System.out.println("Exception in server");
			}
		}else{

			clientLoop(nickname,servIP,servPort,clntPort);
		}

	}
}

