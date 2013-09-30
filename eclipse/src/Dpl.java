/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import framework.Config;
import framework.NetController;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.*;

import java.util.*;
import threephasecommit.Message.messageType;
import threephasecommit.Message;


public class Dpl {
	//private final Config config;
	
	//public test(Config config){
	//	this.config = config;
	//}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//(new ThreadTest()).start();
		//test1 = new test();
		try {
			Config config1 = new Config("config.txt");
			
			//have to add it after Config, and inside try module??
			NetController nc1 = new NetController(config1);
                        
                        Message m1 = new Message(messageType.VOTE_REQ,"0","1","add");
                        
                        System.out.println(m1.msgToString());
                        nc1.sendMsg(1,m1.msgToString());
                        
                        Message m2 = new Message();
                        
                        List <String> msglist = nc1.getReceivedMsgs();
                        System.out.println("msglist "+msglist);
                        
                        Iterator<String> iterator = msglist.iterator();
                        while (iterator.hasNext()) {
                            //System.out.println("m2" + iterator.next());
                            String str = iterator.next();
                            m2.extractMessage(str);
                            m2.printMessage();
                        }
			
		}catch (IOException e) {
			System.out.println(e);
		}


		System.out.println("hello world");

	}

}


/*
public class ThreadTest extends Thread {

    public void run() {
        System.out.println("Hello from a thread!");
    }

    //public static void main(String args[]) {
    //    (new test()).start();
    //}

}
*/