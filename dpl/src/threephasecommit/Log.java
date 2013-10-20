package threephasecommit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
public class Log {
    
    private File file;
    private String filePath;    
    private FileWriter fileWriter;
    
    public Log(String filePath, boolean create) throws FileNotFoundException, IOException {
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
        
        this.file = file;
        this.filePath = file.getPath();
        this.fileWriter = new FileWriter(this.file, true);
    }
    
    public String getFilePath() {
        return this.filePath;
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
            System.out.println("Enter independentUpdateLog....");
            lastline = logread(fileName);
            System.out.println(lastline);
            String[] argslist = lastline.split(" ");
            
            for (String arg: argslist) {
                System.out.println(arg);
            }
            
            if (lastline != "") {
                System.out.println("Updating now....");
                this.fileWriter.write(System.currentTimeMillis() + " " + "ABORT" + " " + argslist[2] + " " + argslist[3] + " " + argslist[4] + "\n");
                this.fileWriter.flush();
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
    
    public boolean log(String data, String command, String song, String URL) {
        try {
            this.fileWriter.write(System.currentTimeMillis()+" "+data+" "+command+" "+song+" "+URL+"\n");
            this.fileWriter.flush();
        } catch(IOException e) {
            return false;
        }
        return true;
        
    }
    
    public boolean log(String parameter) {
        try {
            this.fileWriter.write(parameter);
            this.fileWriter.flush();
        } catch(IOException e) {
            return false;
        }
        return true;
    }
    
}
