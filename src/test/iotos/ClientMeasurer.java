package test.iotos;

import java.io.File;
import java.io.FileInputStream;

import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.e2e.GenericPacket;
import it.unibo.deis.lia.ramp.core.e2e.UnicastPacket;
import it.unibo.deis.lia.ramp.core.internode.Dispatcher;
import it.unibo.deis.lia.ramp.service.management.ServiceManager;
import test.iotos.messagetype.MeasureMessage;

public class ClientMeasurer extends Thread{

    private static ClientMeasurer clientMeasurer;

    private BoundReceiveSocket service;

    private static boolean active = true;
    private static String rampID;
    private static boolean occupied;

    public static ClientMeasurer getInstance(){
        if(clientMeasurer == null){
            clientMeasurer = new ClientMeasurer();
        }

        clientMeasurer.start();
        return clientMeasurer;
    }

    private ClientMeasurer(){

        rampID = Dispatcher.getLocalRampIdString();
        try {
            service = E2EComm.bindPreReceive(E2EComm.UDP);    
        } catch (Exception e) {
            // e.printStackTrace();
        }
        ServiceManager.getInstance(false).registerService(
            "measure_" + rampID,
            service.getLocalPort(),
            E2EComm.UDP
        );
        occupied = false;
    }

    public synchronized boolean tryOccupy(){
        if(occupied == true){
            return false;
        }else{
            occupied = true;
            return true;
        }
    }
    public void releaseOccupy(){
        occupied = false;
    }

    public void stopMeasure(){
        active = false;
        try {
            ServiceManager.getInstance(false).removeService("measure_" + rampID);
            service.close();
            clientMeasurer = null;    
        } catch (Exception e) {
        }
    }

    @Override
    public void run(){

        while(active){
            try {
                GenericPacket gp = E2EComm.receive(service);
                new MeasurerHandle(gp).start();
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }

        
        
    }

    private class MeasurerHandle extends Thread{

        private GenericPacket gp;

        MeasurerHandle(GenericPacket gp) {
            this.gp = gp;
        }

        @Override
        public void run() {

            UnicastPacket up = (UnicastPacket)gp;
            Object payload = null;
            try {
                payload = E2EComm.deserialize(up.getBytePayload());
            } catch (Exception e) {
                // e.printStackTrace();
            }
            if(payload instanceof MeasureMessage){
                MeasureMessage mm = (MeasureMessage)payload;
                int messagetype = mm.getMessageType();
                // System.out.println("==========");
                MeasureMessage res;
                switch (messagetype) {
                    case MeasureMessage.Check_Occupy:
                        // System.out.println("receive request about check occupy");
                        if(tryOccupy()){
                            res = new MeasureMessage(MeasureMessage.Response_OK);
                            try {
                                E2EComm.sendUnicast(
                                    E2EComm.ipReverse(up.getSource()),
                                    mm.getClientPort(),
                                    E2EComm.UDP,
                                    E2EComm.serialize(res)
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }else{
                            res = new MeasureMessage(MeasureMessage.Response_Occupied);
                            try {
                                E2EComm.sendUnicast(
                                    E2EComm.ipReverse(up.getSource()),
                                    mm.getClientPort(),
                                    E2EComm.UDP,
                                    E2EComm.serialize(res)
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case MeasureMessage.Test_Delay:
                        // System.out.println("receive request about test delay");

                        res = new MeasureMessage(MeasureMessage.Response_OK);
                        try {
                            E2EComm.sendUnicast(
                                E2EComm.ipReverse(up.getSource()),
                                mm.getClientPort(),
                                E2EComm.UDP,
                                E2EComm.serialize(res)
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        break;

                    case MeasureMessage.Test_Throughput:
                        // System.out.println("receive request about test throughput");

                        String fileName = mm.getFilename();
                        try {
                            File f = new File("./temp/fsService" + "/" + fileName);
	                        FileInputStream fis = new FileInputStream(f);

                            E2EComm.sendUnicast(
                                E2EComm.ipReverse(up.getSource()),
                                mm.getClientPort(),
                                E2EComm.TCP,
                                fis
                            );
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case MeasureMessage.Test_Done:
                        releaseOccupy();
                        break;
                }
            }
        }
    }
}