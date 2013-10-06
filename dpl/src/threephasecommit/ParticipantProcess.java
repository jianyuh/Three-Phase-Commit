package threephasecommit;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
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
import java.util.logging.Level;
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
    private SortedSet<String> recoverList;
    private SortedSet<String> broadcastList;
    private String command;
    // parameter may include
    // 1. song
    // 2. URL1/2
    // 3. setCoordinatorProcNum
    private String parameter;
    private Boolean vote;
    private String song;
    private String URL;
    //lookup table? ProcNum->Port???
    public static int CTIMEOUT = 5000; //Is this too short??
    public static int PTIMEOUT = 10000;
    public static final String START = "Start-3PC";
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
            recoverList = new TreeSet<String>();

            broadcastList = new TreeSet<String>();
            for (int i = 0; i < config.numProcesses; i++) {
                if (i != Integer.parseInt(procNum)) {
                    broadcastList.add(Integer.toString(i));
                }
            }
            upList = broadcastList; 
            System.out.print("Uplist: ");
            for (String s: this.upList) {
                System.out.println(s+ " ");
            }
            // How to generate upList??
            /*
            for (int i = 0; i < config.numProcesses; i++) {
            if (i != Integer.parseInt(procNum)) {
            upList.add(Integer.toString(i));
            }
            //upList.add(Integer.toString(1));
            }
             */
            //need to be changed....
            // we need to consider following cases: 
            // 1. failure but timeout is known to this participant
            // this can be easily implemented by removing the failed one in the UP List
            // 2. failure but timeout is not known to this participant
            // this can be implemented by removing all the participants before the new coordinator 


            // This is the default setup for the current coordinator
            currentCoordinatorProcnum = "0";

            //command = "add"; // do not know why
            vote = true;
            playList = new PlayList();

            System.out.println("logfile:" + this.config.logfile);
            String logFile = this.config.logfile;
            this.logger = new Log(logFile, true);

            this.state = State.ABORTED;
            cmdlog.info("A new process with ProcNum " + this.getProcNum() + " is recovered.");

            if (!this.procNum.equals(Integer.toString(this.config.numProcesses))) {
                // Broadcast the RECOVERY message to all the processes
                this.broadcastMessage(this.getBroadcastList(), messageType.RECOVERY);
                cmdlog.info("A new process with ProcNum " + this.getProcNum() + " has reported RECOVERY to all other processes.");
            }   
        } catch (IOException e) {
            System.out.println(e);
        }

    }

    // This function will be called uniformly by no matter 
    // a coordinator or participant
    // which will automatically select someone to be a coordinator
    // and implement its role
    public void ProcessStartProtocol() {
        Message message = new Message();

        upList.addAll(recoverList);
        recoverList.clear();

        cmdlog.info("The process with ProcNum " + this.getProcNum() + " is started.");
        while (true) {
            cmdlog.info("wait for initial signal or the RECOVERY message from recovered processes...");
            boolean recv_initial_flag = false;
            while (!recv_initial_flag) {
                for (String msg : nc.getReceivedMsgs()) {
                    cmdlog.info("get some message...");
                    message.extractMessage(msg);
                    message.printMessage();

                    messageType msgType = message.getMsgType();
                    if (msgType == messageType.INITIAL) {
                        cmdlog.info("The received message is an INITIAL message...");

                        recv_initial_flag = true;
                        this.command = message.getMsgCommand();//////////////////////////later we should assert(msg.command == this.command)???
                        cmdlog.info("This INITIAL message contains the command " + this.command);

                        String[] str = message.extractSong_URL_Coordinator(message.getParameter());
                        cmdlog.info("This INITIAL message contains the parameters: " + message.getParameter());
                        cmdlog.info("The following parameters are contained in this " + this.command + " command");
                        this.song = str[0];
                        this.URL = str[1];
                        cmdlog.info(str[0] + str[1]);
                        this.currentCoordinatorProcnum = str[2];
                        cmdlog.info("The manager wants to set the process with ProcNum " + str[2] + " to be the coordinator");

                        if (this.getCurrentCoordinatorProcnum().equals(this.getProcNum())) {
                            this.CoordinatorCommitProtocol();
                        } else {
                            this.ParticipantCommitProtocol();
                        }
                        break;
                    } else if (msgType == messageType.RECOVERY) {
                        cmdlog.info("The received message is a RECOVERY message....");
                        String recoverProcNum = message.getMsgSource();
                        //recoverList.add(recoverProcNum);
                        upList.add(recoverProcNum);
                        cmdlog.info("The recovered ProcessNum is " + recoverProcNum);
                        cmdlog.info("I have added it to my UpList");
                    }
                }
            }
        }
    }

    public String getCurrentCoordinatorProcnum() {
        return this.currentCoordinatorProcnum;
    }

    public void setCurrentCoordinatorProcnum(String newCoordinatorProcnum) {
        this.currentCoordinatorProcnum = newCoordinatorProcnum;
    }

    public String getProcNum() {
        return this.procNum;
    }

    public SortedSet<String> getUpList() {
        // when set UpList, we should alwasy keep away the "this" process(oneself).
        return this.upList;
    }

    public SortedSet<String> getBroadcastList() {
        return this.broadcastList;
    }

    public void removeCoordinatorFromUpList() {
        this.upList.remove(this.getCurrentCoordinatorProcnum());
    }

    public void broadcastMessage(messageType msgType) {
        this.broadcastMessage(this.getUpList(), msgType, this.command, this.parameter);
    }

    //public void broadcastMessage(messageType msgType, )
    public void broadcastMessage(messageType msgType, String command, String parameter) {
        this.broadcastMessage(this.getUpList(), msgType, command, parameter);
    }

    public void broadcastMessage(Set<String> recipients, messageType msgType, String command, String parameter) {
        for (String p : recipients) {
            Message msg = new Message(msgType, this.procNum, p, command, parameter);
            msg.printMessage();
            nc.sendMsg(Integer.parseInt(p), msg.msgToString());
        }
    }

    public void broadcastMessage(Set<String> recipients, messageType msgType) {
        for (String p : recipients) {
            Message msg = new Message(msgType, this.procNum, p, this.command, this.parameter);
            msg.printMessage();
            nc.sendMsg(Integer.parseInt(p), msg.msgToString());
        }
    }

    //override...successNum: broadcast successNum messages, and then fail
    public void broadcastMessage(Set<String> recipients, messageType msgType, String command, String parameter, int successNum) {
        int count = 0;
        for (String p : recipients) {
            count++;
            if (count >= successNum) {
                break;
            }
            Message msg = new Message(msgType, this.procNum, p, command, parameter);
            msg.printMessage();
            nc.sendMsg(Integer.parseInt(p), msg.msgToString());
        }
    }

    //override...ProcNum: broadcast message to ProcNum, and then fail....
    public void broadcastMessage(Set<String> recipients, messageType msgType, String command, String parameter, String ProcNum) {
        for (String p : recipients) {
            if (p != ProcNum) {
                continue;
            }
            Message msg = new Message(msgType, this.procNum, p, command, parameter);
            msg.printMessage();
            nc.sendMsg(Integer.parseInt(p), msg.msgToString());
        }
    }

    /*
     * if (flag_partial_commit_n == true)
     *     broadcast(......successNum); or broadcast(........ProcNum);
     * else
     *     broadcast(........normal parameter).....
     * 
     */
    public void sendMessage(String procNum, messageType msgType, String command, String parameter) {
        Message msg = new Message(msgType, this.procNum, procNum, command, parameter);
        msg.printMessage();
        nc.sendMsg(Integer.parseInt(procNum), msg.msgToString());
    }

    public void sendMessage(String procNum, messageType msgType) {
        Message msg = new Message(msgType, this.procNum, procNum, this.command, this.parameter);
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
            playList.add(song, URL);
        } else if (command == "delete") {
            playList.delete(song, URL);
        } else if (command == "edit") {
            playList.edit(song, URL);//actually, we can use only URL2....
        }
        return;
    }

    public void abort(String command) {
    }

    public void CoordinatorTerminationProtocol() {
        cmdlog.info("Procnum " + this.getCurrentCoordinatorProcnum() + " is elected as a new coordinator");
        // the new coordinator should update the uplist 
        // to include all the survived participants
        // we will write this part later

        // now let me just end messages to all the processes
        // this is only used for testing

        // first broadcast the state_req signal
        // this.upList.
        System.out.print("Uplist: ");
        for (String s: this.upList) {
            System.out.println(s + " ");
        }
        this.broadcastMessage(messageType.STATE_REQ, this.command, this.parameter);

        int upListSize = this.getUpList().size();
        //int voteCount = upListSize - 1;
        int state_report_count = upListSize;
        Message message = new Message();
        boolean ExistAborted = false;
        boolean ExistCommitted = false;
        boolean ExistCommittable = false;
        SortedSet<String> UncertainParticipantList = new TreeSet<String>();

        // the new coordinator is waiting for the state_report from the survived participants
        // it collects the received reports and ignores the failed ones
        long startTime = System.currentTimeMillis();
        while (state_report_count > 0) {
            long endTime = System.currentTimeMillis();
            if (endTime - startTime >= CTIMEOUT) {
                cmdlog.info("time exceed...when waiting for state reports");
                break;
            }
            for (String msg : nc.getReceivedMsgs()) {
                message.extractMessage(msg);
                message.printMessage();
                messageType msgType = message.getMsgType();
                String srcNum = message.getMsgSource();
                if (msgType == messageType.ABORTED) {
                    //yesVoteList.add(message.getMsgSource());
                    ExistAborted = true;
                    state_report_count--;
                } else if (msgType == messageType.COMMITTED) {
                    ExistCommitted = true;
                    state_report_count--;
                } else if (msgType == messageType.UNCERTAIN) {
                    UncertainParticipantList.add(srcNum);
                    state_report_count--;
                } else if (msgType == messageType.COMMITTABLE) {
                    ExistCommittable = true;
                    state_report_count--;
                } else if (msgType == messageType.RECOVERY) {
                    this.recoverList.add(message.getMsgSource());
                }
            }
        }

        // Now, the new coordinator will make the decision 
        // according to the collected information from the participants
        // there are four cases: TR1 - TR4
        // TR1: if ExistAbort == true, then decision is abort
        // TR2: else if ExistCommit == true,  then decision is commit
        // TR3: else if ExistCommittable == false, then decision is abort
        // TR4: else decision is pre_commit, and the coordinator will do a lot of things
        // TR 1
        if (this.state == State.ABORTED || ExistAborted) {
            // the coordinator should first check whether there has already been an abort
            // record in its log
            // if not, it will log abort
            cmdlog.info("TR 1 entered.");
            logger.log(ABORT, command, song, URL);
            this.broadcastMessage(messageType.ABORT);
        } // TR 2
        else if (this.state == State.COMMITTED || ExistCommitted) {
            // the coordinator should first check whether there has already been a commit
            // record in its log
            // if not, it will log commit
            cmdlog.info("TR 2 entered.");
            logger.log(COMMIT, command, song, URL);
            this.broadcastMessage(messageType.COMMIT);
        } // TR 3
        else if (this.state == State.UNCERTAIN && !ExistCommittable) {
            cmdlog.info("TR 3 entered.");
            this.state = State.ABORTED;
            logger.log(ABORT, command, song, URL);
            this.broadcastMessage(messageType.ABORT);
        } // TR 4
        else {
            this.broadcastMessage(UncertainParticipantList, messageType.PRE_COMMIT);

            cmdlog.info("TR 4 entered.");
            int ACKcount = UncertainParticipantList.size();
            startTime = System.currentTimeMillis();
            while (ACKcount > 0) {
                long endTime = System.currentTimeMillis();
                if (endTime - startTime >= CTIMEOUT) {
                    cmdlog.info("time exceed...when waiting for ACK in the termination protocol");
                    // although timeout, the coordinator will do nothing
                    // and it will still commit and send commit to all the participants
                    break;
                }
                for (String msg : nc.getReceivedMsgs()) {
                    message.extractMessage(msg);
                    message.printMessage();
                    messageType msgType = message.getMsgType();
                    if (msgType == messageType.ACK) {
                        ACKcount--;
                    } else if (msgType == messageType.RECOVERY) {
                        this.recoverList.add(message.getMsgSource());
                    }
                }
            }
            logger.log(COMMIT, command, song, URL);
            this.broadcastMessage(messageType.COMMIT);
        }
    }

    public void Managerprocess() {
        Message message = new Message();
        String line = null;
        while (true) {
            try {
                System.out.println("input:");
                BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
                line = bufferRead.readLine();
                System.out.println(line);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String[] str = line.split(" ");
            this.command = str[0];
            this.song = str[1];
            this.URL = str[2];
            this.currentCoordinatorProcnum = str[3];
            this.parameter = this.song + "#" + this.URL + "#" + this.currentCoordinatorProcnum;


            this.broadcastMessage(this.broadcastList, messageType.INITIAL);

        }

    }

    // election protocol
    // 1. select the new coordinator from the uplist
    // 2. notify the new coordinator if it is not myself
    public void ElectionProtocol() {
        if (this.upList.isEmpty()) {
            this.setCurrentCoordinatorProcnum(this.procNum);
            return;
        }
        String newCoordinator = this.upList.first();
        if (Integer.parseInt(this.procNum) < Integer.parseInt(newCoordinator)) {
            this.setCurrentCoordinatorProcnum(this.procNum);
            return;
        } else {
            this.setCurrentCoordinatorProcnum(newCoordinator);
        }

        cmdlog.info(newCoordinator);

        this.sendMessage(this.getCurrentCoordinatorProcnum(), messageType.UR_ELECTED);
    }

    public void ParticipantTerminationProtocol() {
        boolean recv_statereq_flag = false;
        Message message = new Message();
        messageType msgType = null;
        messageType stateType = null;


        long startTime = System.currentTimeMillis();

        // wait the state_req signal
        while (!recv_statereq_flag) {
            long endTime = System.currentTimeMillis();
            if (endTime - startTime >= PTIMEOUT) {
                this.removeCoordinatorFromUpList();
                this.ElectionProtocol();
                if (this.getCurrentCoordinatorProcnum() == this.procNum) {
                    this.CoordinatorTerminationProtocol();
                } else {
                    this.ParticipantTerminationProtocol();
                }
                return;
            }
            for (String msg : nc.getReceivedMsgs()) {
                message.extractMessage(msg);
                message.printMessage();
                msgType = message.getMsgType();
                if (msgType == messageType.STATE_REQ) {
                    recv_statereq_flag = true;
                    if (this.state == State.ABORTED) {
                        stateType = messageType.ABORTED;
                    } else if (this.state == State.UNCERTAIN) {
                        stateType = messageType.UNCERTAIN;
                    } else if (this.state == State.COMMITTABLE) {
                        stateType = messageType.COMMITTABLE;
                    } else if (this.state == State.COMMITTED) {
                        stateType = messageType.COMMITTED;
                    }
                    assert (this.getCurrentCoordinatorProcnum().equals(message.getMsgSource()));
                    this.sendMessage(this.getCurrentCoordinatorProcnum(), stateType);
                    cmdlog.info("fuckkkk");
                    // I guess this sendMessage function might be wrong 
                    // because different participant may not agree on the same coordinator
                    break;
                } else if (msgType == messageType.UR_ELECTED) {
                    this.removeCoordinatorFromUpList();
                    // removeCoordinatorFromUplist function needs to be written
                    // it should realize: remove all the previous died coordinators
                    this.setCurrentCoordinatorProcnum(this.procNum);
                    this.CoordinatorTerminationProtocol();
                    return;
                } else if (msgType == messageType.RECOVERY) {
                    this.recoverList.add(message.getMsgSource());
                }
            }
        }

        cmdlog.info("ilovewangqi");
        // wait the response from the coordinator
        boolean recv_response = false;
        startTime = System.currentTimeMillis();
        while (!recv_response) {
            cmdlog.info("ilovewangqi1");
            long endTime = System.currentTimeMillis();
            if (endTime - startTime >= PTIMEOUT) {
                this.removeCoordinatorFromUpList();
                this.ElectionProtocol();
                cmdlog.info("ilovewangqi2s");
                cmdlog.info(this.getCurrentCoordinatorProcnum());

                if (this.getCurrentCoordinatorProcnum() == this.procNum) {
                    this.CoordinatorTerminationProtocol();
                } else {
                    this.ParticipantTerminationProtocol();
                }
                return;
            }
            for (String msg : nc.getReceivedMsgs()) {
                message.extractMessage(msg);
                message.printMessage();
                msgType = message.getMsgType();
                if (msgType == messageType.ABORT) {
                    // you need to first check whether the log contains the ABORT record 
                    // for the current command
                    logger.log(ABORT, command, song, URL);
                    return;
                } else if (msgType == messageType.COMMIT) {
                    // you need to first check whether the log contains the C record 
                    // for the current command
                    logger.log(COMMIT,command, song, URL);
                    return;
                } else if (msgType == messageType.PRE_COMMIT) {
                    this.sendMessage(this.getCurrentCoordinatorProcnum(), messageType.ACK);
                    recv_response = true;
                    break;
                } else if (msgType == messageType.RECOVERY) {
                    this.recoverList.add(message.getMsgSource());
                }
            }
        }
        cmdlog.info("ilovewangqi3");

        // wait the final decision (commit/abort) from the coordinator
        recv_response = false;
        startTime = System.currentTimeMillis();
        while (!recv_response) {
            long endTime = System.currentTimeMillis();
            if (endTime - startTime >= PTIMEOUT) {
                this.removeCoordinatorFromUpList();
                this.ElectionProtocol();
                if (this.getCurrentCoordinatorProcnum() == this.procNum) {
                    this.CoordinatorTerminationProtocol();
                } else {
                    this.ParticipantTerminationProtocol();
                }
                return;
            }
            for (String msg : nc.getReceivedMsgs()) {
                message.extractMessage(msg);
                message.printMessage();
                msgType = message.getMsgType();
                if (msgType == messageType.COMMIT) {
                    logger.log(COMMIT, command, song, URL);
                    return;
                } else if (msgType == messageType.UR_ELECTED) {
                    this.removeCoordinatorFromUpList();
                    // removeCoordinatorFromUplist function needs to be written
                    // it should realize: remove all the previous died coordinators
                    this.setCurrentCoordinatorProcnum(this.procNum);
                    this.CoordinatorTerminationProtocol();
                    return;
                } else if (msgType == messageType.RECOVERY) {
                    this.recoverList.add(message.getMsgSource());
                }
            }
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
        //initial_state:
        //while (true) {
        /*
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
        this.command = message.getMsgCommand();//////////////////////////later we should assert(msg.command == this.command)???
        break;
        }
        }
        }
         * 
         */

        // cmdlog.info("recv initial message");

        // Now we pass the INITIAL state 
        // and we are heading to wait the VOTE_REQ singal
        boolean recv_votereq_flag = false;               //what if receive two coordinator...one is just recovered??....need to valify the coordinator num consistant?
        startTime = System.currentTimeMillis();
        while (!recv_votereq_flag) {
            // if TIMEOUT when waiting for the vot_req
            // then log abort and contiue initial_state
            long endTime = System.currentTimeMillis();
            if (endTime - startTime >= PTIMEOUT) {
                //decision = false;
                //to do, handle with timeout failure?
                //break;
                cmdlog.info("time exceed...when waiting for vote_req");
                logger.log(ABORT, command, song, URL);
                this.abort(command);
                // we do not need to remove the coordinator from the uplist
                // because we can make the decision to abort now
                // and uplist is only used in the termination protocol
                // i.e. when the participant cannot make the decision unilaterally 
                // also because uplist is known from the vote_req message 
                // which is sent from the coordinator
                return;
                //continue initial_state;
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
                    // this.upList.addAll(message.extractUplist(message.getParameter()));
                    this.upList = message.extractUplist(message.getParameter());
                    upList.remove(this.getProcNum());

                    recv_votereq_flag = true;
                    // cmdlog.info("time exceed...when waiting for vote_req");
                    break;
                } else if (msgType == messageType.UR_ELECTED) {
                    this.removeCoordinatorFromUpList();
                    // removeCoordinatorFromUplist function needs to be written
                    // it should realize: remove all the previous died coordinators
                    this.setCurrentCoordinatorProcnum(this.procNum);
                    this.ParticipantTerminationProtocol();
                    return;
                    //continue initial_state;
                } else if (msgType == messageType.INITIAL) {
                    // send the manager a msg: you cannot operate at this time
                    // because the processes are doing somthing now!
                } else if (msgType == messageType.RECOVERY) {
                    this.recoverList.add(message.getMsgSource());
                }
            }
        }

        // Now we pass the VOTE_REQ state
        // go ahead to the vote process

        // If voting YES
        if (this.castVote(this.command) == true) {
            logger.log(YES, command, song, URL);
            this.state = State.UNCERTAIN;

            sendMessage(this.getCurrentCoordinatorProcnum(), messageType.YES);
            cmdlog.info("after sending YES");

            // try to receive the PRE_COMMIT signal
            startTime = System.currentTimeMillis();
            boolean recv_precommit_flag = false;
            while (!recv_precommit_flag) {
                //Hope this will work..........
                long endTime = System.currentTimeMillis();
                if (endTime - startTime >= PTIMEOUT) {
                    cmdlog.info("timeout, when waiting for pre_commit");
                    //remove Coordinator from UpList
                    // this is used for update
                    System.out.print("Uplist: ");
                    for (String s: this.upList) {
                        System.out.println(s + " ");
                    }
                    this.removeCoordinatorFromUpList();
                    // election protocol does the following things: 
                    // it finds the participants existing in this participant's uplist
                    // and then choose it as the new coordinator
                    cmdlog.info("process " + this.getCurrentCoordinatorProcnum() + " is and old coordinator");
                    this.ElectionProtocol();
                    cmdlog.info("process " + this.getCurrentCoordinatorProcnum() + " is elected as a new coordinator");
                    if (this.getCurrentCoordinatorProcnum() == this.procNum) {
                        this.CoordinatorTerminationProtocol();
                    } else {
                        this.ParticipantTerminationProtocol();
                    }
                    return;
                    //continue initial_state;
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
                        this.sendMessage(this.getCurrentCoordinatorProcnum(), messageType.ACK);
                        cmdlog.info("after sending ack");
                        this.state = State.COMMITTABLE;

                        // msg1: COMMIT or ABORT
                        startTime = System.currentTimeMillis();
                        boolean recv_commit_flag = false;
                        while (!recv_commit_flag) {
                            endTime = System.currentTimeMillis();
                            if (endTime - startTime >= PTIMEOUT) {
                                cmdlog.info("timeout...when waiting for commit");
                                //remove Coordinator from UpList
                                // this is used for update
                                this.removeCoordinatorFromUpList();
                                // election protocol does the following things: 
                                // it finds the participants existing in this participant's uplist
                                // and then choose it as the new coordinator
                                this.ElectionProtocol();
                                if (this.getCurrentCoordinatorProcnum() == this.procNum) {
                                    this.CoordinatorTerminationProtocol();
                                } else {
                                    this.ParticipantTerminationProtocol();
                                }
                                return;
                                //continue initial_state;
                            }
                            for (String msg1 : nc.getReceivedMsgs()) {
                                message.extractMessage(msg1);
                                message.printMessage();
                                msgType = message.getMsgType();
                                if (msgType == messageType.COMMIT) {
                                    recv_commit_flag = true;
                                    cmdlog.info("receive commit");
                                    logger.log(COMMIT, command, song, URL);
                                    this.state = State.COMMITTED;
                                    this.commit(command);
                                    return;
                                    //continue initial_state;
                                } else if (msgType == messageType.UR_ELECTED) {
                                    this.removeCoordinatorFromUpList();
                                    // removeCoordinatorFromUplist function needs to be written
                                    // it should realize: remove all the previous died coordinators
                                    this.setCurrentCoordinatorProcnum(this.procNum);
                                    this.CoordinatorTerminationProtocol();
                                    return;
                                    //continue initial_state;
                                } else if (msgType == messageType.RECOVERY) {
                                    this.recoverList.add(message.getMsgSource());
                                }
                            }
                        }
                    } else if (msgType == messageType.ABORT) {
                        cmdlog.info("receive abort when waiting for pre_commit");
                        logger.log(ABORT, command, song, URL);
                        this.state = State.ABORTED;
                        this.abort(command);
                        return;
                        //continue initial_state;
                    } else if (msgType == messageType.UR_ELECTED) {
                        this.removeCoordinatorFromUpList();
                        // removeCoordinatorFromUplist function needs to be written
                        // it should realize: remove all the previous died coordinators
                        this.setCurrentCoordinatorProcnum(this.procNum);
                        this.CoordinatorTerminationProtocol();
                        return;
                        //continue initial_state;
                    } else if (msgType == messageType.RECOVERY) {
                        this.recoverList.add(message.getMsgSource());
                    }
                }
            }


        } else {
            //gen error: before send...
            cmdlog.info("send no and abort");
            this.sendMessage(this.currentCoordinatorProcnum, messageType.NO);

            //gen erro after send..

            logger.log(ABORT, command, song, URL);
            this.abort(command);
            return;
            //continue initial_state;

            //gen error: after abort??

        }

    }
    //}

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
        //initial_state: 
        //while (true) {
        /*
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
        this.command = message.getMsgCommand();
        break;
        }
        }
        }
        cmdlog.info("recv initial message");
         */

        // once received the initial signal 
        // the coordinator can begin 3PC 
        logger.log(START, command, song, URL);

        //System.exit(0);

        // compress the UpList into a string
        // and send it to all the participants in the UpList
        System.out.println("Uplist: ");
        for (String s: this.upList) {
            System.out.println(s + " ");
        }
        String toSendParameter = message.toStringUpList(this.upList, this.procNum);
        System.out.println(toSendParameter);
        this.broadcastMessage(messageType.VOTE_REQ, this.command, toSendParameter);
        // this.broadcastMessage(Message.messageType.VOTE_REQ);

        //if (this.getProcNum().equals("0")) {
        // System.exit(0);
        //}

        cmdlog.info("after broadcast VOTE_REQ");

        decision = true;
        yesVoteList = new HashSet<String>();

        int upListSize = this.getUpList().size();
        //int voteCount = upListSize - 1;
        int voteCount = this.config.numProcesses - 1;
        //int voteCount = upListSize;
        System.out.println("Print the UpList now: ");
        for (String s : upList) {
            System.out.println(s);
        }

        System.out.println("voteCount:" + voteCount);
        cmdlog.info("before receive the vote");

        // the coordinator is waiting for all the votes
        // and the time recoding starts here 

        // the coordinator is collecting the votes
        long startTime = System.currentTimeMillis();
        while (voteCount > 0) {
            long endTime = System.currentTimeMillis();
            if (endTime - startTime >= CTIMEOUT) {
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
                } else if (msgType == messageType.RECOVERY) {
                    this.recoverList.add(message.getMsgSource());
                }
            }
        }
        cmdlog.info("after receive vote");

        // Now the coordinator has finished the collection
        // it begins to process the votes based on his own vote
        // decision may be made before, or not

        //if (this.getProcNum().equals("0")) {
        //System.exit(0);
        //}
        
        System.out.println("Kill the coordinator with ProcNum 0");
        try {
            Thread.sleep(8000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Kill the coordinator with ProcNum 1");
        try {
            Thread.sleep(8000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        // Decision is YES
        if (decision && this.castVote(command) == true) {
            //send precommit
            //add some manual error here....
            // coordinator needs to log PRE_COMMIT
            logger.log(PRE_COMMIT, command, song, URL);
            this.broadcastMessage(Message.messageType.PRE_COMMIT);//I hope to add some errors when sending to nth client...
            //if (this.getProcNum().equals("0")) {
            //    System.exit(0);
            //}
            cmdlog.info("after broadcast PRE_COMMIT");
            //add some manul error here.
            yesVoteList = new HashSet<String>();
            upListSize = this.getUpList().size();//may be different from handling with previous size???...Someone said "I am fail..., or someone didn't respond first(but it will continue to here)...
            //voteCount = upListSize -1;
            //voteCount = upListSize;
            voteCount = this.config.numProcesses - 1;

            // coordinator begins to collect the ACKs
            // two cases may happen
            // 1. all the ACKs are collected in time before timeout
            // 1. then the while will be cut
            // 2. timeout before all the ACKs are collected
            // 2. then it will break from the while loop
            startTime = System.currentTimeMillis();
            while (voteCount > 0) {
                long endTime = System.currentTimeMillis();
                if (endTime - startTime >= CTIMEOUT) {
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
                    } else if (msgType == messageType.RECOVERY) {
                        this.recoverList.add(message.getMsgSource());
                    }
                }
            }

            cmdlog.info("after receive ack");
            //ACK is not received for all. Just ignore....continue to send commit...

            //commit
            logger.log(COMMIT,command, song, URL);
            this.commit(command);//execute the command....
            //System.out.println(COMMIT);

            //error here..after commit....
            this.broadcastMessage(messageType.COMMIT);
            // why is it supposed to only sent to yesVoteList?????
            //this.broadcastMessage(yesVoteList, messageType.COMMIT, command);
            cmdlog.info("after send commit");
            //error here....
            return;
            //continue initial_state;
        } else {
            cmdlog.info("enter the abort part");
            //decision : ABORT...
            decision = false;//including the condition for only coodinator vote for NO...
            //generate error here...after abort, before send ...
            logger.log(ABORT, command, song, URL);
            this.abort(command);
            this.broadcastMessage(yesVoteList, messageType.ABORT);

            return;
            //continue initial_state;
            //generate error here..
        }

    }
}
    
//}
