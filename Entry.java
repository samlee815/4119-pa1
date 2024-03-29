package mypack;
/* the class and methods of client table entry */
import java.net.*;

public class Entry{
	String IP;
	int portNum;
	boolean online;

	//constructor
	public Entry(String IP,int portNum){
		//this.nickname = nickname;
		this.IP = IP;
		this.online = true;
		this.portNum = portNum;
	}

	public void update(String IP,int portNum,boolean online){
		this.IP = IP;
        this.online = online;
        this.portNum = portNum;
	}

	public void offline (){
		this.online = false;
	}

	public void online (){
        this.online = true;
    }
	
	public String getIP(){
		return this.IP;
	}

	public String format(){

		String ret = IP + "\u2561"+Integer.toString(portNum);
		if(this.online){
			ret=ret+"\u2561"+Integer.toString(1);
		}else{
			ret=ret+"\u2561"+Integer.toString(0);
		}
		return ret;

	}

	public int getPort(){
		return this.portNum;
	}	

	public boolean isonline(){
		return this.online;
	}
	
	/*public boolean equals(Object o){
		if(o==this){return true;}
		if (!(o instanceof Entry)) { 
            return false; 
        } 
		Entry e = (Entry) o;
		if(!this.nickname.equals(e.nickname)){return false;}
		if(!this.IP.equals(e.IP)){return false;}
		if(this.portNum!=e.portNum){return false;}
		if(this.online!=e.online){return false;}
		return true;
	}*/

}
