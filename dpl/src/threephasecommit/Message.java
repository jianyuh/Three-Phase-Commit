package threephasecommit;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.StringBuilder;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jianyu
 */
public class Message {
    public enum messageType {
        INITIAL,
        RECOVERY,
        
        VOTE_REQ,
        NO,
        ABORT,
        YES,
        PRE_COMMIT,
        ACK,
        COMMIT,
        
        STATE_REQ,
        
        UR_ELECTED,
        
        ABORTED,
        UNCERTAIN,
        COMMITTED,
        COMMITTABLE,
    }
    
    //public Logger logger;
    private messageType msgType;
    private String src;
    private String dst;
    private String command;
    private String parameter;
    
    public Message() {
        //System.out.println("I create a message without parameter");
    }
    
    //public Message()
   
    /*
    public Message(messageType msgType, String src, String dst, String command) {
        this.msgType = msgType;
        this.src = src;
        this.dst = dst;
        this.command = command;
    }
    */
    
    public Message(messageType msgType, String src, String dst, String command, String parameter) {
        this.msgType = msgType;
        this.src = src;
        this.dst = dst;
        this.command = command;
        this.parameter = parameter;
    }
    
    public String msgToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("msgType =" +this.msgType);
        sb.append("\nsrc ="+this.src);
        sb.append("\ndst ="+this.dst);
        sb.append("\ncommand ="+this.command);
        sb.append("\nparameter ="+this.parameter);
        return sb.toString();
    }
    
    public void extractMessage(String str) {
        try {
       // System.out.println("flag 1");
        InputStream stream = new ByteArrayInputStream(str.getBytes("UTF-8"));
        //System.out.println("flag2");
        Properties prop =  new Properties();
        prop.load(stream);
        this.msgType = messageType.valueOf(prop.getProperty("msgType"));
        this.src = prop.getProperty("src");
        this.dst = prop.getProperty("dst");
        this.command = prop.getProperty("command");
        this.parameter = prop.getProperty("parameter");
        //this.
        } catch (IOException e) {
            System.out.println(e);
        }

    }
    
    public messageType getMsgType() {
        return this.msgType;
    }
    
    public String getMsgSource() {
        return this.src;
    }
    
    public String getMsgCommand() {
        return this.command;
    }
    
    public void printMessage() {
        System.out.println("msgType:"+msgType+";src:"+src+";dst:"+dst+";command:"+command+";parameter:"+parameter);
    }
    
    public String toStringUpList(SortedSet<String> UpList, String SelfProcNum) {
        String parameter =  null;
        for (String str: UpList) {
            parameter = parameter + str + "#";
        }
        parameter = parameter + SelfProcNum;
        return parameter;
    }
    
    public String toStringSong_URL(String Song, String URL, String Coordinator) {
        String parameter = Song+"#"+URL+"#"+Coordinator;
        return parameter;
    }
    
    public String toStringSong2_URL2(String Song1, String URL1, String URL2, String SetCoordinator) {
        String parameter = Song1+"#"+URL1+"#"+URL2+"#"+SetCoordinator;
        return parameter;
    }
    
    //public String toStringCoordinatorProcNum(String Coordinator) {
     //  return Coordinator;
    //}
    
    
    //may be some problem...
    public SortedSet<String> extractUplist(String parameter){
        TreeSet <String> UpList = new TreeSet <String>();
        String[] temp  = parameter.split("#");
        for (String s: temp) {
            UpList.add(s);
        }
        return UpList;
    }
    
    public String[] extractSong_URL_Coordinator(String parameter) {
        String[] str = parameter.split("#");
        return str;
    }
    
    public String getParameter () {
        return this.parameter;
    }
}
