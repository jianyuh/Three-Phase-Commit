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
public class smallsize_failure_test03_recovery {
        public static void main(String[] args) {
        ParticipantProcess participant = new ParticipantProcess("config3.txt", "testfile_good.txt");
        //participant.ProcessStartProtocol();
        //participant.CoordinatorCommitProtocol();
        participant.Managerprocess();
        
    }
    
}
