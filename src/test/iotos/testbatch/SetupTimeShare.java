package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.PathSelectionMetric;
import it.unibo.deis.lia.ramp.core.internode.sdn.trafficEngineeringPolicy.TrafficEngineeringPolicy;

/**
 * use topo : link_for_time_share.py
 */
public class SetupTimeShare implements SetupTestBatch {

    public String getTestBatchName(){
        return "time share";
    }
    public String getTestBatchTime(){
        return "2020-06-30";
    }
    public boolean getMobility(){
        return false;
    }
    public boolean getEnableFixedness(){
        return false;
    }
    public PathSelectionMetric getPathSelectionMetric(){
        return PathSelectionMetric.GENETIC_ALGO;
    }
    public TrafficEngineeringPolicy getTrafficEngineeringPolicy(){
        return TrafficEngineeringPolicy.NO_FLOW_POLICY;
    }
    public int getNumberOfClient(){
        return 4;
    }
    public int getNumberOfEdge(){
        return 3;
    }
    public int getTestSecond(){
        return 15;
    }
    public String getAppTarget(String nodeID){
        String targetID = "0";
        switch (nodeID) {
            case "4":
                targetID = "1";
                break;
        }
        return targetID;
    }
    public boolean getReceive(String nodeID){
        return false;
    }
    public ApplicationRequirements getApplicationRequirement(String nodeID){
        ApplicationRequirements applicationRequirements = null;
        TrafficType trafficType = TrafficType.CONTROL_STREAM;
        int payloadSize = 0;
        int GenPacketPerSeconds = 0;
        double requireDelay = 0.0;
        double requireThroughput = 0.0;
        int duration = 0;
        if(nodeID.equals("4")){
            trafficType = TrafficType.FILE_TRANSFER;
            payloadSize = 16;
            GenPacketPerSeconds = 3;
            requireDelay = 2000.0;
            requireThroughput = 8000.0;
            duration = 300;
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