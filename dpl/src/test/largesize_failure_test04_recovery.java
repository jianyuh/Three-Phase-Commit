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
public class largesize_failure_test04_recovery {
        public static void main(String[] args) {
        ParticipantProcess participant = new ParticipantProcess("largeconfig4.txt", "testfile_good.txt");
        //participant.ProcessStartProtocol();
        //participant.CoordinatorCommitProtocol();
        participant.Managerprocess();
        
    }
    
}
