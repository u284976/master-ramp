package test.iotos;

import java.io.File;
import java.io.FileWriter;

import com.opencsv.CSVWriter;

import it.unibo.deis.lia.ramp.RampEntryPoint;
import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.e2e.GenericPacket;
import it.unibo.deis.lia.ramp.core.e2e.UnicastPacket;
import it.unibo.deis.lia.ramp.core.internode.Dispatcher;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.ApplicationRequirements;
import it.unibo.deis.lia.ramp.core.internode.sdn.applicationRequirements.TrafficType;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerClient.ControllerClient;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.PathSelectionMetric;
import it.unibo.deis.lia.ramp.service.management.ServiceDiscovery;
import it.unibo.deis.lia.ramp.service.management.ServiceManager;
import it.unibo.deis.lia.ramp.service.management.ServiceResponse;
import test.iotos.messagetype.timeDataType;
import test.iotos.testbatch.SetupMeshTestBatch;
import test.iotos.testbatch.SetupMinaTestBatch;
import test.iotos.testbatch.SetupSimpleTest;
import test.iotos.testbatch.SetupTestBatch;

public class SDNClient{

    static RampEntryPoint ramp;

    static ControllerClient controllerClient;

    static BoundReceiveSocket applicationSocket = null;

    static String TestTime;

    static String nodeID;

    static Thread listener;

    static SetupTestBatch testBatch;

    public static void main(String[] args){

        
        TestTime = "2020-06-14";
        // change here to change testBatch
        testBatch = new SetupMeshTestBatch();
        
        System.out.println("================================");
        System.out.println("SDN Client starting...");
        System.out.println("version : "+ TestTime + ", testbatch = " + testBatch.getTestBatchName());
        System.out.println("================================");

        ramp = RampEntryPoint.getInstance(true, null);

        /**
         * wait a few second to allow the node to discover 
         * neighbor
         * ex. init. heartbeater
         */
        try{
            Thread.sleep(5*1000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        ramp.forceNeighborsUpdate();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (ramp != null && controllerClient != null) {
                        System.out.println("ShutdownHook is being executed: gracefully stopping RAMP...");
                        ServiceManager.getInstance(false).removeService("application" + nodeID);
                        listener.stop();
                        controllerClient.stopClient();
                        ramp.stopRamp();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }));

        controllerClient = ControllerClient.getInstance();

        System.out.println("========================================");
        System.out.println("get controller client instance done!!!!!");
        System.out.println("========================================");
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            //TODO: handle exception
        }

        /**
         * register service and waiting message by other client
         */
        try{
            applicationSocket = E2EComm.bindPreReceive(E2EComm.UDP);
        }catch(Exception e){
            e.printStackTrace();
        }
        nodeID = Integer.toString(Dispatcher.getLocalRampId());
        ServiceManager.getInstance(false).registerService(
            "application" + nodeID,             // serviceName
            applicationSocket.getLocalPort(),   // servicePort
            E2EComm.UDP                         // protocol
        );

