package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.PathSelectionMetric;
import it.unibo.deis.lia.ramp.core.internode.sdn.trafficEngineeringPolicy.TrafficEngineeringPolicy;

public class SetupMinaTestBatch implements SetupTestBatch{
    public String getTestBatchName() {
        return "mina-topo";
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
        return 29;
    }
    public int getNumberOfEdge(){
        return 0;
    }
    public int getTestSecond(){
        return 30;
    }
    public int getReceive(String nodeID){
        int receive = -1;
        switch (nodeID) {
            case "1":
                receive = E2EComm.UDP;
                break;
            case "11":
                receive = E2EComm.UDP;
                break;
            case "13":
                receive = E2EComm.UDP;
                break;
            case "14":
                receive = E2EComm.UDP;
                break;
            case "10":
                receive = E2EComm.UDP;
                break;
            case "26":
                receive = E2EComm.UDP;
                break;
        }
        return receive;
    }
    public String getAppTarget(String nodeID){
        String targetID = "0";
        switch (nodeID) {
            case "25":
                targetID = "1";
                break;
            case "26":
                targetID = "1";
                break;
            case "19":
                targetID = "11";
                break;
            case "21":
                targetID = "13";
                break;
            case "10":
                targetID = "1";
                break;
            case "24":
                targetID = "14";
                break;
            case "23":
                targetID = "10";
                break;
            case "16":
                targetID = "26";
                break;
        }
        return targetID;
    }

    public ApplicationRequirements getApplicationRequirement(String nodeID){
        ApplicationRequirements applicationRequirements = null;
        TrafficType trafficType = TrafficType.CONTROL_STREAM;
        int payloadSize = 0;
        int GenPacketPerSeconds = 0;
        double requireDelay = 0.0;
        double requireThroughput = 0.0;
        int duration = 0;

        switch (nodeID) {
            case "25":
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 16;
                GenPacketPerSeconds = 500;
                requireDelay = 2000.0;
                requireThroughput = 8000.0;
                duration = 300;
                break;
            case "26":
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 20;
                GenPacketPerSeconds = 400;
                requireDelay = 2000.0;
                requireThroughput = 8000.0;
                duration = 300;
                break;
            case "19":
                    trafficType = TrafficType.FILE_TRANSFER;
                    payloadSize = 16;
                    GenPacketPerSeconds = 500;
                    requireDelay = 2000.0;
                    requireThroughput = 8000.0;
                    duration = 300;
                break;
            case "21":
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 16;
                GenPacketPerSeconds = 300;
                requireDelay = 2000.0;
                requireThroughput = 8000.0;
                duration = 300;
                break;
            case "10":
                trafficType = TrafficType.VIDEO_STREAM;
                payloadSize = 16;
                GenPacketPerSeconds = 300;
                requireDelay = 2000.0;
                requireThroughput = 4800.0;
                duration = 300;
                break;
            case "24":
                trafficType = TrafficType.VIDEO_STREAM;
                payloadSize = 20;
                GenPacketPerSeconds = 200;
                requireDelay = 2000.0;
                requireThroughput = 4000.0;
                duration = 300;
                break;
            case "23":
                trafficType = TrafficType.VIDEO_STREAM;
                payloadSize = 20;
                GenPacketPerSeconds = 200;
                requireDelay = 2000.0;
                requireThroughput = 4000.0;
                duration = 300;
                break;
            case "16":
                trafficType = TrafficType.VIDEO_STREAM;
                payloadSize = 8;
                GenPacketPerSeconds = 300;
                requireDelay = 2000.0;
                requireThroughput = 1600.0;
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