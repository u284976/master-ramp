package test.iotos;

import java.util.HashMap;
import java.util.Vector;

import it.unibo.deis.lia.ramp.RampEntryPoint;
import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.service.management.ServiceDiscovery;
import it.unibo.deis.lia.ramp.service.management.ServiceManager;
import it.unibo.deis.lia.ramp.service.management.ServiceResponse;

/**
 * @author u284976
 */

public class CollectNetworkInfoClient{
    private static RampEntryPoint ramp;
    private static BoundReceiveSocket clientSocket;

    /**
     * nodeID (Integer)
     *    |
     *    |-single hop IP address (String)
     *              |
     *              |-ServiceResponse
     */
    private static HashMap<Integer,HashMap<String, ServiceResponse>> availableSingleHops;

    public static void main(String[] args){
        ramp = RampEntryPoint.getInstance(true, null);

        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ramp.forceNeighborsUpdate();

        try{
            clientSocket = E2EComm.bindPreReceive(E2EComm.TCP);
        } catch (Exception e){
            System.out.println("bind Pre Receive socket failed!");
        }
        

        ServiceManager.getInstance(false).registerService(
            "AssistantClientListenr",       // serviceName
            clientSocket.getLocalPort(),    // servicePort
            E2EComm.TCP                     // protocol
        );

        Vector<ServiceResponse> services = null;
        while(true){
            try{
                services = ServiceDiscovery.findServices(
                    1,                          // TTL
                    "AssistantClientListenr",   // serviceName
                    2000,                       // timeout
                    30                          // serviceAmount
                );
            } catch(Exception e){
                e.printStackTrace();
            }

            availableSingleHops = new HashMap<>();
            
            for (ServiceResponse service : services) {

                Integer singleHopID = service.getServerNodeId();
                String[] paths = service.getServerDest();
                String path = paths[0];

                // old singleHop ID
                if(singleHopID != ramp.getNodeId() && availableSingleHops.containsKey(singleHopID)){
                    // new IP adress
                    if(!availableSingleHops.get(singleHopID).containsKey(path)){
                        availableSingleHops.get(singleHopID).put(path, service);
                    }
                }
                // nodeID
                else if(singleHopID != ramp.getNodeId()){
                    HashMap<String, ServiceResponse> newSingleHopAddress = new HashMap<>();
                    newSingleHopAddress.put(path, service);
                    availableSingleHops.put(singleHopID,newSingleHopAddress);
                }

            }

            System.out.println();
            System.out.println("===============");
            for(Integer nodeID:availableSingleHops.keySet()){
                System.out.println(nodeID);
                for(String path : availableSingleHops.get(nodeID).keySet()){
                    System.out.println(path);
                }
            }
            try {
                Thread.sleep(10*1000);    
            } catch (Exception e) {
            }
            
        }
        

    }
}