package pack2;
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

public class GbnIO implements Runnable{
	List<Character> msg;
	List<Boolean> bools;
	public GbnIO(List<Character> msg,List<Boolean> bools){
		this.msg = msg;
		this.bools = bools;
	}

	@Override
	public void run(){
		Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        String[] inputs =input.split(" ");
        if(inputs.length!=2 || (!inputs[0].equals("send"))){
        	System.out.println("Invalid command. Usage: send <message>");
            System.exit(1);
        }
        char[] chars = inputs[1].toCharArray();
        for(int i = 0; i < chars.length; i++){
                msg.add(chars[i]);
				bools.add(false);
        }

	}

}
