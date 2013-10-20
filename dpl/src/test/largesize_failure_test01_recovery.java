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
public class largesize_failure_test01_recovery {
        public static void main(String[] args) {
        ParticipantProcess participant = new ParticipantProcess("largeconfig1.txt", "testfile_good.txt");
        participant.ProcessStartProtocol();
        //participant.CoordinatorCommitProtocol();
        participant.playList.printPlayList();
        
    }
    
}
