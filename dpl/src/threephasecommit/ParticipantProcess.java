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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Set;
import java.util.TreeSet;
import threephasecommit.Log;
import threephasecommit.Message;
import threephasecommit.Message.messageType;
import java.util.logging.Logger;

public class ParticipantProcess {

    private Config config;
    private NetController nc;
    private Log logger;
    private Logger cmdlog;
    private String procNum;
    private String currentCoordinatorProcnum;      //I don't know whether it is OK? like a point?
    //private Map<String, ParticipantProcess> procnumToParticipantProcess;
    public PlayList playList;
    //private SortedSet<ParticipantProcess> upList;              //Do we need recoverList??
    private SortedSet<String> upList;
    private String command;
    private Boolean vote;
    //lookup table? ProcNum->Port???
    public static int TIMEOUT = 5000; //Is this too short??
    public static final String START = "start-3PC";
    public static final String PRE_COMMIT = "PRE_COMMIT";
    public static final String COMMIT = "COMMIT";
    public static final String ABORT = "ABORT";
    public static final String YES = "YES";
    public static final String NO = "NO";

    public ParticipantProcess(String filename) {
        try {
            cmdlog = Logger.getLogger("3pc");
            config = new Config(filename);
            //have to add it after Config, and inside try module??
            this.procNum = Integer.toString(config.procNum);
            nc = new NetController(config);

            upList = new TreeSet<String>();
            //How to generate upList??
            for (int i = 0; i < config.numProcesses; i++) {
                if (i != Integer.parseInt(procNum)) {
                    upList.add(Integer.toString(i));
                }
            }
            //need to be changed....
            currentCoordinatorProcnum = "0";
            command = "add";
            vote = true;
            playList = new PlayList();
            String logFile = this.config.logfile;
            this.logger = new Log(logFile, true);
        } catch (IOException e) {
            System.out.println(e);
        }

    }

    public String getCurrentCoordinatorProcnum() {
        return this.currentCoordinatorProcnum;
    }

    public String getProcNum() {
        return this.procNum;
    }

    public SortedSet<String> getUpList() {
        // when set UpList, we should alwasy keep away the "this" process(oneself).
        return this.upList;
    }

    public void broadcastMessage(messageType msgType, String command) {
        this.broadcastMessage(this.getUpList(), msgType, command);
    }

    public void broadcastMessage(Set<String> recipients, messageType msgType, String command) {
        for (String p : recipients) {
            Message msg = new Message(msgType, this.procNum, p, command);
            msg.printMessage();
            nc.sendMsg(Integer.parseInt(p), msg.msgToString());
        }
    }

    public void sendMessage(String procNum, messageType msgType, String command) {
        Message msg = new Message(msgType, this.procNum, procNum, command);
        msg.printMessage();
        nc.sendMsg(Integer.parseInt(procNum), msg.msgToString());
    }

    //I hope castVote will return something depending on the configuration, as well as whether it is OK to add, delete for its playlist itself.
    public boolean castVote(String command) {
        return this.vote;
    }

    public void commit(String command) {
        //just add song, url to the playList;
        if (command == "add") {
            playList.add("hello", "www.google.com");
        } else if (command == "delete") {
            playList.delete("hello");
        }
        return;
    }

    public void abort(String command) {
    }

