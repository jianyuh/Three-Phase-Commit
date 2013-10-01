/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package threephasecommit;

/**
 *
 * @author jianyu
 */
public class test02 {
    
    public static void main(String[] args) {
        ParticipantProcess participant = new ParticipantProcess("config2.txt");
        //participant.CoordinatorCommitProtocol();
        participant.ParticipantCommitProtocol();
        participant.playList.printPlayList();
    }
}
