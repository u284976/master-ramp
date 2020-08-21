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
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerMessage.ControllerMessage;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerMessage.MessageType;
import it.unibo.deis.lia.ramp.core.internode.sdn.pathSelection.PathSelectionMetric;
import it.unibo.deis.lia.ramp.service.management.ServiceDiscovery;
import it.unibo.deis.lia.ramp.service.management.ServiceManager;
import it.unibo.deis.lia.ramp.service.management.ServiceResponse;
import test.iotos.messagetype.timeDataType;

import test.iotos.testbatch.SetupFinalTest;
import test.iotos.testbatch.SetupFinalTest2;
import test.iotos.testbatch.SetupFinalTest3;
import test.iotos.testbatch.SetupTestBatch;

public class SDNClient{

    static RampEntryPoint ramp;

    static ControllerClient controllerClient;

    static BoundReceiveSocket applicationSocket = null;

    static String TestTime;

    static String nodeID;

    static Thread listener;

    static int flowID;

    static SetupTestBatch testBatch;

    public static void main(String[] args){
        
        // change here to change testBatch
        testBatch = new SetupFinalTest3();
        TestTime = testBatch.getTestBatchTime();
        PathSelectionMetric testMetric = testBatch.getPathSelectionMetric();
        
        System.out.println("================================");
        System.out.println("SDN Client starting...");
        System.out.println("topo_version : "+ TestTime + ", testbatch = " + testBatch.getTestBatchName());
        System.out.println("================================");

        ramp = RampEntryPoint.getInstance(true, null);
        nodeID = Integer.toString(Dispatcher.getLocalRampId());

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
        controllerClient.enableMeasure();
        

        System.out.println("========================================");
        System.out.println("get controller client instance done!!!!!");
        System.out.println("========================================");
        // try {
        //     Thread.sleep(5000);
        // } catch (Exception e) {
        //     //TODO: handle exception
        // }

        /**
         * register service and waiting message by other client
         */
        int applicationProtocol = testBatch.getReceive(nodeID);
        if(applicationProtocol != -1){
            try{
                applicationSocket = E2EComm.bindPreReceive(applicationProtocol);
            }catch(Exception e){
                e.printStackTrace();
            }
            ServiceManager.getInstance(false).registerService(
                "application" + nodeID,             // serviceName
                applicationSocket.getLocalPort(),   // servicePort
                applicationProtocol                 // protocol
            );

            // start application listen
            listener = new receiveThread(TestTime,applicationSocket,nodeID);
            listener.start();
        }
        

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
            System.out.println("Waiting topo complete to get flow path");
            System.out.println("========================================");

            while (System.currentTimeMillis() < startTime) {
                try {
                    Thread.sleep(startTime - System.currentTimeMillis());
                } catch (Exception e) {
                }
            }

            if(testBatch.getMobility() == false){
                controllerClient.disableMeasure();
            }

            if(nodeID.equals("7")){
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
            }

            System.out.println("========================================");
            System.out.println("send request to SDNController!!!!!");
            System.out.println("========================================");
            flowID = controllerClient.getFlowId(
                applicationRequirements,
                destNodeID,
                destNodePort,
                testMetric
            );
            String[] path = null;
            
            if(flowID == -1){
                System.out.println("========================================");
                System.out.println("need request after a while");
                System.out.println("========================================");
                // TODO : wait few second and request new path

                ServiceResponse controllerService = controllerClient.getControllerService();

                try {
                    BoundReceiveSocket tempSocket = E2EComm.bindPreReceive(controllerService.getProtocol());

                    ControllerMessage requestMessage = new ControllerMessage(MessageType.DELAY_PATH_REQUEST,tempSocket.getLocalPort());

                    E2EComm.sendUnicast(
                        controllerService.getServerDest(),
                        controllerService.getServerNodeId(),
                        controllerService.getServerPort(),
                        controllerService.getProtocol(),
                        0,
                        E2EComm.serialize(requestMessage)
                    );

                    UnicastPacket up = (UnicastPacket)E2EComm.receive(tempSocket);
                    ControllerMessageReady payload = (ControllerMessageReady)E2EComm.deserialize(up.getBytePayload());
                    long waitToTest = payload.getStartTime();

                    System.out.println("========================================");
                    System.out.println("wait " + (waitToTest - System.currentTimeMillis()) + " to try get path");
                    System.out.println("========================================");

                    try {
                        Thread.sleep(waitToTest - System.currentTimeMillis());
                    } catch (Exception e) {
                    }

                } catch (Exception e) {
                    //TODO: handle exception
                }

                flowID = controllerClient.getFlowId(
                    applicationRequirements,
                    destNodeID,
                    destNodePort,
                    PathSelectionMetric.GENETIC_ALGO
                );

            }else{
                System.out.println("========================================");
                System.out.println("receive response!!!!!");
                path = controllerClient.getFlowPath(appService.getServerNodeId(), flowID);
                for(String s : path){
                    System.out.println(s);
                }
                System.out.println("========================================");

                
                System.out.println("========================================");
                System.out.println("Waiting new flow path to Start Traffic");
                System.out.println("========================================");

                while (System.currentTimeMillis() < startTime+10000) {
                    try {
                        Thread.sleep(startTime+10000 - System.currentTimeMillis());
                    } catch (Exception e) {
                    }
                }
                
            }
            
                      


            path = controllerClient.getFlowPath(appService.getServerNodeId(), flowID);
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
                        path,
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
            System.out.println("finally , use path:");
            String[] pathArray = controllerClient.getFlowPath(appService.getServerNodeId(), flowID);
            for(String s : pathArray){
                System.out.print(s + "  ");
            }
            System.out.println();
            System.out.println("========================================");
        }else{
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(testBatch.getMobility() == false){
                controllerClient.disableMeasure();
            }
            System.out.println("========================================");
            System.out.println("Process Done");
            System.out.println("========================================");
        }

        controllerClient.sendCompleteToController();
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
    private long currentTime;

    public packetHandler(UnicastPacket up, String outputFile){
        this.up = up;
        this.outputFile = outputFile;
        currentTime = System.currentTimeMillis();
    }

    public void run(){
        try {
            // when receive packet, true to append message to file
            CSVWriter writer = new CSVWriter(new FileWriter(outputFile, true), ','); 
            timeDataType payload = (timeDataType)E2EComm.deserialize(up.getBytePayload());

            String seqNum = Integer.toString(payload.getSeqNumber());
            String sendTime = Long.toString(payload.getSendTime());
            String currentTimeString = Long.toString(currentTime);
            String payloadSize = Integer.toString(payload.getPayloadSize());
            String delay = Long.toString(currentTime-payload.getSendTime());
            
            String[] entry = {seqNum, sendTime, currentTimeString, payloadSize, delay};
            writer.writeNext(entry);
            writer.close();
        } catch (Exception e) {
        }
    }

}