    //send VOTE_REQ
    public void CoordinatorCommitProtocol() {
        Message message = new Message();
        //Set <ParticipantProcess> yesVoteList;
        Set<String> yesVoteList;
        //Set<
        boolean decision;

        //command = null....later is changed by someone else???

        //boolean recoverList()?????


        // try {
        //Do we need to add while(true) so as to be a loop?
        //while(true) {

        //msg.

        //messageType msgType = msg.
        logger.log(START);
        this.broadcastMessage(Message.messageType.VOTE_REQ, command);

        cmdlog.info("after broadcast VOTE_REQ");
        //possible error

        decision = true;
        yesVoteList = new HashSet<String>();

        int upListSize = this.getUpList().size();
        //int voteCount = upListSize - 1;
        int voteCount = upListSize;

        System.out.println("voteCount:" + voteCount);
        cmdlog.info("before receive the vote");

        long startTime = System.currentTimeMillis();
        //Hope this will work..........
        while (voteCount > 0) {
            long endTime = System.currentTimeMillis();
            if (endTime - startTime >= TIMEOUT) {
                cmdlog.info("time exceed...when waiting for vote");
                decision = false;
                //to do, handle with timeout failure?
                break;
            }

            for (String msg : nc.getReceivedMsgs()) {
                message.extractMessage(msg);
                message.printMessage();
                messageType msgType = message.getMsgType();
                if (msgType == messageType.YES) {
                    yesVoteList.add(message.getMsgSource());
                    voteCount--;
                } else if (msgType == messageType.NO) {
                    decision = false;
                    voteCount--;
                    //Do we need to continue ???
                } //else if (msgType == messageType.)DO we need to handle the message fail

            }

        }

        cmdlog.info("after receive vote");

        //I don't know if there are some issues about direcly copy??
        //Set<String> remains = upList;
        Set<String> remains = new HashSet<String>();;
        //Do we need to add remains = new Set<String>(); here?
        remains.addAll(upList);
        remains.removeAll(yesVoteList);
        if (!remains.isEmpty()) {//Or only left the "self"??????????
            decision = false;
            //handle with decision false...send ABORT to the yesVoteList??
            //at next else part...
            cmdlog.info("remain is not empty, someone dead==Time out...");
        }

        if (decision && this.castVote(command) == true) {
            //send precommit
            //add some manual error here....
            this.broadcastMessage(Message.messageType.PRE_COMMIT, command);//I hope to add some errors when sending to nth client...
            cmdlog.info("after broadcast PRE_COMMIT");
            //add some manul error here.
            yesVoteList = new HashSet<String>();
            upListSize = this.getUpList().size();//may be different from handling with previous size???...Someone said "I am fail..., or someone didn't respond first(but it will continue to here)...
            //voteCount = upListSize -1;
            voteCount = upListSize;

            startTime = System.currentTimeMillis();
            while (voteCount > 0) {
                long endTime = System.currentTimeMillis();
                if (endTime - startTime >= TIMEOUT) {
                    cmdlog.info("timeout, waiting for ack");
                    decision = false;
                    //to do, handle with timeout failure?
                    break;
                }
                for (String msg : nc.getReceivedMsgs()) {
                    message.extractMessage(msg);
                    message.printMessage();
                    messageType msgType = message.getMsgType();
                    if (msgType == messageType.ACK) {
                        yesVoteList.add(message.getMsgSource());
                        voteCount--;
                    } //else if (msgType == messageType.)//DO WE NEED TO HANDLE THE MESSAGE FAIL OR ALIVE? 
                }


            }
            cmdlog.info("after receive ack");
            //ACK is not received for all. Just ignore....continue to send commit...

            //commit
            logger.log(COMMIT);
            this.commit(command);//execute the command....
            //System.out.println(COMMIT);

            //error here..after commit....
            this.broadcastMessage(messageType.COMMIT, command);
            // why is it supposed to only sent to yesVoteList?????
            //this.broadcastMessage(yesVoteList, messageType.COMMIT, command);
            cmdlog.info("after send commit");
            //error here....
        } else {
            cmdlog.info("enter the abort part");
            //decision : ABORT...
            decision = false;//including the condition for only coodinator vote for NO...
            //generate error here...after abort, before send ...
            logger.log(ABORT);
            this.abort(command);
            this.broadcastMessage(yesVoteList, messageType.NO, command);

            //generate error here..
        }
        //}
        //} catch (InterruptedException e) {
        //broadcastInterrupt?????fail error??
        //    return;//???            
        //}
    }

