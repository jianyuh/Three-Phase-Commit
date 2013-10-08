/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import threephasecommit.*;

/**
 *
 * @author xwang
 */
public class participant_failure_test02 {
        public static void main(String[] args) {
        ParticipantProcess participant = new ParticipantProcess("config2.txt", true);
        participant.ProcessStartProtocol();
        //participant.CoordinatorCommitProtocol();
        participant.playList.printPlayList();
        
    }
    
}
