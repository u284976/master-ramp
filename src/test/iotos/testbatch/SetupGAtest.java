package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.PathSelectionMetric;
import it.unibo.deis.lia.ramp.core.internode.sdn.trafficEngineeringPolicy.TrafficEngineeringPolicy;

public class SetupGAtest implements SetupTestBatch {
    public String getTestBatchName(){
        return "GA_SIMPLE_TEST";
    }
    public String getTestBatchTime(){
        return "2020-06-15";
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
        return 5;
    }
    public int getNumberOfEdge(){
        return 5;
    }
    public int getTestSecond(){
        return 15;
    }
    public String getAppTarget(String nodeID){
        String targetID = "0";
        switch(nodeID){
            case "2":
                targetID = "5";
                break;
        }
        return targetID;
    }
    public int getReceive(String nodeID){
        int receive = -1;
        switch (nodeID) {
            case "5":
                receive = E2EComm.UDP;
                break;
        }
        return receive;
    }
    public ApplicationRequirements getApplicationRequirement(String nodeID){
        ApplicationRequirements applicationRequirements = null;
        TrafficType trafficType = TrafficType.CONTROL_STREAM;
        int payloadSize = 0;
        int GenPacketPerSeconds = 0;
        double requireDelay = 0.0;
        double requireThroughput = 0.0;
        int duration = 0;

        switch(nodeID){
            case "2":
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 10000;            
                GenPacketPerSeconds = 100;
                requireDelay = 50.0;
                requireThroughput = 1000000.0;
                duration = 300;
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