        System.out.println("========================================");
        System.out.println("register Service to local management done!!");
        System.out.println("========================================");
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
            //TODO: handle exception
        }

        System.out.println("========================================");
        System.out.println("Waiting controller notice Start Test");
        System.out.println("========================================");
        long startTime = 0;
        while (true) {
            startTime = controllerClient.getReadyToTest();
            if(startTime != 0){
                break;
            }

            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                //TODO: handle exception
            }
        }

        // start listen
        boolean IamReceive = testBatch.getReceive(nodeID);
        if(IamReceive){
            listener = new receiveThread(TestTime,applicationSocket,nodeID);
            listener.start();
        }
        

        ServiceResponse appService = null;
        String targetID = testBatch.getAppTarget(nodeID);

        /**
         * if no match anyone will return 0 , which means this node not sender
         */
        if(!targetID.equals("0")){
            // find target
            try {
                appService = ServiceDiscovery.findServices(
                    5,
                    "application" + targetID,
                    3000,
                    1
                ).elementAt(0);
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("========================================");
            System.out.println("find Service done!!!");
            if(appService != null){
                for(String s : appService.getServerDest()){
                    System.out.println(s);
                }
            }
            System.out.println("========================================");

            // setup parameter of traffic
            ApplicationRequirements applicationRequirements = testBatch.getApplicationRequirement(nodeID);
            int payloadSize = applicationRequirements.getPakcetLength();
            int GenPacketPerSeconds = applicationRequirements.getPacketRate();
            int[] destNodeID = {appService.getServerNodeId()};
            int[] destNodePort = {appService.getServerPort()};
    
            System.out.println("========================================");
            System.out.println("Waiting Start");
            System.out.println("========================================");

            while (System.currentTimeMillis() < startTime) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
    
            System.out.println("========================================");
            System.out.println("send request to SDNController!!!!!");
            System.out.println("========================================");
            int flowID = controllerClient.getFlowId(
                applicationRequirements,
                destNodeID,
                destNodePort,
                PathSelectionMetric.GENETIC_ALGO
            );
            System.out.println("========================================");
            System.out.println("receive response!!!!!");
            String[] path = controllerClient.getFlowPath(appService.getServerNodeId(), flowID);
            for(String s : path){
                System.out.println(s);
            }
            System.out.println("========================================");
            



            System.out.println("========================================");
            System.out.println("Start transfer!!!!!");
            System.out.println("========================================");
    
            try {
                // record packet send time
                // String outputFile = "node_" + nodeID + "_send_" + TestTime + ".csv";
                // CSVWriter writer = new CSVWriter(new FileWriter(outputFile, false), ',');
    
                // setup packet 
                timeDataType packet = new timeDataType();

                packet.setPayloadSize(payloadSize);

                // setup sendPacket per seconds
                long computedSleepTime = (long) Math.ceil(1000/GenPacketPerSeconds);
                long preWhile = System.currentTimeMillis();
                int seqNum = 0;

                // start transfer execute "testSecond"
                int testSecond = testBatch.getTestSecond();
                while(System.currentTimeMillis() - preWhile <= testSecond*1000){

                    long currentTime = System.currentTimeMillis();
                    seqNum++;
                    packet.setSeqNumber(seqNum);
                    packet.setSendTime(currentTime);
    
                    E2EComm.sendUnicast(
                        appService.getServerDest(),
                        appService.getServerNodeId(),
                        appService.getServerPort(),
                        appService.getProtocol(),
                        false,
                        GenericPacket.UNUSED_FIELD,
                        E2EComm.DEFAULT_BUFFERSIZE,
                        GenericPacket.UNUSED_FIELD,
                        GenericPacket.UNUSED_FIELD,
                        GenericPacket.UNUSED_FIELD,
                        flowID,
                        GenericPacket.UNUSED_FIELD,
                        E2EComm.serialize(packet)
                    );
                    
                    // String[] entry = {Integer.toString(seqNum),Long.toString(currentTime)};
                    // writer.writeNext(entry);
    
                    long sleepTime = (computedSleepTime * seqNum) - (currentTime - preWhile);
                    if (sleepTime > 0) {
                            Thread.sleep(sleepTime);
                    }
                }
                // writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            System.out.println("========================================");
            System.out.println("Transfer Done!!!!!");
            System.out.println("========================================");
        }else{
            System.out.println("========================================");
            System.out.println("Process Done");
            System.out.println("========================================");
        }
    }
}


class receiveThread extends Thread{

    private String TestTime;

    public Boolean appActive;

    private String nodeID;

    BoundReceiveSocket applicationSocket;

    public receiveThread(String TestTime, BoundReceiveSocket applicationSocket, String nodeID){
        this.TestTime = TestTime;
        this.applicationSocket = applicationSocket;
        this.nodeID = nodeID;
        appActive = true;
    }

    public void terminal(){
        appActive = false;
    }

    @Override
    public void run(){
        String outputFile = "../0-outputFile/" + TestTime + "receive_on_" + nodeID + ".csv";
        try {
            // init file , false to not append file
            CSVWriter writer = new CSVWriter(new FileWriter(outputFile, false), ','); 
            String[] entry1 = {TestTime,"nodeID="+nodeID};
            writer.writeNext(entry1);
            String[] entry2 = {"seq","sendTime","receiveTime","payloadSize"};
            writer.writeNext(entry2);
            writer.close();
            while (appActive) {
                    UnicastPacket up = (UnicastPacket)E2EComm.receive(applicationSocket);
                    new packetHandler(up, outputFile).start();
            }
            
        } catch (Exception e) {
        }
    }
}

class packetHandler extends Thread{

    private UnicastPacket up;
    private String outputFile;

    public packetHandler(UnicastPacket up, String outputFile){
        this.up = up;
        this.outputFile = outputFile;
    }

    public void run(){
        try {
            // when receive packet, true to append message to file
            CSVWriter writer = new CSVWriter(new FileWriter(outputFile, true), ','); 
            timeDataType payload = (timeDataType)E2EComm.deserialize(up.getBytePayload());

            String sendTime = Long.toString(payload.getSendTime());
            String currentTime = Long.toString(System.currentTimeMillis());
            String seqNum = Integer.toString(payload.getSeqNumber());
            String payloadSize = Integer.toString(payload.getPayloadSize());
            
            String[] entry = {seqNum, sendTime, currentTime, payloadSize};
            writer.writeNext(entry);
            writer.close();
        } catch (Exception e) {
        }
    }

}