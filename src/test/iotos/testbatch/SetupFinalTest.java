package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.PathSelectionMetric;
import it.unibo.deis.lia.ramp.core.internode.sdn.trafficEngineeringPolicy.TrafficEngineeringPolicy;

/**
 * use topo : final_topo.py
 */
public class SetupFinalTest implements SetupTestBatch {
    public String getTestBatchName(){
        return "Final_Test";
    }
    public String getTestBatchTime(){
        return "2020-07-06";
    }
    public boolean getMobility(){
        return false;
    }
    public boolean getEnableFixedness(){
        return false;
    }
    public PathSelectionMetric getPathSelectionMetric(){
        // return PathSelectionMetric.GENETIC_ALGO;
        return PathSelectionMetric.BREADTH_FIRST;
    }
    public TrafficEngineeringPolicy getTrafficEngineeringPolicy(){
        // return TrafficEngineeringPolicy.NO_FLOW_POLICY;
        return TrafficEngineeringPolicy.TRAFFIC_SHAPING;
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
            // case "2":
            //     targetID = "4";
            //     break;
            case "3":
                targetID = "10";
                break;
            case "4":
                targetID = "11";
                break;
            case "5":
                targetID = "9";
                break;
            // case "6":
            //     targetID = "3";
            //     break;
            case "7":
                targetID = "11";
                break;
            case "8":
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
            // case "3":
            //     receive = true;
            //     break;
            // case "4":
            //     receive = true;
            //     break;
            case "9":
                receive = true;
                break;
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
            // case "2":       // send to 4
            //     trafficType = TrafficType.VIDEO_STREAM;
            //     payloadSize = 15002;
            //     GenPacketPerSeconds = 10;
            //     requireDelay = 20.0;
            //     requireThroughput = 150000.0;
            //     duration = 35;
            //     break;
            case "3":       // send to 10
                trafficType = TrafficType.VIDEO_STREAM;
                payloadSize = 40003;
                GenPacketPerSeconds = 10;
                requireDelay = 20.0;
                requireThroughput = 400030.0;
                duration = 35;
                break;
            case "4":       // send to 11
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 40004;
                GenPacketPerSeconds = 10;
                requireDelay = 40.0;
                requireThroughput = 400040.0;
                duration = 35;
                break;
            case "5":       // send to 9
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 40005;
                GenPacketPerSeconds = 10;
                requireDelay = 40.0;
                requireThroughput = 400050.0;
                duration = 35;
                break;
            // case "6":       // send to 3
            //     trafficType = TrafficType.VIDEO_STREAM;
            //     payloadSize = 15006;
            //     GenPacketPerSeconds = 10;
            //     requireDelay = 20.0;
            //     requireThroughput = 150000.0;
            //     duration = 35;
            //     break;
            case "7":       // send to 11
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 40007;
                GenPacketPerSeconds = 10;
                requireDelay = 40.0;
                requireThroughput = 400070.0;
                duration = 35;
                break;
            case "8":       // send to 2
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 40008;
                GenPacketPerSeconds = 10;
                requireDelay = 40.0;
                requireThroughput = 400080.0;
                duration = 35;
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