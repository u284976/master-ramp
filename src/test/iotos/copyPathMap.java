package test.iotos;

import java.util.HashMap;

import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.pathDescriptors.PathDescriptor;

/**
 * @author u284976
 */

public class copyPathMap{
    public synchronized static HashMap<Integer,PathDescriptor> copy(HashMap<Integer,PathDescriptor> origin){
        HashMap<Integer,PathDescriptor> copy = new HashMap<Integer,PathDescriptor>();
        for(int key : origin.keySet()){
            PathDescriptor path = new PathDescriptor(origin.get(key).getPath(), origin.get(key).getPathNodeIds());
            copy.put(key,path);
        }
        return copy;
    }
}