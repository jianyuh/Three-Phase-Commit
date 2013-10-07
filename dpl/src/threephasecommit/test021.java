/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package threephasecommit;

/**
 *
 * @author jianyu
 */
public class test021 {
    
    public static void main(String[] args) {
        ParticipantProcess participant = new ParticipantProcess("config2.txt",false);
        //participant.CoordinatorCommitProtocol();
        //participant.ParticipantCommitProtocol();
        participant.ProcessStartProtocol();
        participant.playList.printPlayList();
    }
}
