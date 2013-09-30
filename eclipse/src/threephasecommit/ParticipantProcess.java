package threephasecommit;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jianyu
 */

import framework.Config;
import framework.NetController;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.Set;
import threephasecommit.Log;
import threephasecommit.Message;
import threephasecommit.Message.messageType;

public class ParticipantProcess {
    private Config config;
    private NetController nc;
    private Log logger;
    private String procNum;
    private ParticipantProcess currentCoordinator;      //I don't know whether it is OK? like a point?
    
    private PlayList playList;
    private SortedSet<ParticipantProcess> upList;              //Do we need recoverList??
    private String command;
    
    //lookup table? ProcNum->Port???
    
    
    public static int TIMEOUT = 1000; //Is this too short??
    
    
    public ParticipantProcess() {
        try {
            config = new Config("config.txt");
            //have to add it after Config, and inside try module??
            this.procNum = Integer.toString(config.procNum);
            nc = new NetController(config);
            
            command = "add";
            
        } catch (IOException e) {
            System.out.println(e);
        }

    }
    
    public String getProcNum(){
        return this.procNum;
    }
    
    public SortedSet<ParticipantProcess> getUpList() {
        return this.upList;
    }
    
    public void broadcastMessage(messageType msgType, String command) {
        this.broadcastMessage(this.getUpList(), msgType, command);
    }
    
    public void broadcastMessage(Set<ParticipantProcess> recipients, messageType msgType, String command) {
        for (ParticipantProcess p :recipients) {
            Message msg = new Message(msgType, this.procNum, p.getProcNum(), command);
            nc.sendMsg(Integer.parseInt(p.getProcNum()), msg.msgToString());
        }
    }
    
   // public List<String> 
    
    
    //send VOTE_REQ
    
    public void CoordinatorCommitProtocol() {
        Message message = null;
        Set <ParticipantProcess> yesVoteList;
        //Set<
        boolean decision;
        
        //command = null....later is changed by someone else???
        
        //boolean recoverList()?????
        
        
        try {
            while(true) {
                
                //msg.
                
                //messageType msgType = msg.
                        
                this.broadcastMessage(Message.messageType.VOTE_REQ, command);
                
                //possible error
                
                decision = true;
                yesVoteList = new HashSet<ParticipantProcess>();
                
                int upListSize = this.getUpList().size();
                int voteCount = upListSize - 1;
                
                long startTime = System.currentTimeMillis();
                
                while (voteCount > 0) {
                    long endTime = System.currentTimeMillis();
                    if (endTime - startTime >= TIMEOUT) {
                        decision = false;
                        //to do, handle with timeout failure?
                    }
                    
                    List<String> msgList = nc.getReceivedMsgs();
                    Iterator<String> iterator = msgList.iterator();
			while (iterator.hasNext()) {
				//System.out.println("m2" + iterator.next());
                                String str = iterator.next();
				message.extractMessage(str);
				message.printMessage();     //for test
                                
                                messageType msgType = message.getMsgType();
                                if (msgType == messageType.YES) {
                                    yesVoteList.add(message.getMsgSource());
                                    
                                }
			}
                    
                }
                
                
            }
        }
        
    }
    
    
    
    
}
