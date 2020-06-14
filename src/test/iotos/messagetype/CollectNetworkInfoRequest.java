package test.iotos.messagetype;

import java.io.Serializable;
import java.util.HashMap;

import it.unibo.deis.lia.ramp.service.management.ServiceResponse;

/**
 * @author u284976
 */

public class CollectNetworkInfoRequest implements Serializable{
    private static final long serialVersionUID = 1;

    private HashMap<Integer,HashMap<String,ServiceResponse>> availableSingleHops;

    
    public void setNeighbor(HashMap<Integer,HashMap<String,ServiceResponse>> availableSingleHops){
        this.availableSingleHops = availableSingleHops;
    }
    public HashMap<Integer,HashMap<String,ServiceResponse>> getNeighbor(){
        return availableSingleHops;
    }
}