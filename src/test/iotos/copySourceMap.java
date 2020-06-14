package test.iotos;

import java.util.HashMap;

/**
 * @author u284976
 */

public class copySourceMap{
    public HashMap<Integer,Integer> copy(HashMap<Integer,Integer> origin){
        HashMap<Integer,Integer> copy = new HashMap<Integer,Integer>();
        for(int key : origin.keySet()){
            Integer SourceID = origin.get(key);
            copy.put(key, SourceID);
        }
        return copy;
    }
}