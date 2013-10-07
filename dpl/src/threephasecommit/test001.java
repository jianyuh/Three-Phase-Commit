/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package threephasecommit;

/**
 *
 * @author xwang
 */
public class test001 {
        public static void main(String[] args) {
        ParticipantProcess participant = new ParticipantProcess("config0.txt", false);
        participant.ProcessStartProtocol();
        //participant.CoordinatorCommitProtocol();
        participant.playList.printPlayList();
        
    }
    
}
