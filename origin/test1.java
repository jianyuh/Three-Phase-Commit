import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.*;

import java.util.*;

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


public class test1 {
	//private final Config config;
	
	//public test(Config config){
	//	this.config = config;
	//}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//(new ThreadTest()).start();
		//test1 = new test();
		try {
			Config config1 = new Config("config1.txt");
			
			//have to add it after Config, and inside try module??
			NetController nc1 = new NetController(config1);
			nc1.getReceivedMsgs();
			nc1.sendMsg(1,"helordfsaffa");
			nc1.sendMsg(1,"fhsak");
			List <String> tmp = nc1.getReceivedMsgs();
			System.out.println("Rec "+tmp);
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
