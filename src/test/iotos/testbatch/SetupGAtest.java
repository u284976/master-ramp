package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;

public class SetupGAtest implements SetupTestBatch {
    public String getTestBatchName(){
        return "GA_SIMPLE_TEST";
    }
    public String getTestBatchTime(){
        return "2020-06-15";
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
    public boolean getReceive(String nodeID){
        boolean receive = false;
        switch(nodeID){
            case "5":
                receive = true;
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
                payloadSize = 54200;            // 50KByte
                GenPacketPerSeconds = 4;
                requireDelay = 2000.0;
                requireThroughput = 2000.0;
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