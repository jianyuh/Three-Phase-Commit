/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package threephasecommit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 */
public class testlog {
    public static void main(String[] args) {
        UplistLog logger;
        SortedSet<String> strset = new TreeSet<String>();
        String logFile = "test.txt";
        try {
            logger = new UplistLog(logFile, true);
            
            strset.add("1");
            logger.log(strset,"2");
            logger.log(strset,"2");
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(testlog.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(testlog.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
        
    }
    
}
