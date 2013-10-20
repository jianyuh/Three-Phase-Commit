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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import java.util.Properties;

public class ParticipantProcess {

    boolean participantfailure_case1_flag = false;
    boolean participantfailure_case2_flag = false;
    boolean participantfailure_case3_flag = false;
    boolean coordinatorfailure_case1_flag = false;
    boolean coordinatorfailure_case2_flag = false;
    boolean coordinatorfailure_case3_flag = false;
    boolean coordinatorfailure_case4_flag = false;
    boolean coordinatorfailure_case5_flag = false;
    boolean coordinatorfailure_case6_flag = false;
    
    boolean new_flag6 = false;
    boolean new_flag7 = false;
    boolean new_flag8 = false;
    
    boolean new_flag9 = false;
    boolean new_flag10 = false;
    boolean new_flag11 = false;

    boolean totalfailure_case1_flag = false;
    boolean totalfailure_case2_flag = false;
    boolean totalfailure_case3_flag = false;
    
    boolean futurecoordinatorfailure_case1_flag = false;
    //boolean futurecoordinatorfailure_case2_flag = false;
    boolean cascadingcoordinatorfailure_case1_flag = false;
    //boolean cascadingcoordinatorfailure_case2_flag = false;
    
    
    private Config config;
    private NetController nc;
    private Log logger;
    private UplistLog uplistlogger;
    private Logger cmdlog;
    private String procNum;
    private String currentCoordinatorProcnum;      //I don't know whether it is OK? like a point?
    //private Map<String, ParticipantProcess> procnumToParticipantProcess;
    public PlayList playList;
    //private SortedSet<ParticipantProcess> upList;              //Do we need recoverList??
    private SortedSet<String> upList;
    private SortedSet<String> replyList;
    private SortedSet<String> recoverList;
    private SortedSet<String> broadcastList;
    private String command;
    //private boolean testing_flag;
    // parameter may include
    // 1. song
    // 2. URL1/2
    // 3. setCoordinatorProcNum
    private String parameter;
    private Boolean vote;
    private String song;
    private String URL;
    private int runtimes;
    //lookup table? ProcNum->Port???
    public static int CTIMEOUT = 2000; //Is this too short??
    public static int PTIMEOUT = 4000;
    public static final String INITIAL = "INITIAL";
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

