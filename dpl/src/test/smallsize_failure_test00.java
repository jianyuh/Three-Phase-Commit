/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import threephasecommit.*;

/**
 *
 * 
 */
public class smallsize_failure_test00 {
        public static void main(String[] args) {
            
        ParticipantProcess participant = new ParticipantProcess("config0.txt", "testfile0.txt");
        participant.ProcessStartProtocol();
        //participant.CoordinatorCommitProtocol();
        participant.playList.printPlayList();
        
        
        
    }
    
}
