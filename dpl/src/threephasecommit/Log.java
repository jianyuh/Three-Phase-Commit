package threephasecommit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jianyu
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
        this.fileWriter = new FileWriter(this.file);
    }
    
    public String getFilePath() {
        return this.filePath;
    }

    public boolean log(String data) {
        try {
            this.fileWriter.write(System.currentTimeMillis()+":"+ data +"\n");
            this.fileWriter.flush();
        } catch(IOException e) {
            return false;
        }
        return true;
    }
    
}
