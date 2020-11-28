import java.io.*;
import java.lang.*;
import java.util.*;
import java.net.*;
import java.nio.channels.*;
import mypack.Entry;
import mypack.ClientIO;
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
					byte buf[] = new byte[2048];
					String msg = "ent"+enc+k+enc+map.get(k).format();
					buf = msg.getBytes();
					DatagramPacket entry = new DatagramPacket(buf,buf.length,keyAddr,keyPort);
					servSock.send(entry);
				}
			}
			byte buf[] = new byte[2048];
			String msg = "ent"+enc+"end";
            buf = msg.getBytes();
            DatagramPacket entry = new DatagramPacket(buf,buf.length,keyAddr,keyPort);
            servSock.send(entry);
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
			received.trim();
			/* now we try to parse the message we receive
			 * we only send acks to three kind of packets: 
			 * 1.registration+nickname or reregistration+nickname
			 * 2.offline message sent to server
			 * 3.deregistration request
			 */
			System.out.println(received);//
			String[] strings = received.split(enc);
			if(strings.length == 3){
				if(strings[0].equals("reg")){
					//registration request
					boolean rereg = false;
					boolean reg = false;
					boolean dup = false;
					int clntPort = Integer.parseInt(strings[2]);
					if(map.containsKey(strings[1])){
						if(!map.get(strings[1]).isonline()){
							map.get(strings[1]).update(reqAdd,clntPort,true);
							rereg = true;
							reg = true;
						}else{

							dup = true;
						}
					}
					else{
						map.put(strings[1],new Entry(reqAdd,clntPort));
						ArrayList<String> temp = new ArrayList<>();
						offmsg.put(strings[1],temp);
						reg = true;
					}
					if (reg){
						String res = "ack"+"\u2561"+"reg";
						buf = new byte[2048];
						buf = res.getBytes();
						DatagramPacket response = new DatagramPacket(buf,buf.length,reqAddr,clntPort);
						servSock.send(response);
						System.out.println("Client "+strings[1]+" is online");
						transmitTable(map,servSock);

					}
					
					if (dup){
						String res = "ack"+"\u2561"+"dec";
                        buf = new byte[2048];
                        buf = res.getBytes();
                        DatagramPacket response = new DatagramPacket(buf,buf.length,reqAddr,clntPort);
                        servSock.send(response);
                        System.out.println("Decling duplicated nicknames");
					}

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
				}
			}else if(strings.length == 4){  
				if(strings[0].equals("off")){
                    if(map.containsKey(strings[1])){
                        if(map.get(strings[1]).isonline()){
                            //send back the the table entry
							//because of 
                        }else{
                            //the 
                            String temp ="off"+enc+"<"+strings[2]+"> "+timestamp.toString()+" "+strings[3];
                            offmsg.get(strings[1]).add(strings[3]);
                        }
                    }
                }
			}// offline msg sent to server has format off|dest|sender|msg
		}
	}	

	public static boolean compareLoop(String msg,DatagramSocket clntSock,HashMap<String,Entry> map) throws Exception{
		boolean ret = true;
		String e="ack"+enc;
		try{
compare:
			for(;;){
				byte buf[] = new byte[2048];
				DatagramPacket pack = new DatagramPacket(buf,buf.length);
				clntSock.receive(pack);
				String received = new String(pack.getData(),0,pack.getLength());
				String[] s=received.split(enc);
				if(received.replace(e,"").equals(msg)){
					//message received
					break compare;
				}
				if(s.length == 2){
					if(s[0].equals("ent") && s[1].equals("end")){
						System.out.println("[Client table updated]");
					}else if(s[0].equals("off")){
                        System.out.println("Offline msg :"+s[1]);
                    }
				}else if (s.length == 4){

					if(s[0].equals("msg")){
						String ack =e+received;
						buf = new byte[2048];
						buf = ack.getBytes();
						int reqPort = pack.getPort();
						InetAddress reqAddr = pack.getAddress();
						pack = new DatagramPacket(buf,buf.length,reqAddr,reqPort);
						clntSock.send(pack);
						System.out.println(s[2]+":"+s[3]);
					}



				}
				
			}
		}catch(SocketTimeoutException ex){

			ret = false;
		}

		return ret;
	}
	

	public static void clientLoop(String nickname,String servIP,int servPort,int clntPort)throws Exception {
		//int recordCount = 0;
        boolean initReg = false;
		boolean clntOnline = false;
		InetAddress servAddr = InetAddress.getByName(servIP);
		InetSocketAddress servSockAddr = new InetSocketAddress(servIP,servPort);	    

		HashMap <String,Entry> map = new HashMap<String,Entry>();
	    DatagramChannel clntChannel = DatagramChannel.open();
		clntChannel.socket().bind(new InetSocketAddress(clntPort));
		DatagramSocket clntSock = clntChannel.socket();
		ArrayList<String> msg = new ArrayList<>();
		List<String> synmsg = Collections.synchronizedList(msg); 
		ArrayList<String> regis = new ArrayList<>();
		List<String> synreg = Collections.synchronizedList(regis);
		//Selector selector = Selector.open();	
	
		System.out.print(">>>");
		while(!initReg){
			byte buf[] = new byte[2048];
	        String s = "reg" + enc + nickname + enc + Integer.toString(clntPort);
	        buf = s.getBytes();   
			DatagramPacket send = new DatagramPacket(buf,buf.length,servAddr,servPort);
			clntSock.send(send);
			buf = new byte[2048];
            DatagramPacket servMes = new DatagramPacket(buf,buf.length);
            clntSock.receive(servMes);
			String received = new String(servMes.getData(),0,servMes.getLength());
            //String received = new String(buf.array(),"UTF-8");

			//System.out.println(received);
			String[] split = received.split(enc);
			if(split[0].equals("ack") && split[1].equals("reg")){
				initReg = true;
				clntOnline = true;
			}
			if(split[0].equals("ack") && split[1].equals("dec")){
                System.out.println("[The nickname is occupied. Process exited]");
				System.exit(1);
            }

		}//end of initial registration
		System.out.println("[Welcome, you are registered]");
		System.out.print(">>>");
		
		//create a thread to deal with stdin input	
		ClientIO c = new ClientIO(synmsg,synreg,nickname);	
		Thread thread = new Thread(c);
        thread.start();


		while(true) {
			clntSock.setSoTimeout(500);
			while (synreg.size() != 0) {
				//we have pending registration msg
				String entry = synreg.get(0);
				//System.out.println(entry);
				synreg.remove(0);
				String temp[] = entry.split(enc);
				if(temp[0].equals("reg")){
					String nick = temp[1];//registration
					String send = "reg"+enc+nick+enc+Integer.toString(clntPort);
					int count = 0;
outloopone:
					for(count = 0; count < 5;){
						byte buf[] = new byte[2048];
						buf = send.getBytes();
            			DatagramPacket mes = new DatagramPacket(buf,buf.length,servAddr,servPort);
						clntSock.send(mes);
						System.out.println(count);
						buf = new byte[2048];	
						DatagramPacket servMes = new DatagramPacket(buf,buf.length);
            			try{
							for(;;){
							clntSock.receive(servMes);
							String received = new String(servMes.getData(),0,servMes.getLength());
							String[] s=received.split(enc);
							if(s.length == 2){
								if(s[0].equals("ack")&&s[1].equals("reg")){
									System.out.println("[Welcome, you are registered]");
									System.out.print(">>>");
									break outloopone;
								}else if(s[0].equals("ack")&&s[1].equals("dec")){
									System.out.println("[The nickname is occupied. Process exited]");
                                    System.exit(1);
								}
							}}

						}catch(SocketTimeoutException e){
							count++;
						}
					}
					if(count>=5){
						System.out.println("[Server not responding]");
                        System.out.println("[Exiting]");
						System.exit(1);
					}//timeout five times
					nickname = nick;
					clntOnline = true;
				}else{
					String nick = temp[1];
					if(nick.equals(nickname));
					String send = "der"+enc+nick+enc+Integer.toString(clntPort);
					int count = 0;
outlooptwo:
					for(count = 0; count < 5;){
                        byte buf[] = new byte[2048];
                        buf = send.getBytes();
                        DatagramPacket mes = new DatagramPacket(buf,buf.length,servAddr,servPort);
                        clntSock.send(mes);
                        System.out.println(count);
                        buf = new byte[2048];
                        DatagramPacket servMes = new DatagramPacket(buf,buf.length);
                        try{
                            for(;;){
                            clntSock.receive(servMes);
                            String received = new String(servMes.getData(),0,servMes.getLength());
                            String[] s=received.split(enc);
                            if(s.length == 2){
                                if(s[0].equals("ack")&&s[1].equals("der")){
                                    System.out.println("[You are offline, bye]");
                                    System.out.print(">>>");
                                    break outlooptwo;
                                }
                            }}

                        }catch(SocketTimeoutException e){
                            count++;
                        }
						if(count>=5){
							System.out.println("[Server not responding]");
                        	System.out.println("[Exiting]");
                        	System.exit(1);
						}
						clntOnline = false ;
					}			
					//dereg complete 	
				}
			}
			//process reg/dereg message queue complete

			if (!clntOnline){
				continue;			
			}
			// if the 

			while(synmsg.size()!=0){
                //we have pending message and we dequeue the message and process it
                String entry = synmsg.get(0);
                synmsg.remove(0);
                String s[]=entry.split(enc);
                String target = s[0];
                String mes = s[1];
                if(map.containsKey(target)){
                    boolean tarOnline = true;
					if(map.get(target).isonline()){
                        String send = "msg"+enc+target+enc+nickname+enc+mes;
                        System.out.println(send);
						Entry tar = map.get(target);
						byte buf[] = new byte[2048];
                        buf = send.getBytes();
                        DatagramPacket se = new DatagramPacket(buf,buf.length,InetAddress.getByName(tar.getIP()),tar.getPort());
                        clntSock.send(se);
                    }else{
                        //destination is offline
                    
                    }


                }

            }
			if(clntOnline){
				byte buf[] =new byte[2048];
				DatagramPacket re = new DatagramPacket(buf,buf.length);
				try{
					clntSock.receive(re);
                	String received = new String(re.getData(),0,re.getLength());
                	String[] s=received.split(enc);
					System.out.println(received);

				}catch(SocketTimeoutException e){
                 //           count++;
                }
			}
			
			
			//System.out.println(">>>");
			
			//ByteBuffer buf = ByteBuffer.allocate(2048);		
			//byte[] buf = new byte[2048];
			//DatagramPacket servMes = new DatagramPacket(buf,buf.length);
			//clntChannel.read(buf);

			//String received = new String(buf.array());
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
				servPort = Integer.parseInt(args[1]);
				servMode = true;
			}catch (NumberFormatException e) {
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
				serverLoop(servPort);
/*			try{
				serverLoop(servPort);

			}catch(Exception e){
				System.out.println("Exception in server");
			}
*/
		}else{

			clientLoop(nickname,servIP,servPort,clntPort);
		}

	}
}

