package test.iotos;

import java.util.HashMap;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;



/**
 * @author u284976
 */

public class copyARMap{
    public HashMap<Integer,ApplicationRequirements> copy(HashMap<Integer,ApplicationRequirements> origin){
        HashMap<Integer,ApplicationRequirements> copy = new HashMap<Integer,ApplicationRequirements>();
        for(int key : origin.keySet()){
            ApplicationRequirements AR = new ApplicationRequirements(
                origin.get(key).getTrafficType(),
                origin.get(key).getPakcetLength(),
                origin.get(key).getPacketRate(),
                origin.get(key).getRequireDelay(),
                origin.get(key).getRequireThroughput(),
                origin.get(key).getDuration()
            );
            copy.put(key, AR);
        }
        return copy;
    }
}