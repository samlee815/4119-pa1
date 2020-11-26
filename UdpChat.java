import java.io.*;
import java.lang.*;
import java.util.*;
import java.net.*;
import java.nio.channels.*;
import mypack.Entry;

public class UdpChat {
	private static final String enc = "\u2561";	// our special delimiter and encryption of udp message

	public static void invalidArgs(){
		System.out.println("Server mode usage: java UdpChat -s <port>");
		System.out.println("Client mode usage: java UdpChat -c <nickname> <server-ip> <server-port> <client-port>");
		System.exit(0);
	}


	


	public static void serverLoop(int portNum) throws Exception{
		int recordCount = 0;
		byte buf[] = new byte[2048];
		HashMap<String,Entry> map = new HashMap<>();//the hashmap <key=nickname,value = entry>
		DatagramSocket servSock = new DatagramSocket(portNum);//throws Socket Exceptions
		while(true){
			System.out.print(">>>");
			DatagramPacket request = new DatagramPacket(buf,buf.length);
			servSock.receive(request);
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
			if(strings.length == 2){
				if(strings[0].equals("reg")){
					//registration request
					if(map.containsKey(strings[1])){
						map.get(strings[1]).update(reqAdd,reqPort,true);
					}
					else{
						map.put(strings[1],new Entry(reqAdd,reqPort));
					}
					received="ack"+"\u2561"+"reg";
					buf = received.getBytes();
					DatagramPacket response = new DatagramPacket(buf,buf.length,reqAddr,reqPort);
					servSock.send(response);
				}else if(strings[0].equals("der")){
					//deregistration request
					if(map.containsKey(strings[1])){
                        map.get(strings[1]).update(reqAdd,reqPort,false);
                    	received="ack"+"\u2561"+"der";
                    	buf = received.getBytes();
                    	DatagramPacket response = new DatagramPacket(buf,buf.length,reqAddr,reqPort);
                    	servSock.send(response);
					}					
				}
				System.out.println("Client "+strings[1]+" is online");
				
			}
			else if(strings.length == 3){
					
			}
			else{
				//Do nothing, discard the message.
			}
		}
	}	
	

	public static void clientLoop(String nickname,String servIP,int servPort,int clntPort)throws Exception {
		int recordCount = 0;
        byte buf[] = new byte[2048];
        InetAddress servAddr = InetAddress.getByName(servIP);
		HashMap<String,Entry> map = new HashMap<>();//the hashmap <key=nickname,value = entry>
        DatagramSocket clntSock = new DatagramSocket(clntPort);//throws Socket Exceptions
		String s = "req"+enc+nickname;
		buf = s.getBytes();

		DatagramPacket request = new DatagramPacket(buf,buf.length,servAddr,servPort);
		clntSock.send(request);
		

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