    public ParticipantProcess(String filename, String testfile) {
        try {
            
            testflagconfig(testfile);
       
            //testing_flag = flag;
            runtimes = 0;
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
            currentCoordinatorProcnum = "-1";

            //command = "add"; // do not know why
            vote = true;
            playList = new PlayList();


            //String logFile = this.config.logfile;
            //System.out.println("logfile:" + this.config.logfile);
            //String uplistlogFile = this.config.uplistfile;
            this.uplistlogger = new UplistLog(this.config.uplistfile, true);
            this.logger = new Log(this.config.log3pcfile, true);


            this.state = State.ABORTED;
            cmdlog.info("A new process with ProcNum " + this.getProcNum() + " is recovered.");

            SortedSet<String> recoverUpList = new TreeSet<String>();
            recoverUpList = this.uplistlogger.extractUplistLog(this.uplistlogger.logread(this.config.uplistfile));


            SortedSet<String> intersectUpList = new TreeSet<String>();
            intersectUpList = this.uplistlogger.extractUplistLog(this.uplistlogger.logread(this.config.uplistfile));
            System.out.println("Print the intersectUpList....");
            for (String s : intersectUpList) {
                System.out.println(s);
            }
            SortedSet<String> SurviveList = new TreeSet<String>();
            SurviveList.add(this.procNum);

            System.out.println("Print the SurviveList....");
            for (String s : intersectUpList) {
                System.out.println(s);
            }

            boolean recv_logreply_flag = false;
            boolean recv_reply1_flag = false;
            boolean recv_logreply1_flag = false;

            if (!this.procNum.equals(Integer.toString(this.config.numProcesses))) {
                while (true) {
                    //String logtype = argslist[1];
                    //==null or == ""???????????????
                    if (logger.logread(this.config.log3pcfile).equals("")) {
                        this.upList = broadcastList;
                        break;
                    } else if (isIndependable() == 1) {
                        System.out.println("I am indenpendent 1");
                        // Broadcast the RECOVERY message to all the processes
                        this.broadcastMessage(this.getBroadcastList(), messageType.RECOVERY);
                        cmdlog.info("A new process with ProcNum " + this.getProcNum() + " has reported RECOVERY to all other processes.");
                        logger.independentUpdateLog(this.config.log3pcfile);

                        this.upList = broadcastList;
                        UpdatePlayListByLog();

                        //this.abort(command);
                        break;

                    } else if (isIndependable() == 2) {
                        System.out.println("I am indenpendent 2");
                        this.broadcastMessage(this.getBroadcastList(), messageType.RECOVERY);
                        cmdlog.info("A new process with ProcNum " + this.getProcNum() + " has reported RECOVERY to all other processes.");
                        UpdatePlayListByLog();
                        this.upList = broadcastList;
                        break;
                    } else {
                        System.out.println("I am indenpendent 3");
                        this.broadcastMessage(this.getBroadcastList(), messageType.RECOVERY);
                        cmdlog.info("A new process with ProcNum " + this.getProcNum() + " has reported RECOVERY to all other processes.");
                        this.broadcastMessage(this.getBroadcastList(), messageType.INQUIRY);

                        Message message = new Message();
                        
                        if (SurviveList.containsAll(intersectUpList)) {
                            String line = logger.logread(this.config.log3pcfile);
                                    //System.out.println(line);
                                    //System.out.println("What the fuck...");
                                    String[] argslist = line.split(" ");
                                    //System.out.println(argslist[2]);
                                    //System.out.println(argslist[3]);
                                    //System.out.println(argslist[4]);
                                    if(argslist[1].equals("PRE_COMMIT")){
                                        this.state = State.COMMITTABLE;
                                    } else if(argslist[1].equals("YES")) {
                                        this.state = State.UNCERTAIN;
                                    }
                                    this.command = argslist[2];
                                    this.song = argslist[3];
                                    this.URL = argslist[4];
                                    
                                    System.out.println("OHOHOHOH");
                                    this.upList.addAll(intersectUpList);
                                    for (String s : upList) {
                                        System.out.println(s);
                                    }
                                    this.upList.remove(this.procNum);
                                    for (String s : upList) {
                                        System.out.println(s);
                                    }
                                    
                                    this.currentCoordinatorProcnum = intersectUpList.first();
                                    System.out.println("OHOHOHOHAH");
                                    //System.out.println(t)
                                    //System.out.println(this.currentCoordinatorProcnum);
                                    System.out.println(intersectUpList.first());
                                    if (this.getCurrentCoordinatorProcnum().equals(this.procNum)) {
                                        this.CoordinatorTerminationProtocol();
                                    } else {
                                        this.ParticipantTerminationProtocol();
                                    } 
                                    UpdatePlayListByLog();
                                    this.upList.clear();
                                    this.setCurrentCoordinatorProcnum("-1");
                                    this.command = "";
                                    this.song = "";
                                    this.URL = "";
                                    recv_logreply_flag = true;
                                    this.state = State.ABORTED;
                            
                        } else {
                        
                        for (String msg : nc.getReceivedMsgs()) {
                            cmdlog.info("get some message...");
                            message.extractMessage(msg);
                            message.printMessage();

                            messageType msgType = message.getMsgType();
                            if (msgType == messageType.REPLY && !recv_reply1_flag) {
                                
                                System.out.println("I am Reply!");
                                System.out.println(message.getParameter());
                                this.playList.extractPlayList(message.getParameter());
                                this.upList.add(message.getMsgSource()); 
                                recv_logreply_flag = true;
                                this.playList.printPlayList();
                                recv_reply1_flag = true;
                                if (recv_logreply1_flag) {
                                    break;
                                }
                            } else if (msgType == messageType.LOGREPLY && !recv_logreply1_flag) {
                                
                                this.upList.add(message.getMsgSource());
                                recv_logreply_flag = true;
                                logger.log(message.getParameter());
                                recv_logreply1_flag = true;
                                if(recv_reply1_flag) {
                                    break;
                                }
                            } else if (msgType == messageType.UPLISTSYN) {
                                //extract the UpList from the message;
                                //get and update the intersection;
                                intersectUpList.retainAll(uplistlogger.extractUplistLog(message.getParameter()));
                                System.out.println("Print the message.getParameter...");
                                System.out.println(message.getParameter());
                                System.out.println("Print the extractUpList....");
                                for (String s : uplistlogger.extractUplistLog(message.getParameter())) {
                                    System.out.println(s);
                                }
                                System.out.println("Print the intersectUpList....");
                                for (String s : intersectUpList) {
                                    System.out.println(s);
                                }
                                //get and update the srcProcNum into a set;
                                SurviveList.add(message.getMsgSource());
                                System.out.println("Print the surviveList....");
                                for (String s : SurviveList) {
                                    System.out.println(s);
                                }
                                //if(the intersection set /in the srcProcNum set)    
                                //&& myself /in the intersection set) 
                                if (SurviveList.containsAll(intersectUpList)) {

                                    String line = logger.logread(this.config.log3pcfile);
                                    //System.out.println(line);
                                    //System.out.println("What the fuck...");
                                    String[] argslist = line.split(" ");
                                    //System.out.println(argslist[2]);
                                    //System.out.println(argslist[3]);
                                    //System.out.println(argslist[4]);
                                    if(argslist[1].equals("PRE_COMMIT")){
                                        this.state = State.COMMITTABLE;
                                    } else if(argslist[1].equals("YES")) {
                                        this.state = State.UNCERTAIN;
                                    }
                                    this.command = argslist[2];
                                    this.song = argslist[3];
                                    this.URL = argslist[4];
                                    
                                    System.out.println("OHOHOHOH");
                                    this.upList.addAll(intersectUpList);
                                    for (String s : upList) {
                                        System.out.println(s);
                                    }
                                    this.upList.remove(this.procNum);
                                    for (String s : upList) {
                                        System.out.println(s);
                                    }
                                    
                                    this.currentCoordinatorProcnum = intersectUpList.first();
                                    System.out.println("OHOHOHOHAH");
                                    //System.out.println(t)
                                    //System.out.println(this.currentCoordinatorProcnum);
                                    System.out.println(intersectUpList.first());
                                    if (this.getCurrentCoordinatorProcnum().equals(this.procNum)) {
                                        this.CoordinatorTerminationProtocol();
                                    } else {
                                        this.ParticipantTerminationProtocol();
                                    } 
                                    UpdatePlayListByLog();
                                    this.upList.clear();
                                    this.setCurrentCoordinatorProcnum("-1");
                                    this.command = "";
                                    this.song = "";
                                    this.URL = "";
                                    recv_logreply_flag = true;
                                    this.state = State.ABORTED;
                                    break;
                                    
                                    /*
                                    
                                    

                                    //update log + abort;
                                    
                                    //updateplaybylog
                                    

                                    //broadcast(messageType: LOGREPLY, command: abort);
                                    //broadcast(messageType: REPLY,)

                                    
                                    
                                     * 
                                     */
                                }
                            } else if (msgType == messageType.INQUIRY) {
                                String uplistStr = uplistlogger.toStringUpListLog(recoverUpList, this.procNum);
                                //reply uplist with messagetype uplistmessage containing the uplist
                                this.parameter = uplistStr;
                                this.sendMessage(message.getMsgSource(), messageType.UPLISTSYN);
                            } else if (msgType == messageType.STATE_REQ) {
                                
                                String line = logger.logread(this.config.log3pcfile);
                                String[] argslist = line.split(" ");
                                if (argslist[1].equals("PRE_COMMIT")) {
                                    this.state = State.COMMITTABLE;
                                } else if (argslist[1].equals("YES")) {
                                    this.state = State.UNCERTAIN;
                                }
                                this.command = argslist[2];
                                this.song = argslist[3];
                                this.URL = argslist[4];
                                
                                System.out.println("OHOHOHOH");
                                this.upList.addAll(intersectUpList);
                                for (String s : upList) {
                                    System.out.println(s);
                                }
                                this.upList.remove(this.procNum);
                                for (String s : upList) {
                                    System.out.println(s);
                                }
                                
                                this.currentCoordinatorProcnum = intersectUpList.first();
                                System.out.println("OHOHOHOHAH");
                                //System.out.println(t)
                                //System.out.println(this.currentCoordinatorProcnum);
                                System.out.println(intersectUpList.first());
                                
                                ParticipantTerminationProtocol_recvStateReq(message.getMsgSource());
                                
                                UpdatePlayListByLog();
                                this.upList.clear();
                                this.setCurrentCoordinatorProcnum("-1");
                                this.command = "";
                                this.song = "";
                                this.URL = "";
                                recv_logreply_flag = true;
                                this.state = State.ABORTED;
                                break;
                                
                            }
                        }

                        if (recv_logreply_flag) {
                            break;
                        }

                    }
                        
                    }
                }

            }

            upList = broadcastList;
            System.out.print("Uplist: ");
            for (String s : this.upList) {
                System.out.println(s + " ");
            }

        } catch (IOException e) {
            System.out.println(e);
        }

    }

