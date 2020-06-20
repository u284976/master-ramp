package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;

public class SetupGA_Change_Test implements SetupTestBatch{
    public String getTestBatchName(){
        return "GA_Change_Test";
    }
    public String getTestBatchTime(){
        return "2020-06-21";
    }
    public boolean getMobility(){
        return false;
    }
    public int getNumberOfClient(){
        return 7;
    }
    public int getNumberOfEdge(){
        return 8;
    }
    public int getTestSecond(){
        return 15;
    }
    public String getAppTarget(String nodeID){
        String targetID = "0";
        switch (nodeID) {
            case "2":
                targetID = "6";
                break;
            case "6":
                targetID = "2";
                break;
            case "7":
                targetID = "2";
                break;
        }
        return targetID;
    }
    public boolean getReceive(String nodeID){
        boolean receive = false;
        switch (nodeID) {
            case "2":
                receive = true;
                break;
            case "6":
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
            case "2":       // send to 6
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 1600;
                GenPacketPerSeconds = 10;
                requireDelay = 20.0;
                requireThroughput = 16000.0;
                duration = 100;
                break;
            case "6":       // send to 2
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 1600;
                GenPacketPerSeconds = 15;
                requireDelay = 20.0;
                requireThroughput = 24000.0;
                duration = 100;
                break;
            case "7":       // send to 2
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 800;
                GenPacketPerSeconds = 30;
                requireDelay = 10.0;
                requireThroughput = 24000.0;
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