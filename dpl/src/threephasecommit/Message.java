package threephasecommit;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.StringBuilder;
import java.util.Properties;
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
        VOTE_REQ,
        NO,
        ABORT,
        YES,
        PRE_COMMIT,
        ACK,
        COMMIT,
        
        STATE_REQ,
        
        UR_ELECTED,

    }
    
    //public Logger logger;
    private messageType msgType;
    private String src;
    private String dst;
    private String command;
    private String parameter1;
    private String parameter2;
    private String parameter3;
    private String parameter4;
    
    public Message() {
        System.out.println("I create a message without parameter");
    }
    
    //public Message()
   
    public Message(messageType msgType, String src, String dst, String command) {
        this.msgType = msgType;
        this.src = src;
        this.dst = dst;
        this.command = command;
    }
    
    public String msgToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("msgType =" +this.msgType);
        sb.append("\nsrc ="+this.src);
        sb.append("\ndst ="+this.dst);
        sb.append("\ncommand ="+this.command);
        return sb.toString();
    }
    
    public void extractMessage(String str) {
        try {
        System.out.println("flag 1");
        InputStream stream = new ByteArrayInputStream(str.getBytes("UTF-8"));
        System.out.println("flag2");
        Properties prop =  new Properties();
        prop.load(stream);
        this.msgType = messageType.valueOf(prop.getProperty("msgType"));
        this.src = prop.getProperty("src");
        this.dst = prop.getProperty("dst");
        this.command = prop.getProperty("command");
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
    
    public void printMessage() {
        System.out.println("msgType:"+msgType+";src:"+src+";dst:"+dst+";command:"+command);
    }
    
}
