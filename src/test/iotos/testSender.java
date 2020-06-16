package test.iotos;

import it.unibo.deis.lia.ramp.RampEntryPoint;
import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.e2e.GenericPacket;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerClient.ControllerClient;
import it.unibo.deis.lia.ramp.core.internode.sdn.controllerService.ControllerService;
import it.unibo.deis.lia.ramp.service.management.ServiceDiscovery;
import it.unibo.deis.lia.ramp.service.management.ServiceManager;
import it.unibo.deis.lia.ramp.service.management.ServiceResponse;
import test.iotos.messagetype.timeDataType;


public class testSender{

    static ControllerService controllerService;

    static ControllerClient controllerClient;

	static RampEntryPoint ramp;

    public static void main(String[] args){
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
		
		try{
            Thread.sleep(5*1000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        ramp.forceNeighborsUpdate();

		ServiceResponse appService=null;
		try {
			appService = ServiceDiscovery.findServices(2, "application2", 10000, 1).elementAt(0);	
		} catch (Exception e) {
			//TODO: handle exception
		}
		for(String s : appService.getServerDest()){
			System.out.println(s);
		}
		try{
            Thread.sleep(5*1000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }

		timeDataType packet = new timeDataType();

		
		
		// for(int i=1 ; i<=50 ; i++){
		// 	System.out.println("==========send========");
		// 	System.out.println(i);
		// 	packet.setSeqNumber(i);
		// 	packet.setSendTime(System.currentTimeMillis());
		// 	try {
		// 		E2EComm.sendUnicast(
		// 			appService.getServerDest(),
		// 			appService.getServerNodeId(),
		// 			appService.getServerPort(),
		// 			appService.getProtocol(),
		// 			false,
		// 			GenericPacket.UNUSED_FIELD,
		// 			E2EComm.DEFAULT_BUFFERSIZE,
		// 			GenericPacket.UNUSED_FIELD,
		// 			GenericPacket.UNUSED_FIELD,
		// 			GenericPacket.UNUSED_FIELD,
		// 			0,
		// 			GenericPacket.UNUSED_FIELD,
		// 			E2EComm.serialize(packet)
		// 		);
		// 		Thread.sleep(1);
		// 	} catch (Exception e) {
		// 		//TODO: handle exception
		// 	}
		// }

		

		long prewhile = System.currentTimeMillis();

		int i = 0;
		long computedSleepTime = 10;
		long currentTime;
		packet.setPayloadSize(1000);
		while(System.currentTimeMillis() - prewhile <= 200){
			System.out.println("==========send========");
			System.out.println(i);
			currentTime = System.currentTimeMillis();
			packet.setSeqNumber(i);
			packet.setSendTime(currentTime);
			try {
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
					0,
					GenericPacket.UNUSED_FIELD,
					E2EComm.serialize(packet)
				);
				long sleepTime = (computedSleepTime * i) - (currentTime - prewhile);
				if (sleepTime > 0) {
						Thread.sleep(sleepTime);
				}
				i++;
			} catch (Exception e) {
				//TODO: handle exception
			}
		}
		packet.setPayloadSize(2000);
		while(System.currentTimeMillis() - prewhile <= 400){
			System.out.println("==========send========");
			System.out.println(i);
			currentTime = System.currentTimeMillis();
			packet.setSeqNumber(i);
			packet.setSendTime(currentTime);
			try {
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
					0,
					GenericPacket.UNUSED_FIELD,
					E2EComm.serialize(packet)
				);
				long sleepTime = (computedSleepTime * i) - (currentTime - prewhile);
				if (sleepTime > 0) {
						Thread.sleep(sleepTime);
				}
				i++;
			} catch (Exception e) {
				//TODO: handle exception
			}
		}


		packet.setPayloadSize(5000);
		while(System.currentTimeMillis() - prewhile <= 600){
			System.out.println("==========send========");
			System.out.println(i);
			currentTime = System.currentTimeMillis();
			packet.setSeqNumber(i);
			packet.setSendTime(currentTime);
			try {
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
					0,
					GenericPacket.UNUSED_FIELD,
					E2EComm.serialize(packet)
				);
				long sleepTime = (computedSleepTime * i) - (currentTime - prewhile);
				if (sleepTime > 0) {
						Thread.sleep(sleepTime);
				}
				i++;
			} catch (Exception e) {
				//TODO: handle exception
			}
		}

		packet.setPayloadSize(10000);
		while(System.currentTimeMillis() - prewhile <= 800){
			System.out.println("==========send========");
			System.out.println(i);
			currentTime = System.currentTimeMillis();
			packet.setSeqNumber(i);
			packet.setSendTime(currentTime);
			try {
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
					0,
					GenericPacket.UNUSED_FIELD,
					E2EComm.serialize(packet)
				);
				long sleepTime = (computedSleepTime * i) - (currentTime - prewhile);
				if (sleepTime > 0) {
						Thread.sleep(sleepTime);
				}
				i++;
			} catch (Exception e) {
				//TODO: handle exception
			}
		}


		packet.setPayloadSize(50000);
		while(System.currentTimeMillis() - prewhile <= 1000){
			System.out.println("==========send========");
			System.out.println(i);
			currentTime = System.currentTimeMillis();
			packet.setSeqNumber(i);
			packet.setSendTime(currentTime);
			try {
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
					0,
					GenericPacket.UNUSED_FIELD,
					E2EComm.serialize(packet)
				);
				long sleepTime = (computedSleepTime * i) - (currentTime - prewhile);
				if (sleepTime > 0) {
						Thread.sleep(sleepTime);
				}
				i++;
			} catch (Exception e) {
				//TODO: handle exception
			}
		}
		
    }
}