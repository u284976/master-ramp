package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;

public class SetupFinalTest implements SetupTestBatch {
    public String getTestBatchName(){
        return "Final_topo_test";
    }
    public String getTestBatchTime(){
        return "2020-06-28";
    }
    public boolean getMobility(){
        return false;
    }
    public boolean getEnableFixedness(){
        return false;
    }
    public int getNumberOfClient(){
        return 11;
    }
    public int getNumberOfEdge(){
        return 16;
    }
    public int getTestSecond(){
        return 15;
    }
    public String getAppTarget(String nodeID){
        String targetID = "0";

        switch (nodeID) {
            case "6":
                targetID = "11";
                break;
            default:
                break;
        }


        return targetID;
    }
    public boolean getReceive(String nodeID){
        boolean receive = false;
        switch (nodeID) {
            case "11":
                receive = true;
                break;
        }
        return receive;
    }
    public ApplicationRequirements getApplicationRequirement(String nodeID){
        
        ApplicationRequirements applicationRequirements = null;
        TrafficType trafficType = null;
        int payloadSize = 0;
        int GenPacketPerSeconds = 0;
        double requireDelay = 0.0;
        double requireThroughput = 0.0;
        int duration = 0;

        switch (nodeID) {
            case "6":       // send to 11
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 15000;
                GenPacketPerSeconds = 10;
                requireDelay = 20.0;
                requireThroughput = 150000.0;
                duration = 100;
                break;
        }
        
        applicationRequirements = new ApplicationRequirements(
            trafficType,
            payloadSize,
            GenPacketPerSeconds,
            requireDelay,
            requireThroughput,
            duration
        );

        return applicationRequirements;
    }
}