package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;

/**
 * use topo : final_topo.py
 */
public class SetupTimeShare2 implements SetupTestBatch {
    public String getTestBatchName(){
        return "Time_Share_2";
    }
    public String getTestBatchTime(){
        return "2020-06-30";
    }
    public boolean getMobility(){
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
            case "2":
                targetID = "11";
                break;
            case "3":
                targetID = "11";
                break;
            case "5":
                targetID = "10";
                break;
            case "6":
                targetID = "10";
                break;
        }


        return targetID;
    }
    public boolean getReceive(String nodeID){
        boolean receive = false;
        switch (nodeID) {
            case "10":
                receive = true;
                break;
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
            case "2":       // send to 11
                trafficType = TrafficType.VIDEO_STREAM;
                payloadSize = 15000;
                GenPacketPerSeconds = 10;
                requireDelay = 20.0;
                requireThroughput = 150000.0;
                duration = 100;
                break;
            case "3":       // send to 11
                trafficType = TrafficType.VIDEO_STREAM;
                payloadSize = 15000;
                GenPacketPerSeconds = 10;
                requireDelay = 20.0;
                requireThroughput = 150000.0;
                duration = 100;
                break;
            case "5":       // send to 10
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 15000;
                GenPacketPerSeconds = 10;
                requireDelay = 40.0;
                requireThroughput = 150000.0;
                duration = 100;
                break;
            case "6":       // send to 10
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 15000;
                GenPacketPerSeconds = 10;
                requireDelay = 40.0;
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