/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package threephasecommit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 */
public class testlog {
    public static void main(String[] args) {
        Log logger;
        String logFile = "test.txt";
        try {
            logger = new Log(logFile, false);
            
            logger.log("test11");
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(testlog.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(testlog.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        
    }
    
}
