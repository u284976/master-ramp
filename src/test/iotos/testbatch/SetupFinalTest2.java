package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.PathSelectionMetric;
import it.unibo.deis.lia.ramp.core.internode.sdn.trafficEngineeringPolicy.TrafficEngineeringPolicy;

/**
 * use topo : final_topo.py
 */
public class SetupFinalTest2 implements SetupTestBatch {
    public String getTestBatchName(){
        return "Final_Test2";
    }
    public String getTestBatchTime(){
        return "2020-08-05";
    }
    public boolean getMobility(){
        return false;
    }
    public boolean getEnableFixedness(){
        return false;
        // return true;
    }
    public PathSelectionMetric getPathSelectionMetric(){
        // return PathSelectionMetric.GENETIC_ALGO;
        return PathSelectionMetric.BREADTH_FIRST;
    }
    public TrafficEngineeringPolicy getTrafficEngineeringPolicy(){
        return TrafficEngineeringPolicy.NO_FLOW_POLICY;
        // return TrafficEngineeringPolicy.TRAFFIC_SHAPING;
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
                targetID = "10";
                break;
            case "5":
                targetID = "11";
                break;
            case "6":
                targetID = "11";
                break;
            case "7":
                targetID = "10";
                break;
        }
        
        return targetID;
    }
    public int getReceive(String nodeID){
        int receive = -1;
        switch (nodeID) {
            case "10":
                receive = E2EComm.UDP;
                break;
            case "11":
                receive = E2EComm.TCP;
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
            case "2":       // send to 10
                trafficType = TrafficType.VIDEO_STREAM;
                payloadSize = 50002;
                GenPacketPerSeconds = 10;
                requireDelay = 40.0;
                requireThroughput = 0.0;
                duration = 25;
                break;
            case "5":       // send to 11
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 50005;
                GenPacketPerSeconds = 10;
                requireDelay = 0.0;
                requireThroughput = 500050.0;
                duration = 25;
                break;
            case "6":       // send to 11
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 50006;
                GenPacketPerSeconds = 10;
                requireDelay  = 0.0;
                requireThroughput = 500060.0;
                duration = 25;
                break;
            case "7":       // send to 10
                trafficType = TrafficType.VIDEO_STREAM;
                payloadSize = 50007;
                GenPacketPerSeconds = 10;
                requireDelay = 40.0;
                requireThroughput = 0.0;
                duration = 25;
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