    public void testflagconfig(String filename) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(filename));
            this.participantfailure_case1_flag = Boolean.parseBoolean(prop.getProperty("participantfailure_case1_flag"));
            this.participantfailure_case2_flag = Boolean.parseBoolean(prop.getProperty("participantfailure_case2_flag"));
            this.participantfailure_case3_flag = Boolean.parseBoolean(prop.getProperty("participantfailure_case3_flag"));
            this.coordinatorfailure_case1_flag = Boolean.parseBoolean(prop.getProperty("coordinatorfailure_case1_flag"));
            this.coordinatorfailure_case2_flag = Boolean.parseBoolean(prop.getProperty("coordinatorfailure_case2_flag"));
            this.coordinatorfailure_case3_flag = Boolean.parseBoolean(prop.getProperty("coordinatorfailure_case3_flag"));
            this.coordinatorfailure_case4_flag = Boolean.parseBoolean(prop.getProperty("coordinatorfailure_case4_flag"));
            this.coordinatorfailure_case5_flag = Boolean.parseBoolean(prop.getProperty("coordinatorfailure_case5_flag"));
            this.coordinatorfailure_case6_flag = Boolean.parseBoolean(prop.getProperty("coordinatorfailure_case6_flag"));
            this.totalfailure_case1_flag = Boolean.parseBoolean(prop.getProperty("totalfailure_case1_flag"));
            this.totalfailure_case2_flag = Boolean.parseBoolean(prop.getProperty("totalfailure_case2_flag"));
            this.totalfailure_case3_flag = Boolean.parseBoolean(prop.getProperty("totalfailure_case3_flag"));
            this.futurecoordinatorfailure_case1_flag = Boolean.parseBoolean(prop.getProperty("futurecoordinatorfailure_case1_flag"));
            this.cascadingcoordinatorfailure_case1_flag = Boolean.parseBoolean(prop.getProperty("cascadingcoordinatorfailure_case1_flag"));
            this.new_flag6 = Boolean.parseBoolean(prop.getProperty("new_flag6"));
            this.new_flag7 = Boolean.parseBoolean(prop.getProperty("new_flag7"));
            this.new_flag8 = Boolean.parseBoolean(prop.getProperty("new_flag8"));
            
            this.new_flag9 = Boolean.parseBoolean(prop.getProperty("new_flag9"));
            this.new_flag10 = Boolean.parseBoolean(prop.getProperty("new_flag10"));
            this.new_flag11 = Boolean.parseBoolean(prop.getProperty("new_flag11"));

            
            
        } catch (IOException ex) {
            Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void UpdatePlayListByLog() throws FileNotFoundException, IOException {
        BufferedReader input = new BufferedReader(new FileReader(this.config.log3pcfile));
        String line;
        while ((line = input.readLine()) != null) {

            System.out.println("line:" + line);

            String[] argslist = line.split(" ");

            for (String arg : argslist) {
                System.out.println(arg);
            }

            String logtype = argslist[1];
            if (argslist[1].equals(COMMIT)) {

                System.out.println("Enter into the commit session");
                commit(argslist[2], argslist[3], argslist[4]);
            }
        }
        System.out.println("Print Playlist after updating: ");
        this.playList.printPlayList();
    }

    //1: add log && send recovery && update playlist   2:send recovery  3. send recovery && send inquiry 
    public int isIndependable() {
        String logLastLine;
        try {
            logLastLine = logger.logread(this.config.log3pcfile);

            String logtype = logger.extractLogType(logLastLine);


            if (logtype.equals(INITIAL) || logtype.equals(START)) {
                return 1;
            } else if (logtype.equals(COMMIT) || logtype.equals(ABORT)) {
                return 2;
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 3;

    }

    // This function will be called uniformly by no matter git d
    // a coordinator or participant
    // which will automatically select someone to be a coordinator
    // and implement its role
    public void ProcessStartProtocol() {
        Message message = new Message();

        cmdlog.info("The process with ProcNum " + this.getProcNum() + " is started.");
        while (true) {

            runtimes++;

            this.uplistlogger.log(this.upList, this.procNum);
            System.out.println("I will print the PlayList: ");
            this.playList.printPlayList();
            upList.addAll(recoverList);
            this.uplistlogger.log(this.upList, this.procNum);
            recoverList.clear();

            /*
            if(this.currentCoordinatorProcnum.equals(this.getProcNum())) {
            
            this.parameter = this.playList.toStringPlayList();
            broadcastMessage(this.replyList,messageType.REPLY);
            try {
            this.parameter = logger.logread(this.config.logfile);
            } catch (FileNotFoundException ex) {
            Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
            Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
            }
            broadcastMessage(this.replyList,messageType.LOGREPLY);
            
            } else {
            send Uplist;
            
            }
             * 
             */

            //replyList.clear();

            cmdlog.info("wait for initial signal or the RECOVERY message from recovered processes...");
            boolean recv_initial_flag = false;
            boolean recv_logreply_flag = false;
            long startTime = System.currentTimeMillis();

            while (!recv_initial_flag) {

                /*
                long endTime = System.currentTimeMillis();
                if (endTime - startTime >= RTIMEOUT && this.currentCoordinatorProcnum == "-1" && !recv_logreply_flag) {
                if(the intersection set /in the srcProcNum set && myself /in the intersection set) {
                send the processes in the intersection set; 
                update my uplist = intersection/myself; 
                electionProtocol;
                
                }
                }
                 * 
                 */

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
                        this.parameter = message.getParameter();
                        cmdlog.info("The manager wants to set the process with ProcNum " + str[2] + " to be the coordinator");

                        logger.log(INITIAL, command, song, URL);



                        this.state = State.ABORTED;

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
                        recoverList.add(recoverProcNum);
                        upList.addAll(recoverList);
                        this.uplistlogger.log(this.upList, this.procNum);
                        cmdlog.info("The recovered ProcessNum is " + recoverProcNum);
                        cmdlog.info("I have added it to my UpList");
                    } else if (msgType == messageType.INQUIRY) {
                        cmdlog.info("The received message is a INQUIRY message....");
                        String inquiryProcNum = message.getMsgSource();
                        this.parameter = this.playList.toStringPlayList();
                        
                        this.sendMessage(inquiryProcNum, messageType.REPLY);
                        try {
                            this.parameter = logger.logread(this.config.log3pcfile);
                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        this.sendMessage(inquiryProcNum, messageType.LOGREPLY);
                        //recoverList.add(recoverProcNum);
                        //replyList.add(inquiryProcNum);
                        //cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                        //cmdlog.info("I have added it to my replyList");
                    } /*else if (msgType == messageType.REPLY) {
                    this.playList.extractPlayList(message.getParameter());        
                    } else if (msgType == messageType.LOGREPLY) {
                    recv_logreply_flag = true;
                    logger.log(message.getParameter(), command, song, URL);
                    } else if (msgType == messageType.TOTALFAILURE) {
                    extract the UpList from the message;
                    get and update the intersection; 
                    
                    get and update the srcProcNum into a set;
                    
                    }*/ else if (msgType == messageType.STATE_REQ) {
                        if (this.state == State.COMMITTED) {
                            this.sendMessage(message.getMsgSource(), messageType.COMMITTED);
                        } else if (this.state == State.ABORTED) {
                            this.sendMessage(message.getMsgSource(), messageType.ABORTED);
                        }
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
        this.uplistlogger.log(this.upList, this.procNum);
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
            if (!p.equals(ProcNum)) {
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

    public void commit(String command, String song, String URL) {
        //just add song, url to the playList;
        System.out.println("Enter Commit Function....");
        System.out.println(this.command);
        if (command.equals("add")) {
            System.out.println("Enter Commit Function....");
            playList.add(song, URL);
        } else if (command.equals("delete")) {
            playList.delete(song, URL);
        } else if (command.equals("edit")) {
            playList.edit(song, URL);//actually, we can use only URL2....
        }
        return;
    }

    public void abort(String command) {
        //System.out.println("ABORT");
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
        for (String s : this.upList) {
            System.out.println(s + " ");
        }
        
        
        if (this.new_flag11 && this.procNum.equals("2")) {
            System.exit(0);
        }

        if (totalfailure_case3_flag && this.procNum.equals("1") && runtimes >= 3) {
            System.exit(0);
        }

        this.broadcastMessage(messageType.STATE_REQ, this.command, this.parameter);


        if (this.totalfailure_case2_flag && this.procNum.equals("1") && runtimes >= 3) {
            System.exit(0);
        }

        if (this.cascadingcoordinatorfailure_case1_flag && this.procNum.equals("1")) {
            System.exit(0);
        }
        
        if (this.cascadingcoordinatorfailure_case1_flag && this.procNum.equals("2")) {
            System.exit(0);
        }




        int upListSize = this.getUpList().size();
        //int voteCount = upListSize - 1;
        int state_report_count = upListSize;
        Message message = new Message();
        boolean ExistAborted = false;
        boolean ExistCommitted = false;
        boolean ExistCommittable = false;
        if(this.state == State.COMMITTABLE) {
            ExistCommittable = true;
        }
        if(this.state == State.ABORTED) {
            ExistAborted = true;
        }
        if(this.state == State.COMMITTED) {
            ExistCommitted = true;
        }
        SortedSet<String> UncertainParticipantList = new TreeSet<String>();
        Set<String> VoteList = new HashSet<String>();

        /*
        if (this.procNum.equals("1")) {
        System.exit(0);
        }
         * 
         */

        /*
        if (this.procNum.equals("2")) {
        System.exit(0);
        }
        
        if (this.procNum.equals("3")) {
        System.exit(0);
        }
        
        if (this.procNum.equals("4")) {
        System.exit(0);
        }
         * 
         */
        
        
        


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
                    VoteList.add(srcNum);
                    ExistAborted = true;
                    state_report_count--;
                } else if (msgType == messageType.COMMITTED) {
                    VoteList.add(srcNum);
                    ExistCommitted = true;
                    state_report_count--;
                } else if (msgType == messageType.UNCERTAIN) {
                    VoteList.add(srcNum);
                    UncertainParticipantList.add(srcNum);
                    state_report_count--;
                } else if (msgType == messageType.COMMITTABLE) {
                    VoteList.add(srcNum);
                    ExistCommittable = true;
                    state_report_count--;
                } else if (msgType == messageType.RECOVERY) {
                    this.recoverList.add(message.getMsgSource());
                } /*else if (msgType == messageType.INQUIRY) {
                cmdlog.info("The received message is a INQUIRY message....");
                String inquiryProcNum = message.getMsgSource();
                //recoverList.add(recoverProcNum);
                //replyList.add(inquiryProcNum);
                cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                cmdlog.info("I have added it to my replyList");
                }*/
            }
        }

        upList.retainAll(VoteList);
        this.uplistlogger.log(this.upList, this.procNum);

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
            
            
            if(isReplicatedLog()) {
                
            }
            else {
                logger.log(ABORT, command, song, URL);
            }
            this.abort(this.command);
            this.broadcastMessage(messageType.ABORT);
        } // TR 2
        else if (this.state == State.COMMITTED || ExistCommitted) {
            // the coordinator should first check whether there has already been a commit
            // record in its log
            // if not, it will log commit
            cmdlog.info("TR 2 entered.");
            if(isReplicatedLog()) {
                
            }
            else {
                logger.log(COMMIT, command, song, URL);
            }
            this.commit(command, song, URL);
            this.broadcastMessage(messageType.COMMIT);
        } // TR 3
        else if (this.state == State.UNCERTAIN && !ExistCommittable) {
            cmdlog.info("TR 3 entered.");
            this.state = State.ABORTED;
            logger.log(ABORT, command, song, URL);
            this.abort(command);
            
            



            this.broadcastMessage(messageType.ABORT);
            
        } // TR 4
        else {
            if(this.new_flag6 && this.procNum.equals("1")) {
                System.exit(0);
            }
            
            
            if (this.new_flag7 && this.procNum.equals("1")) {
                this.broadcastMessage(UncertainParticipantList, messageType.PRE_COMMIT, command, parameter, "2");
                System.exit(0);
            } else {
                this.broadcastMessage(UncertainParticipantList, messageType.PRE_COMMIT);
            }

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
                    } /*else if (msgType == messageType.INQUIRY) {
                    cmdlog.info("The received message is a INQUIRY message....");
                    String inquiryProcNum = message.getMsgSource();
                    //recoverList.add(recoverProcNum);
                    replyList.add(inquiryProcNum);
                    cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                    cmdlog.info("I have added it to my replyList");
                    }*/
                }
            }
    


            logger.log(COMMIT, command, song, URL);
            this.commit(command, song, URL);

            if (this.new_flag9 && this.procNum.equals("2")) {
                System.exit(0);
            }


            if (this.new_flag8 && this.procNum.equals("1")) {
                System.exit(0);
            }
            
            
            if (this.new_flag11 && this.procNum.equals("1")) {
                System.exit(0);
            }
            
            this.broadcastMessage(messageType.COMMIT);
            
            
        }
    }

    public boolean isReplicatedLog() {
        String line;
        try {
            line = logger.logread(this.config.log3pcfile);
            String[] argslist = line.split(" ");
            if (argslist[1].equals("ABORT") || argslist[1].equals("COMMIT")) {
                return true;
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;

    }

    public void Managerprocess() {
        Message message = new Message();
        //String line = null;
        while (true) {
            String line = null;
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

    public void ParticipantTerminationProtocol_recvStateReq(String Src) {

        if (totalfailure_case3_flag && this.procNum.equals("2") && runtimes >= 3) {
            System.exit(0);
        }

        Message message = new Message();
        messageType msgType = null;
        messageType stateType = null;

        if (this.state == State.ABORTED) {
            stateType = messageType.ABORTED;
        } else if (this.state == State.UNCERTAIN) {
            stateType = messageType.UNCERTAIN;
        } else if (this.state == State.COMMITTABLE) {
            stateType = messageType.COMMITTABLE;
        } else if (this.state == State.COMMITTED) {
            stateType = messageType.COMMITTED;
        }

        this.currentCoordinatorProcnum = Src;
        updateUpListAfterRecvStateReq(this.currentCoordinatorProcnum);

        assert (this.getCurrentCoordinatorProcnum().equals(message.getMsgSource()));
        this.sendMessage(this.getCurrentCoordinatorProcnum(), stateType);
        //cmdlog.info("fuckkkk");
        // I guess this sendMessage function might be wrong 
        // because different participant may not agree on the same coordinator
        
        if (this.totalfailure_case2_flag && this.procNum.equals("2") && runtimes >= 3) {
            System.exit(0);
        }


        //cmdlog.info("ilovewangqi");
        // wait the response from the coordinator
        boolean recv_response = false;
        long startTime = System.currentTimeMillis();
        while (!recv_response) {
            //cmdlog.info("ilovewangqi1");
            long endTime = System.currentTimeMillis();
            if (endTime - startTime >= PTIMEOUT) {
                this.removeCoordinatorFromUpList();
                this.ElectionProtocol();
                //cmdlog.info("ilovewangqi2s");
                cmdlog.info(this.getCurrentCoordinatorProcnum());

                if (this.getCurrentCoordinatorProcnum().equals(this.procNum)) {
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
                    this.abort(command);
                    return;
                } else if (msgType == messageType.COMMIT) {
                    // you need to first check whether the log contains the C record 
                    // for the current command
                    logger.log(COMMIT, command, song, URL);
                    this.commit(command, song, URL);
                    return;
                } else if (msgType == messageType.PRE_COMMIT) {
                    logger.log(PRE_COMMIT, command, song, URL);
                    this.sendMessage(this.getCurrentCoordinatorProcnum(), messageType.ACK);
                    recv_response = true;
                    break;
                } else if (msgType == messageType.RECOVERY) {
                    this.recoverList.add(message.getMsgSource());
                } /*else if (msgType == messageType.INQUIRY) {
                cmdlog.info("The received message is a INQUIRY message....");
                String inquiryProcNum = message.getMsgSource();
                //recoverList.add(recoverProcNum);
                replyList.add(inquiryProcNum);
                cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                cmdlog.info("I have added it to my replyList");
                }*/ else if (msgType == messageType.STATE_REQ) {
                    ParticipantTerminationProtocol_recvStateReq(message.getMsgSource());
                    return;
                }
            }
        }
        //cmdlog.info("ilovewangqi3");
        
        


        // wait the final decision (commit/abort) from the coordinator
        recv_response = false;
        startTime = System.currentTimeMillis();
        while (!recv_response) {
            long endTime = System.currentTimeMillis();
            if (endTime - startTime >= PTIMEOUT) {
                this.removeCoordinatorFromUpList();
                this.ElectionProtocol();
                if (this.getCurrentCoordinatorProcnum().equals(this.procNum)) {
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
                    this.commit(this.command, song, URL);
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
                } /*else if (msgType == messageType.INQUIRY) {
                cmdlog.info("The received message is a INQUIRY message....");
                String inquiryProcNum = message.getMsgSource();
                //recoverList.add(recoverProcNum);
                replyList.add(inquiryProcNum);
                cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                cmdlog.info("I have added it to my replyList");
                }*/ else if (msgType == messageType.STATE_REQ) {
                    ParticipantTerminationProtocol_recvStateReq(message.getMsgSource());
                    return;
                }
            }
        }
    }

    public void ParticipantTerminationProtocol() {
        
        if (totalfailure_case3_flag && this.procNum.equals("2") && runtimes >= 3) {
            System.exit(0);
        }
        
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
                if (this.getCurrentCoordinatorProcnum().equals(this.procNum)) {
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

                    this.currentCoordinatorProcnum = message.getMsgSource();
                    updateUpListAfterRecvStateReq(this.currentCoordinatorProcnum);

                    assert (this.getCurrentCoordinatorProcnum().equals(message.getMsgSource()));
                    this.sendMessage(this.getCurrentCoordinatorProcnum(), stateType);
                    //cmdlog.info("fuckkkk");
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
                } /*else if (msgType == messageType.INQUIRY) {
                cmdlog.info("The received message is a INQUIRY message....");
                String inquiryProcNum = message.getMsgSource();
                //recoverList.add(recoverProcNum);
                replyList.add(inquiryProcNum);
                cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                cmdlog.info("I have added it to my replyList");
                }*/ else if (msgType == messageType.STATE_REQ) {
                    ParticipantTerminationProtocol_recvStateReq(message.getMsgSource());
                    return;
                }
            }
        }


        if (this.totalfailure_case2_flag && this.procNum.equals("2") && runtimes >= 3) {
            System.exit(0);
        }


        //cmdlog.info("ilovewangqi");
        // wait the response from the coordinator
        boolean recv_response = false;
        startTime = System.currentTimeMillis();
        while (!recv_response) {
            //cmdlog.info("ilovewangqi1");
            long endTime = System.currentTimeMillis();
            if (endTime - startTime >= PTIMEOUT) {
                this.removeCoordinatorFromUpList();
                this.ElectionProtocol();
                //cmdlog.info("ilovewangqi2s");
                cmdlog.info(this.getCurrentCoordinatorProcnum());

                if (this.getCurrentCoordinatorProcnum().equals(this.procNum)) {
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
                    this.abort(command);
                    return;
                } else if (msgType == messageType.COMMIT) {
                    // you need to first check whether the log contains the C record 
                    // for the current command
                    logger.log(COMMIT, command, song, URL);
                    this.commit(command, song, URL);
                    return;
                } else if (msgType == messageType.PRE_COMMIT) {
                    this.sendMessage(this.getCurrentCoordinatorProcnum(), messageType.ACK);
                    logger.log(PRE_COMMIT, command, song, URL);
                    this.state=State.COMMITTABLE;
                    recv_response = true;
                    break;
                } else if (msgType == messageType.RECOVERY) {
                    this.recoverList.add(message.getMsgSource());
                } /*else if (msgType == messageType.INQUIRY) {
                cmdlog.info("The received message is a INQUIRY message....");
                String inquiryProcNum = message.getMsgSource();
                //recoverList.add(recoverProcNum);
                replyList.add(inquiryProcNum);
                cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                cmdlog.info("I have added it to my replyList");
                }*/ else if (msgType == messageType.STATE_REQ) {
                    ParticipantTerminationProtocol_recvStateReq(message.getMsgSource());
                    return;
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
                if (this.getCurrentCoordinatorProcnum().equals(this.procNum)) {
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
                    this.commit(command, song, URL);
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
                } /*else if (msgType == messageType.INQUIRY) {
                cmdlog.info("The received message is a INQUIRY message....");
                String inquiryProcNum = message.getMsgSource();
                //recoverList.add(recoverProcNum);
                replyList.add(inquiryProcNum);
                cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                cmdlog.info("I have added it to my replyList");
                }*/ else if (msgType == messageType.STATE_REQ) {
                    ParticipantTerminationProtocol_recvStateReq(message.getMsgSource());
                    return;
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
                } /*else if (msgType == messageType.INQUIRY) {
                cmdlog.info("The received message is a INQUIRY message....");
                String inquiryProcNum = message.getMsgSource();
                //recoverList.add(recoverProcNum);
                replyList.add(inquiryProcNum);
                cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                cmdlog.info("I have added it to my replyList");
                }*/ else if (msgType == messageType.STATE_REQ) {
                    ParticipantTerminationProtocol_recvStateReq(message.getMsgSource());
                    return;
                }
            }
        }

        // Now we pass the VOTE_REQ state
        // go ahead to the vote process

        // If voting YES
        if (this.castVote(this.command) == true) {
                        
            if (participantfailure_case1_flag && this.procNum.equals("2")) {
                System.exit(0);
            }
            
            logger.log(YES, command, song, URL);
            this.state = State.UNCERTAIN;



            sendMessage(this.getCurrentCoordinatorProcnum(), messageType.YES);
            cmdlog.info("after sending YES");
            
            
            
            if (this.futurecoordinatorfailure_case1_flag && this.procNum.equals("1")) {
                System.exit(0);
            }
            
            if (this.futurecoordinatorfailure_case1_flag && this.procNum.equals("2")) {
                System.exit(0);
            }

            if (this.totalfailure_case1_flag && this.procNum.equals("1")) {
                System.exit(0);
            }
            if (this.totalfailure_case1_flag && this.procNum.equals("2")) {
                System.exit(0);
            }




            /*
            if (this.procNum.equals("1") && testing_flag) {
            System.exit(0);
            }
            if (this.procNum.equals("2") && testing_flag) {
            System.exit(0);
            }
             * 
             */
            /*
            if (this.getProcNum().equals("1")) {
            System.exit(0);
            }
            
            if (this.getProcNum().equals("2")) {
            System.exit(0);
            }
             * 
             */

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
                    System.out.println("Uplist:");
                    for (String s : this.upList) {
                        System.out.println(s + " ");
                    }
                    this.removeCoordinatorFromUpList();
                    
                    
                    
                    
                    // election protocol does the following things: 
                    // it finds the participants existing in this participant's uplist
                    // and then choose it as the new coordinator
                    cmdlog.info("process " + this.getCurrentCoordinatorProcnum() + " is and old coordinator");
                    this.ElectionProtocol();
                    cmdlog.info("process " + this.getCurrentCoordinatorProcnum() + " is elected as a new coordinator");
                    if (this.getCurrentCoordinatorProcnum().equals(this.procNum)) {
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

                        /*
                        if (this.procNum.equals("1") && testing_flag) {
                        System.exit(0);
                        }
                        if (this.procNum.equals("2") && testing_flag) {
                        System.exit(0);
                        }
                         * 
                         */
                        // send ack to coordinator
                        if (this.new_flag9 && this.procNum.equals("1")) {
                            System.exit(0);
                        }
                        
                        if (this.participantfailure_case2_flag && this.procNum.equals("1")) {
                            System.exit(0);
                        }
                        
                        if (this.participantfailure_case2_flag && this.procNum.equals("2")) {
                            System.exit(0);
                        }

                        logger.log(PRE_COMMIT, command, song, URL);
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
                                if (this.new_flag10 && this.procNum.equals("1")) {
                                    System.exit(0);
                                }

                                if (this.new_flag10 && this.procNum.equals("2")) {
                                    System.exit(0);
                                }

                                // election protocol does the following things: 
                                // it finds the participants existing in this participant's uplist
                                // and then choose it as the new coordinator
                                this.ElectionProtocol();
                                if (this.getCurrentCoordinatorProcnum().equals(this.procNum)) {
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
                                    System.out.println("Enter the commit part:");
                                    this.commit(command, song, URL);



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
                                } /*else if (msgType == messageType.INQUIRY) {
                                cmdlog.info("The received message is a INQUIRY message....");
                                String inquiryProcNum = message.getMsgSource();
                                //recoverList.add(recoverProcNum);
                                replyList.add(inquiryProcNum);
                                cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                                cmdlog.info("I have added it to my replyList");
                                }*/ else if (msgType == messageType.STATE_REQ) {
                                    ParticipantTerminationProtocol_recvStateReq(message.getMsgSource());
                                    return;
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
                    } /*else if (msgType == messageType.INQUIRY) {
                    cmdlog.info("The received message is a INQUIRY message....");
                    String inquiryProcNum = message.getMsgSource();
                    //recoverList.add(recoverProcNum);
                    replyList.add(inquiryProcNum);
                    cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                    cmdlog.info("I have added it to my replyList");
                    }*/ else if (msgType == messageType.STATE_REQ) {
                        ParticipantTerminationProtocol_recvStateReq(message.getMsgSource());
                        return;
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

    public void updateUpListAfterRecvStateReq(String StateReqSource) {
        SortedSet<String> DifSet = new TreeSet<String>();
        for (int i = 0; i < Integer.parseInt(StateReqSource); i++) {
            DifSet.add(Integer.toString(i));
        }
        this.upList.removeAll(DifSet);
        this.uplistlogger.log(this.upList, this.procNum);
    }

    //send VOTE_REQ
    public void CoordinatorCommitProtocol() {
        Message message = new Message();
        //Set <ParticipantProcess> yesVoteList;
        Set<String> yesVoteList;
        Set<String> VoteList;
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


        if (this.coordinatorfailure_case1_flag && this.procNum.equals("0")) {
            System.exit(0);
        }

        logger.log(START, command, song, URL);

        //System.exit(0);

        // compress the UpList into a string
        // and send it to all the participants in the UpList
        System.out.println("Uplist: ");
        for (String s : this.upList) {
            System.out.println(s + " ");
        }

        String toSendParameter = message.toStringUpList(this.upList, this.procNum);
        System.out.println(toSendParameter);
        this.broadcastMessage(messageType.VOTE_REQ, this.command, toSendParameter);
        // this.broadcastMessage(Message.messageType.VOTE_REQ);

        if (this.cascadingcoordinatorfailure_case1_flag && this.procNum.equals("0")) {
            System.exit(0);
        }


        if (this.totalfailure_case2_flag && this.procNum.equals("0") && runtimes >= 3) {
            System.exit(0);
        }



        if (this.coordinatorfailure_case2_flag && this.procNum.equals("0")) {
            System.exit(0);
        }
        /*
        if (this.getProcNum().equals("0")) {
        System.exit(0);
        }
         * 
         */

        /*
        if (this.procNum.equals("0") && testing_flag) {
        System.exit(0);
        }
         * 
         */


        cmdlog.info("after broadcast VOTE_REQ");

        decision = true;
        yesVoteList = new HashSet<String>();
        VoteList = new HashSet<String>();

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
                    VoteList.add(message.getMsgSource());
                    yesVoteList.add(message.getMsgSource());
                    voteCount--;
                } else if (msgType == messageType.NO) {
                    VoteList.add(message.getMsgSource());
                    decision = false;
                    voteCount--;
                    //Do we need to continue ???
                } else if (msgType == messageType.RECOVERY) {
                    this.recoverList.add(message.getMsgSource());
                } /*else if (msgType == messageType.INQUIRY) {
                cmdlog.info("The received message is a INQUIRY message....");
                String inquiryProcNum = message.getMsgSource();
                //recoverList.add(recoverProcNum);
                replyList.add(inquiryProcNum);
                cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                cmdlog.info("I have added it to my replyList");
                }*/
            }
        }
        cmdlog.info("after receive vote");


        //upList.clear();
        //upList.addAll(yesVoteList);
        upList.retainAll(VoteList);
        this.uplistlogger.log(this.upList, this.procNum);

        //upList=yesVoteList;

        // Now the coordinator has finished the collection
        // it begins to process the votes based on his own vote
        // decision may be made before, or not

        //if (this.getProcNum().equals("0")) {
        //System.exit(0);
        //}

        /*
        System.out.println("Kill the coordinator with ProcNum 0");
        try {
        Thread.sleep(7000);
        } catch (InterruptedException ex) {
        Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Kill the coordinator with ProcNum 1");
        try {
        Thread.sleep(1);
        } catch (InterruptedException ex) {
        Logger.getLogger(ParticipantProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
         * 
         */

        /*
        if (this.procNum.equals("0")) {
        System.exit(0);
        }
         * 
         */

        // Decision is YES
        if (decision && this.castVote(command) == true) {
            //send precommit
            //add some manual error here....
            // coordinator needs to log PRE_COMMIT
            logger.log(PRE_COMMIT, command, song, URL);
            if (this.coordinatorfailure_case4_flag && this.procNum.equals("0")) {
                this.broadcastMessage(this.upList, messageType.PRE_COMMIT, command, parameter, "1");
                System.exit(0);
            } else if (this.new_flag6 && this.procNum.equals("0")) {
                this.broadcastMessage(this.upList, messageType.PRE_COMMIT, command, parameter, "1");
                System.exit(0);
            } else if (this.new_flag7 && this.procNum.equals("0")) {
                this.broadcastMessage(this.upList, messageType.PRE_COMMIT, command, parameter, "1");
                System.exit(0);
            } else if (this.new_flag8 && this.procNum.equals("0")) {
                this.broadcastMessage(this.upList, messageType.PRE_COMMIT, command, parameter, "1");
                System.exit(0);
            } else {
                this.broadcastMessage(Message.messageType.PRE_COMMIT);//I hope to add some errors when sending to nth client...

            }
            
            if (this.new_flag11 && this.procNum.equals("0")) {
                System.exit(0);
            }
            
            if (this.new_flag9 && this.procNum.equals("0")) {
                System.exit(0);
            }

            if (this.futurecoordinatorfailure_case1_flag && this.procNum.equals("0")) {
                System.exit(0);
            }

            if (this.totalfailure_case1_flag && this.procNum.equals("0")) {
                System.exit(0);
            }
            
            if (this.coordinatorfailure_case3_flag && this.procNum.equals("0")) {
                System.exit(0);
            }
            /*
            if (this.procNum.equals("0") && testing_flag) {
            System.exit(0);
            }
             * 
             */

            /*
            if (this.getProcNum().equals("0")) {
            System.exit(0);
            }
             * 
             */



            cmdlog.info("after broadcast PRE_COMMIT");
            //add some manul error here.
            yesVoteList = new HashSet<String>();
            VoteList.clear();
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
                        VoteList.add(message.getMsgSource());
                        voteCount--;
                    } else if (msgType == messageType.RECOVERY) {
                        this.recoverList.add(message.getMsgSource());
                    } /*else if (msgType == messageType.INQUIRY) {
                    cmdlog.info("The received message is a INQUIRY message....");
                    String inquiryProcNum = message.getMsgSource();
                    //recoverList.add(recoverProcNum);
                    replyList.add(inquiryProcNum);
                    cmdlog.info("The inquiry ProcessNum is " + inquiryProcNum);
                    cmdlog.info("I have added it to my replyList");
                    }*/
                }
            }

            upList.retainAll(VoteList);
            this.uplistlogger.log(this.upList, this.procNum);

            cmdlog.info("after receive ack");
            //ACK is not received for all. Just ignore....continue to send commit...

            //commit
            logger.log(COMMIT, command, song, URL);
            
            if(totalfailure_case3_flag && this.procNum.equals("0") && runtimes >= 3) {
                System.exit(0);
            }
            
            
            this.commit(command, song, URL);//execute the command....
            //System.out.println(COMMIT);

            //error here..after commit....
            
            if (this.new_flag10 && this.procNum.equals("0")) {
                System.exit(0);
            }
            
                        
            if (this.coordinatorfailure_case6_flag && this.procNum.equals("0")) {
                this.broadcastMessage(this.upList, messageType.COMMIT, command, parameter, "1");
                System.exit(0);
            } else {
                this.broadcastMessage(messageType.COMMIT);
            }
            if (this.coordinatorfailure_case5_flag && this.procNum.equals("0")) {
                System.exit(0);
            }

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
