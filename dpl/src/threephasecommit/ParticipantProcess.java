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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    
    // this is used for the termination protocol
    enum State {
        ABORTED, COMMITTED, COMMITTABLE, UNCERTAIN
    };
    private State state;
    

    

    public ParticipantProcess(String filename) {
        try {
            cmdlog = Logger.getLogger("3pc");   //just for test
            config = new Config(filename);
            //have to add it after Config, and inside try module??
            this.procNum = Integer.toString(config.procNum);
            nc = new NetController(config);

            upList = new TreeSet<String>();
            // How to generate upList??
            for (int i = 0; i < config.numProcesses; i++) {
                if (i != Integer.parseInt(procNum)) {
                    upList.add(Integer.toString(i));
                }
                //upList.add(Integer.toString(1));
            }
            //need to be changed....
            // we need to consider following cases: 
            // 1. failure but timeout is known to this participant
            // this can be easily implemented by removing the failed one in the UP List
            // 2. failure but timeout is not known to this participant
            // this can be implemented by removing all the participants before the new coordinator 
            
            currentCoordinatorProcnum = "0";
            command = "add"; // do not know why
            vote = true;
            playList = new PlayList();
            String logFile = this.config.logfile;
            this.logger = new Log(logFile, true);
            this.state=State.ABORTED;
        } catch (IOException e) {
            System.out.println(e);
        }

    }

    public String getCurrentCoordinatorProcnum() {
        return this.currentCoordinatorProcnum;
    }

    public void setCurrentCoordinatorProcnum (String newCoordinatorProcnum) {
        this.currentCoordinatorProcnum = newCoordinatorProcnum;
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

    public void CoordinatorTerminationProtocol() {
    }

    public void ParticipantTerminationProtocol() {
    }

    public void removeCoordinatorFromUpList() {
    }

    public void ElectionProtocol() {
    }
    
    public void Managerprocess() {
        Message message = new Message();
        String cmd = null;
        while (true) {
            try {
                System.out.println("input:");
                BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
                cmd = bufferRead.readLine();

                System.out.println(cmd);
            } catch (IOException e) {
                e.printStackTrace();
            } 
            this.broadcastMessage(messageType.INITIAL, cmd);
        } 
    }

    public void ParticipantCommitProtocol() {
        Message message = new Message();

        //     try {
        //   while (true) {
        //recoverisure...

        //generate error here...fail before init...

        //we wait for the 
        long startTime = System.currentTimeMillis();


        // find the INITIAL singal and then you can go ahead
        // or you will stay here to wait forever
        // all the previous messages before an INITIAL signal will be omitted
        // the INITIAL signal is sent from the first coordinator
        initial_state:
        while (true) {
            cmdlog.info("wait for initial signal");
            boolean recv_initial_flag = false;
            while (!recv_initial_flag) {
                for (String msg : nc.getReceivedMsgs()) {
                    cmdlog.info("get some message");
                    message.extractMessage(msg);
                    message.printMessage();
                    messageType msgType = message.getMsgType();
                    if (msgType == messageType.INITIAL) {
                        recv_initial_flag = true;
                        break;
                    }
                }
            }

            cmdlog.info("recv initial message");

            // Now we pass the INITIAL state 
            // and we are heading to wait the VOTE_REQ singal
            boolean recv_votereq_flag = false;               //what if receive two coordinator...one is just recovered??....need to valify the coordinator num consistant?
            startTime = System.currentTimeMillis();
            while (!recv_votereq_flag) {
                // if TIMEOUT when waiting for the vot_req
                // then log abort and contiue initial_state
                long endTime = System.currentTimeMillis();
                if (endTime - startTime >= TIMEOUT) {
                    //decision = false;
                    //to do, handle with timeout failure?
                    //break;
                    cmdlog.info("time exceed...when waiting for vote_req");
                    logger.log(ABORT);
                    this.abort(command);
                    // we do not need to remove the coordinator from the uplist
                    // because we can make the decision to abort now
                    // and uplist is only used in the termination protocol
                    // i.e. when the participant cannot make the decision unilaterally 
                    // also because uplist is known from the vote_req message 
                    // which is sent from the coordinator
                    continue initial_state;
                }

                // try to receive message
                // if null in the incoming channel, skip the for loop and go back to the while loop
                // else, get the msg, and do the corresponding things
                for (String msg : nc.getReceivedMsgs()) {
                    message.extractMessage(msg);
                    message.printMessage();
                    messageType msgType = message.getMsgType();
                    if (msgType == messageType.VOTE_REQ) {
                        // Do we need to do more?
                        // Do we need to handle with the situation when there are more than VOTE_REQ in the buffer?
                        recv_votereq_flag = true;
                        // cmdlog.info("time exceed...when waiting for vote_req");
                        break;
                    } else if (msgType == messageType.UR_ELECTED) {
                        this.removeCoordinatorFromUpList();
                        // removeCoordinatorFromUplist function needs to be written
                        // it should realize: remove all the previous died coordinators
                        this.setCurrentCoordinatorProcnum(this.procNum);
                        this.ParticipantTerminationProtocol();
                        continue initial_state;
                    } else if (msgType == messageType.INITIAL) {
                        // send the manager a msg: you cannot operate at this time
                        // because the processes are doing somthing now!
                    }
                }
            }

            // Now we pass the VOTE_REQ state
            // go ahead to the vote process

            // If voting YES
            if (this.castVote(this.command) == true) {
                logger.log(YES);
                this.state = State.UNCERTAIN;

                sendMessage(this.getCurrentCoordinatorProcnum(), messageType.YES, command);
                cmdlog.info("after sending YES");

                // try to receive the PRE_COMMIT signal
                startTime = System.currentTimeMillis();
                boolean recv_precommit_flag = false;
                while (!recv_precommit_flag) {
                    //Hope this will work..........
                    long endTime = System.currentTimeMillis();
                    if (endTime - startTime >= TIMEOUT) {
                        cmdlog.info("timeout, when waiting for pre_commit");
                        //remove Coordinator from UpList
                        this.removeCoordinatorFromUpList();
                        //initiate election protocol
                        //if (currentCoordinator == this) CoordinatorTerminationProtocal
                        //else ParticipantTerminationProtocol...
                        continue initial_state;
                    }

                    // msg: PRE_COMMIT or ABORT
                    for (String msg : nc.getReceivedMsgs()) {
                        message.extractMessage(msg);
                        message.printMessage();
                        messageType msgType = message.getMsgType();
                        if (msgType == messageType.PRE_COMMIT) {
                            //generate error here...before ack
                            recv_precommit_flag = true;
                            // logger.log(PRE_COMMIT);
                            // you do not need to log PRE_COMMIT here

                            // send ack to coordinator
                            this.sendMessage(this.getCurrentCoordinatorProcnum(), messageType.ACK, command);
                            cmdlog.info("after sending ack");
                            this.state = State.COMMITTABLE;

                            // msg1: COMMIT or ABORT
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
                                        this.state = State.COMMITTED;
                                        this.commit(command);
                                    } else if (msgType == messageType.UR_ELECTED) {
                                        this.removeCoordinatorFromUpList();
                                        // removeCoordinatorFromUplist function needs to be written
                                        // it should realize: remove all the previous died coordinators
                                        this.setCurrentCoordinatorProcnum(this.procNum);
                                        this.ParticipantTerminationProtocol();
                                        continue initial_state;
                                    }
                                }
                            }
                        } else if (msgType == messageType.ABORT) {
                            cmdlog.info("receive abort when waiting for pre_commit");
                            logger.log(ABORT);
                            this.state = State.ABORTED;
                            this.abort(command);
                        } else if (msgType == messageType.UR_ELECTED) {
                            this.removeCoordinatorFromUpList();
                            // removeCoordinatorFromUplist function needs to be written
                            // it should realize: remove all the previous died coordinators
                            this.setCurrentCoordinatorProcnum(this.procNum);
                            this.ParticipantTerminationProtocol();
                            continue initial_state;
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
        }
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
        
        
        // wait the initial signal from the manager 
        initial_state: 
        while (true) {
            cmdlog.info("wait for initial signal");
            boolean recv_initial_flag = false;
            while (!recv_initial_flag) {
                for (String msg : nc.getReceivedMsgs()) {
                    cmdlog.info("get some message");
                    message.extractMessage(msg);
                    message.printMessage();
                    messageType msgType = message.getMsgType();
                    if (msgType == messageType.INITIAL) {
                        recv_initial_flag = true;
                        break;
                    }
                }
            }
            cmdlog.info("recv initial message");
          
            
            // once received the initial signal 
            // the coordinator can begin 3PC 
            logger.log(START);
            this.broadcastMessage(Message.messageType.VOTE_REQ, command);
            
            cmdlog.info("after broadcast VOTE_REQ");
            
            decision = true;
            yesVoteList = new HashSet<String>();

            int upListSize = this.getUpList().size();
            //int voteCount = upListSize - 1;
            int voteCount = upListSize;

            System.out.println("voteCount:" + voteCount);
            cmdlog.info("before receive the vote");

            // the coordinator is waiting for all the votes
            // and the time recoding starts here 

            // the coordinator is collecting the votes
            long startTime = System.currentTimeMillis();
            while (voteCount > 0) {
                long endTime = System.currentTimeMillis();
                if (endTime - startTime >= TIMEOUT) {
                    cmdlog.info("time exceed...when waiting for vote");
                    decision = false;
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

            // Now the coordinator has finished the collection
            // it begins to process the votes based on his own vote
            // decision may be made before, or not

            // Decision is YES
            if (decision && this.castVote(command) == true) {
                //send precommit
                //add some manual error here....
                // coordinator needs to log PRE_COMMIT
                logger.log(PRE_COMMIT);
                this.broadcastMessage(Message.messageType.PRE_COMMIT, command);//I hope to add some errors when sending to nth client...
                cmdlog.info("after broadcast PRE_COMMIT");
                //add some manul error here.
                yesVoteList = new HashSet<String>();
                upListSize = this.getUpList().size();//may be different from handling with previous size???...Someone said "I am fail..., or someone didn't respond first(but it will continue to here)...
                //voteCount = upListSize -1;
                voteCount = upListSize;

                // coordinator begins to collect the ACKs
                // two cases may happen
                // 1. all the ACKs are collected in time before timeout
                // 1. then the while will be cut
                // 2. timeout before all the ACKs are collected
                // 2. then it will break from the while loop
                startTime = System.currentTimeMillis();
                while (voteCount > 0) {
                    long endTime = System.currentTimeMillis();
                    if (endTime - startTime >= TIMEOUT) {
                        cmdlog.info("time exceed...when waiting for ACK");
                        // although timeout, the coordinator will do nothing
                        // and it will still commit and send commit to all the participants
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
                this.broadcastMessage(yesVoteList, messageType.ABORT, command);

                //generate error here..
            }

        }

    }
    
}
