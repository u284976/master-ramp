package test.iotos;

import java.util.HashMap;

import it.unibo.deis.lia.ramp.RampEntryPoint;
import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.service.management.ServiceManager;
import it.unibo.deis.lia.ramp.service.management.ServiceResponse;

/**
 * @author u284976
 */


public class CollectNetworkInfoService{
    private static RampEntryPoint ramp;
    private static BoundReceiveSocket socket;

    /**
     * nodeID
     *    |
     *    |-neighborID
     *          |
     *          |-(address,service)
     */
    private static HashMap<Integer, HashMap<Integer,HashMap<String,ServiceResponse>>> currentRelation;

    

    public static void main(String[] args){
        ramp = RampEntryPoint.getInstance(true, null);

        try{
            Thread.sleep(5000);
        } catch (Exception e){
            e.printStackTrace();
        }

        ramp.forceNeighborsUpdate();

        try {
            socket = E2EComm.bindPreReceive(E2EComm.TCP);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ServiceManager.getInstance(false).registerService(
            "AssistantService",     // serviceName
            socket.getLocalPort(),  // servicePort
            E2EComm.TCP             // protocol
        );
        



        while(true){
            
        }

    }
}