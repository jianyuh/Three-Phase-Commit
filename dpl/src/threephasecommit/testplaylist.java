/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package threephasecommit;

/**
 *
 * @author jianyu
 */
public class testplaylist {
    
    public static void main(String[] args) {
        PlayList playList = new PlayList();
        //playList.add("song","name");
        //playList.add("song2","name2");
        String str = playList.toStringPlayList();
        PlayList playList2 = new PlayList();
        playList2.extractPlayList(str);
        

    }
}
