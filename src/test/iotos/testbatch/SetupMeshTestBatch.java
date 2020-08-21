package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.PathSelectionMetric;
import it.unibo.deis.lia.ramp.core.internode.sdn.trafficEngineeringPolicy.TrafficEngineeringPolicy;

public class SetupMeshTestBatch implements SetupTestBatch{
    public String getTestBatchName(){
        return "normal-mesh";
    }
    public String getTestBatchTime(){
        return "2020-06-21";
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
        return 8;
    }
    public int getNumberOfEdge(){
        return 13;
    }
    public int getTestSecond(){
        return 15;
    }
    public String getAppTarget(String nodeID){
        String targetID = "0";
        switch (nodeID) {
            case "4":
                targetID = "5";
                break;
            case "6":
                targetID = "2";
                break;
            case "3":
                targetID = "8";
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
            case "2":
                receive = E2EComm.UDP;
                break;
            case "8":
                receive = E2EComm.UDP;
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
            case "3":       // send to 8
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 1600;
                GenPacketPerSeconds = 10;
                requireDelay = 20.0;
                requireThroughput = 16000.0;
                duration = 100;
                break;
            case "4":       // send to 5
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 1600;
                GenPacketPerSeconds = 15;
                requireDelay = 20.0;
                requireThroughput = (double)(1600*15);
                duration = 100;
                break;
            case "6":       // send to 2
                trafficType = TrafficType.VIDEO_STREAM;
                payloadSize = 800;
                GenPacketPerSeconds = 30;
                requireDelay = 10.0;
                requireThroughput = (double)(800*30);
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