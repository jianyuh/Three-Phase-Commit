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
    
    public Log(String filePath) throws FileNotFoundException, IOException{
        this(new File(filePath), false);
    }
    
    public Log(String filePath, boolean create) throws FileNotFoundException, IOException {
        this(new File(filePath), create);
    }
    
    public Log(File file) throws FileNotFoundException, IOException {
        this(file, false);
    }
    
    public Log(File file, boolean create) throws FileNotFoundException, IOException {
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
    
    public boolean log(String data, String command, String song, String URL) {
        try {
            this.fileWriter.write(System.currentTimeMillis()+" "+data+" "+command+" "+song+" "+URL+"\n");
            this.fileWriter.flush();
        } catch(IOException e) {
            return false;
        }
        return true;
        
    }
    
}
