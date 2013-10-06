/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package threephasecommit;

/**
 *
 * @author xwang
 */
public class testInt {
        public static void main(String[] args) {
        ParticipantProcess participant = new ParticipantProcess("config0.txt");
        
        //participant.upList.add("1")
        
        participant.updateUpListAfterRecvStateReq("0") ;
        
        System.out.println(participant.getUpList());


    }
    
}
