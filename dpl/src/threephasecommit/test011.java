/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package threephasecommit;

/**
 *
 * @author jianyu
 */
public class test011 {
    public static void main(String[] args) {
        ParticipantProcess participant = new ParticipantProcess("config1.txt", false);
        participant.ProcessStartProtocol();
        //participant.CoordinatorCommitProtocol();
        participant.playList.printPlayList();
        //participant.ParticipantCommitProtocol();
    }
    
}
