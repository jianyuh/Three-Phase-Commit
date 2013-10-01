package threephasecommit;


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jianyu
 */

import java.util.Hashtable;
import java.util.Map;

public class PlayList {
    public static Hashtable<String, String> playList = new Hashtable<String, String>();
    void add(String songName, String URL){
        //if (playList.contains(songName)) {
        playList.put(songName, URL);
        //}
        // Not contain??
    }
    
    //delete just songName??
    void delete(String songName){
        if(playList.containsKey(songName)){
            playList.remove(songName);
        }
    }
    
    
    void printPlayList(){
        for (String songName: playList.keySet()) {
            System.out.println(songName+":"+playList.get(songName));
        }
        
        //for (Map.Entry<String,String> entry: playList) {   
        //}
    }
    
    //edit: how to handle??...
    //void editName(String songName, String new)
    
}
