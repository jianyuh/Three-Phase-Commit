import threephasecommit.Message;
import framework.Config;
import framework.NetController;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.*;

import java.util.*;
import threephasecommit.Message.messageType;

/*
public class test {

   public static void main(String args[]) {
      // Create a tree set
      HashSet ts = new HashSet();
      // Add elements to the tree set
      ts.add("C");
      ts.add("A");
      ts.add("B");
      ts.add("E");
      ts.add("F");
      ts.add("D");
      ts.sort();
      System.out.println(ts);
   }
}
*/


public class test2 {
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

			//Message m1 = new Message(messageType.VOTE_REQ,"0","1","add");

			try{
				Thread.sleep(10000);
			}catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		//	nc1.sendMsg(1,m1.toString());

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
                        
                        
                        
                        
                        
			//nc1.getReceivedMsgs();
			//nc1.sendMsg(1,"helordfsaffa");
			//nc1.sendMsg(1,"fhsak");
			//List <String> tmp = nc1.getReceivedMsgs();
			//System.out.println("Rec "+tmp);
			//System.out.println(nc1.getReceivedMsgs());
			//ArrayList<String> strlist = nc1.getReceivedMsgs();
			//System.out.println(strlist[0]);
			//nc1.sendMsg(0,"helloword&fsaf&fa");
			//System.out.println(strlist);
			
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
