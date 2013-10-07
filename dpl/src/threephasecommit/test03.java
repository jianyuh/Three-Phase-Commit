/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package threephasecommit;

/**
 *
 * @author jianyu
 */
public class test03 {
    public static void main(String[] args) {
        ParticipantProcess participant = new ParticipantProcess("config3.txt", true);
        //participant.CoordinatorCommitProtocol();
        //participant.ProcessStartProtocol();
        //participant.ParticipantCommitProtocol();
        //participant.playList.printPlayList();
        participant.Managerprocess();
    }
}
