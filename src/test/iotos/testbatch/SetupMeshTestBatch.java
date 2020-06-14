package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;
import test.iotos.testbatch.SetupTestBatch;

public class SetupMeshTestBatch implements SetupTestBatch{
    public String getTestBatchName(){
        return "normal-mesh";
    }
    public int getNumberOfClient(){
        return 8;
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
    public boolean getReceive(String nodeID){
        boolean receive = false;
        switch (nodeID) {
            case "5":
                receive = true;
                break;
            case "2":
                receive = true;
                break;
            case "8":
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
            case "3":
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 16;
                GenPacketPerSeconds = 500;
                requireDelay = 2000.0;
                requireThroughput = 8000.0;
                duration = 100;
                break;
            case "4":
                trafficType = TrafficType.FILE_TRANSFER;
                payloadSize = 16;
                GenPacketPerSeconds = 300;
                requireDelay = 2000.0;
                requireThroughput = 8000.0;
                duration = 100;
                break;
            case "6":
                trafficType = TrafficType.VIDEO_STREAM;
                payloadSize = 20;
                GenPacketPerSeconds = 200;
                requireDelay = 2000.0;
                requireThroughput = 4000.0;
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