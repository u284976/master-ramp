package test.iotos;


import it.unibo.deis.lia.ramp.RampEntryPoint;
import it.unibo.deis.lia.ramp.core.e2e.BoundReceiveSocket;
import it.unibo.deis.lia.ramp.core.e2e.E2EComm;
import it.unibo.deis.lia.ramp.core.e2e.UnicastPacket;
import it.unibo.deis.lia.ramp.service.management.ServiceManager;
import test.iotos.messagetype.timeDataType;

public class testReceiver {
    
    // static List<Integer> seq = new ArrayList<>();
    // static List<Long> sendTime = new ArrayList<>();
    // static List<Long> receiveTime = new ArrayList<>();

    


    static RampEntryPoint ramp;
    
    public static void main(String[] args){
        ramp = RampEntryPoint.getInstance(true, null);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (ramp != null) {
						System.out.println("ShutdownHook is being executed: gracefully stopping RAMP...");
						ServiceManager.getInstance(false).removeService("application1");
						ramp.stopRamp();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
        }));


        BoundReceiveSocket client = null;
        try {
            client = E2EComm.bindPreReceive(E2EComm.UDP);
        } catch (Exception e) {
            //TODO: handle exception
        }


        ServiceManager.getInstance(false).registerService("application2", client.getLocalPort(), E2EComm.UDP);
        try{
            Thread.sleep(2*1000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        ramp.forceNeighborsUpdate();


        int[] seq = new int[100000];
        long[] sendTime = new long[100000];
        long[] receiveTime = new long[100000];
        
        System.out.println("==============wait receive==============");

        for(int i=0 ; i<seq.length ; i++){
            seq[i] = -1;
            sendTime[i] = -1;
            receiveTime[i] = -1;
        }



        UnicastPacket up = null;
        try {
            up = (UnicastPacket)E2EComm.receive(client);
            new listener(up, seq, sendTime, receiveTime).start();
        } catch (Exception e) {
            //TODO: handle exception
        }
        
        

        long prewhile = System.currentTimeMillis();
        long time_diff = System.currentTimeMillis() - prewhile;
        while(time_diff < 3*1000){
            time_diff = System.currentTimeMillis() - prewhile;
            System.out.println(time_diff);
            try {
                up = (UnicastPacket)E2EComm.receive(client,1000);
                System.out.println("receive packet");
                if(up==null){
                    break;
                }
                new listener(up, seq, sendTime, receiveTime).start();
            } catch (Exception e) {
                //TODO: handle exception
            }
        }

        System.out.println("==============end while==============");

        int c = 0;

        long[] delay = new long[100000];
        long total_delay = 0;
        for(int i=0 ; i<seq.length ; i++){
            if(sendTime[i] == -1){

            }else{
                c++;
                delay[i] = receiveTime[i] - sendTime[i];
                total_delay = total_delay + delay[i];
                System.out.println( delay[i] ); 
            }
        }
        System.out.println("c = " + c);
        System.out.println("total_delay = " + total_delay);
        System.out.println("avg_delay = " + total_delay/c);
    }

    
}

class listener extends Thread{
    int[] seq;
    long[] sendTime;
    long[] receiveTime;
    UnicastPacket up;

    listener(UnicastPacket up , int[] seq, long[] sendTime, long[] receiveTime){
        this.seq = seq;
        this.sendTime = sendTime;
        this.receiveTime = receiveTime;
        this.up = up;
    }

    public void run(){
        timeDataType payload = null;
        try {
            payload = (timeDataType)E2EComm.deserialize(up.getBytePayload());
        } catch (Exception e) {
            //TODO: handle exception
        }
        
        int seq = payload.getSeqNumber();
        long send = payload.getSendTime();
        long rece = System.currentTimeMillis();

        System.out.println("===packet====");
        System.out.println(seq);
        System.out.println(send);
        System.out.println(rece);

        sendTime[seq] = send;
        receiveTime[seq] = rece;
    }
}