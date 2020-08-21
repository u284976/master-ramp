package test.iotos;

import it.unibo.deis.lia.ramp.RampEntryPoint;
import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.internode.Dispatcher;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerClient.ControllerClient;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerService.ControllerService;
import it.unibo.deis.lia.ramp.service.management.ServiceManager;
import test.iotos.testbatch.SetupFinalTest;
import test.iotos.testbatch.SetupFinalTest2;
import test.iotos.testbatch.SetupFinalTest3;
import test.iotos.testbatch.SetupTestBatch;

public class SDNController{
    
    static ControllerService controllerService;

    static ControllerClient controllerClient;

	static RampEntryPoint ramp;
	
	static int countClient;
	
	static int countEdge;

	static SetupTestBatch testBatch;

	static String testBatchName;

	static String testTime;


    public static void main(String[] args){
		
		// change here to change testBatch
		testBatch = new SetupFinalTest3();
		
		testBatchName = testBatch.getTestBatchName();
		testTime = testBatch.getTestBatchTime();

		countClient = testBatch.getNumberOfClient();
		countEdge = testBatch.getNumberOfEdge();

        System.out.println("================================");
        System.out.println("SDN Controller starting...");
        System.out.println("topo_version : " + testTime + ", testbatch = " + testBatchName);
        System.out.println("================================");

        ramp = RampEntryPoint.getInstance(true, null);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (ramp != null && controllerService != null) {
						System.out.println("ShutdownHook is being executed: gracefully stopping RAMP...");
						ServiceManager.getInstance(false).removeService("application1");
						controllerService.stopService();
						ramp.stopRamp();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
        }));
        
        controllerService = ControllerService.getInstance();
		controllerService.setCountClient(countClient);
		controllerService.setMobility(testBatch.getMobility());
		controllerService.setEnableFixedness(testBatch.getEnableFixedness());
		controllerService.updateTrafficEngineeringPolicy(testBatch.getTrafficEngineeringPolicy());

        try {
			Thread.sleep(5*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		controllerClient = ControllerClient.getInstance();
		// controllerClient.enableMeasure();
		
		/**
         * register service and waiting message by other client
         */
        BoundReceiveSocket applicationSocket = null;
        try{
            applicationSocket = E2EComm.bindPreReceive(E2EComm.UDP);
        }catch(Exception e){
            e.printStackTrace();
        }
        String nodeID = Integer.toString(Dispatcher.getLocalRampId());
        ServiceManager.getInstance(false).registerService(
            "application" + nodeID,             // serviceName
            applicationSocket.getLocalPort(),   // servicePort
            E2EComm.UDP                         // protocol
        );

        System.out.println("========================================");
        System.out.println("register Service to local management done!!");
        System.out.println("========================================");

		controllerService.displayGraph();

		while(true){
			try {
				Thread.sleep(10*1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(controllerService.getActiveClients().size() >= countClient){
				if(controllerService.checkTopoComplete(countEdge)){
					break;
				}
			}
		}
		System.out.println("================================");
		System.out.println("controller notice to all client");
		System.out.println("================================");

		if(testBatch.getMobility() == false){
			controllerClient.disableMeasure();
		}
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		

		while (!controllerService.checkComplete()) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("================================");
		System.out.println("Transfer complete");
		System.out.println("================================");


		// TODO: analyze data in ~/ramp/0-outputFile/*
	}
}