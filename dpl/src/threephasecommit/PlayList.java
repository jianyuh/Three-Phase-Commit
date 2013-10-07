package threephasecommit;


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
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
    public void delete(String songName, String URL){
        if(playList.containsKey(songName)){
            playList.remove(songName);
        }
    }
    
    public void edit(String songName, String URL) {
        if(playList.contains(songName)) {
            playList.remove(songName);
            playList.put(songName, URL);
        }
    }
    
    public String toStringPlayList() {
          StringBuilder sb = new StringBuilder();
          String splittag = "";
        for (String songName: playList.keySet()) {
            sb.append(songName+":"+playList.get(songName)+splittag);
            splittag = "\n";
        }
        return sb.toString();
    }
    
    public void extractPlayList(String str) {
        String [] hashpairs = str.split("\n");
        for (String hashpair: hashpairs) {
            String keyvalue [] = hashpair.split(":");
            playList.put(keyvalue[0], keyvalue[1]);
        }
    }
    
    
    public void printPlayList(){
        for (String songName: playList.keySet()) {
            System.out.println(songName+":"+playList.get(songName));
        }
        
        //for (Map.Entry<String,String> entry: playList) {   
        //}
    }
    
    //edit: how to handle??...
    //void editName(String songName, String new)
    
}
