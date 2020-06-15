package test.iotos.testbatch;

import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;

public class SetupSimpleTest implements SetupTestBatch {
    public String getTestBatchName(){
        return "SimpleTest";
    }
    public String getTestBatchTime(){
        return "2020-06-15";
    }
    public boolean getMobility(){
        return false;
    }
    public int getNumberOfClient(){
        return 3;
    }
    public int getNumberOfEdge(){
        return 2;
    }
    public int getTestSecond(){
        return 10;
    }
    public String getAppTarget(String nodeID){
        String targetID = "0";
        if(nodeID.equals("2")){
            targetID = "3";
        }
        return targetID;
    }
    public boolean getReceive(String nodeID){
        boolean receive = false;
        if(nodeID.equals("3")){
            receive = true;
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
        if(nodeID.equals("2")){
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