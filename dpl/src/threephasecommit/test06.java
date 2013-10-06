/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package threephasecommit;

/**
 *
 * @author jianyu
 */
public class test06 {
    public static void main(String[] args) {
        ParticipantProcess participant = new ParticipantProcess("config6.txt");
        participant.Managerprocess();
    }
}
