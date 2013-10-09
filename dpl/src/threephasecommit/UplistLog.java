package threephasecommit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 *
 */
public class UplistLog {
    
    private File file;
    private String filePath;    
    private FileWriter fileWriter;
    private BufferedWriter outStream;
    
    public UplistLog(String filePath, boolean create) throws FileNotFoundException, IOException {
        File file = new File(filePath);
        if (file == null) {
            throw new NullPointerException();
        }
        
        if (!file.exists()) {
            if (create) {
                file.createNewFile();
            } else {
                throw new FileNotFoundException();
            }
        }
        if(!file.canWrite()) {
            throw new IOException("cannot write to: " + file.getPath());
        }
        
        file.createNewFile();
        
        this.file = file;
        this.filePath = file.getPath();
        
    }
    
    public String getFilePath() {
        return this.filePath;
    }
    
    //may be some problem...
    public SortedSet<String> extractUplistLog(String UplistLog){
        TreeSet <String> UpList = new TreeSet <String>();
        String[] temp  = UplistLog.split("#");
        for (String s: temp) {
            UpList.add(s);
        }
        return UpList;
    }

    public String toStringUpListLog(SortedSet<String> UpList, String SelfProcNum) {
        
        String uplistLog = "";
        for (String str : UpList) {
            uplistLog = uplistLog + str + "#";
        }
        uplistLog = uplistLog + SelfProcNum;
        return uplistLog;
    }

    /*
    public boolean log(String data) {
        try {
            this.fileWriter.write(data +"\n");
            this.fileWriter.flush();
        } catch(IOException e) {
            return false;
        }
        return true;
    }*/
    
    public String logread(String fileName) throws FileNotFoundException, IOException {
        BufferedReader input = new BufferedReader(new FileReader(fileName));
        String last="", line;
        while ((line = input.readLine()) != null) {
            last = line;
        }
        return last;
    }
    
    public void independentUpdateLog(String fileName) {
        String lastline;
        try {
            lastline = logread(fileName);
            String[] argslist = lastline.split(" ");
            if (lastline != "") {
                this.fileWriter.write(System.currentTimeMillis() + " " + "ABORT" + " " + argslist[2] + " " + argslist[3] + " " + argslist[4] + "\n");
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Log.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Log.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    public String extractLogType(String line) {
        String[] argslist = line.split(" ");
        String logtype = argslist[1];
        return logtype;
    }
    
    public boolean log(SortedSet<String> UpList, String SelfProcNum) {
        try {
            this.fileWriter = new FileWriter(this.file, false);
        } catch (IOException ex) {
            Logger.getLogger(UplistLog.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.outStream= new BufferedWriter(this.fileWriter);
        
        System.out.println("Enter Log Function");
        String uplistLog = toStringUpListLog(UpList, SelfProcNum);
        try {
            //this.fileWriter.write(uplistLog);
            //this.fileWriter.flush();
            this.outStream.write(uplistLog+"\n");
            this.outStream.flush();
        } catch(IOException e) {
            return false;
        }
        return true;
        
    }
    
}