    public void ParticipantCommitProtocol() {
        Message message = new Message();


        //     try {
        //   while (true) {
        //recoverisure...

        //generate error here...fail before init...

        //we wait for the 
        long startTime = System.currentTimeMillis();
        boolean recv_votereq_flag = false;               //what if receive two coordinator...one is just recovered??....need to valify the coordinator num consistant?
        while (!recv_votereq_flag) {
            //Hope this will work..........
            long endTime = System.currentTimeMillis();
            if (endTime - startTime >= TIMEOUT) {
                //decision = false;
                //to do, handle with timeout failure?
                //break;
                cmdlog.info("time exceed...when waiting for vote_req");
                logger.log(ABORT);
                this.abort(command);
                return;
            }

            for (String msg : nc.getReceivedMsgs()) {
                message.extractMessage(msg);
                message.printMessage();
                messageType msgType = message.getMsgType();
                if (msgType == messageType.VOTE_REQ) {
                    //Do we need to do more?
                    //Do we need to handle with the situation when there are more than VOTE_REQ in the buffer?
                    recv_votereq_flag = true;
                    cmdlog.info("time exceed...when waiting for vote_req");
                    break;
                }//else if (msgType == messageType.)//DO WE NEED TO HANDLE THE MESSAGE FAIL OR ALIVE OR UR_ELECTED?
            }

        }

        if (this.castVote(this.command) == true) {
            //error fail after vote_req

            logger.log(YES);
            //state == uncertain??

            //error fail afer vote_req, before send...

            sendMessage(this.getCurrentCoordinatorProcnum(), messageType.YES, command);
            cmdlog.info("after sending YES");

            startTime = System.currentTimeMillis();
            boolean recv_precommit_flag = false;
            while (!recv_precommit_flag) {
                //Hope this will work..........
                long endTime = System.currentTimeMillis();
                if (endTime - startTime >= TIMEOUT) {
                    cmdlog.info("timeout, when waiting for pre_commit");
                    //remove Coordinator from UpList
                    //initiate election protocol
                    //if (currentCoordinator == this) CoordinatorTerminationProtocal
                    //else ParticipantTerminationProtocol...
                    return;
                }

                //Only one message, do we need to go through like this?
                for (String msg : nc.getReceivedMsgs()) {
                    message.extractMessage(msg);
                    message.printMessage();
                    messageType msgType = message.getMsgType();
                    if (msgType == messageType.PRE_COMMIT) {
                        //generate error here...before ack
                        recv_precommit_flag = true;
                        logger.log(PRE_COMMIT);
                        //send ack to coordinator
                        this.sendMessage(this.getCurrentCoordinatorProcnum(), messageType.ACK, command);
                        cmdlog.info("after sending ack");
                        //generate error:after ack
                        startTime = System.currentTimeMillis();
                        boolean recv_commit_flag = false;
                        while (!recv_commit_flag) {
                            endTime = System.currentTimeMillis();
                            if (endTime - startTime >= TIMEOUT) {
                                cmdlog.info("timeout...when waiting for commit");
                                //remove Coordinator from UpList
                                //initiate election protocol
                                //if (currentCoordinator == this) CoordinatorTerminationProtocal
                                //else ParticipantTerminationProtocol...
                                return;
                            }
                            for (String msg1 : nc.getReceivedMsgs()) {
                                message.extractMessage(msg1);
                                message.printMessage();
                                msgType = message.getMsgType();
                                if (msgType == messageType.COMMIT) {
                                    recv_commit_flag = true;
                                    cmdlog.info("receive commit");
                                    logger.log(COMMIT);
                                    this.commit(command);

                                    //gen error:after commit...
                                } //else if.......


                            }



                        }
                    }//else if (msgType == messageType.)//DO WE NEED TO HANDLE THE MESSAGE FAIL OR ALIVE OR UR_ELECTED?
                    else if (msgType == messageType.ABORT) {
                        cmdlog.info("receive abort when waiting for pre_commit");
                        logger.log(ABORT);
                        this.abort(command);
                    }
                }
            }


        } else {
            //gen error: before send...
            cmdlog.info("send no and abort");
            this.sendMessage(this.currentCoordinatorProcnum, messageType.NO, command);

            //gen erro after send..

            logger.log(ABORT);
            this.abort(command);

            //gen error: after abort??

        }


        //         }
        //      } catch(){
        //     }

    }

    public void CoordinatorTerminationProtocol() {
    }

    public void ParticipantTerminationProtocol() {
    }

    public void removeCoordinatorFromUpList() {
    }

    public void ElectionProtocol() {
    }
}
