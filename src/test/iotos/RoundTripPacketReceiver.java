package test.iotos;

import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import test.iotos.messagetype.timeDataType;


public class RoundTripPacketReceiver {
    
    static int number_of_packet = 5;

    public static void main(String[] args){
        try {
            
            
            DatagramSocket ds_r = new DatagramSocket(3000);
            DatagramSocket ds_s = new DatagramSocket(3001);
            InetAddress address = InetAddress.getByName("192.168.72.6");
            System.out.println("Start Receiver echo to 192.168.72.6");
     
            long[] sendTime = new long[number_of_packet];
            long[] receiveTime = new long[number_of_packet];
            List<Long> realReceiveTimes = new ArrayList<Long>();
            

            boolean f = true;
            while(f){

                int i=0;
                while(i < number_of_packet){
                    byte[] buf = new byte[1400];
                    DatagramPacket dp = new DatagramPacket(buf, buf.length);
                    ds_r.receive(dp);
                    System.out.println("11111111111111111111111");
                    // System.out.println("record real time receive");
                    
                    DatagramPacket dp_s = new DatagramPacket(buf,buf.length,address,3001);
                    ds_s.send(dp_s);

                    long currentTime = System.currentTimeMillis();
                    realReceiveTimes.add(currentTime);

                    // System.out.println(currentTime);

                    new RoundTripReceiverHandler(dp,sendTime,receiveTime).start();
                    i++;
                }
    
                Thread.sleep(500);

                System.out.println("====================");
                // sendTime, realReceiveTime, (realReceiveTime-sendTime), threadReceiveTime, (threadReceiveTime-sendTime)
                for(int j=0 ; j<number_of_packet ; j++){
                    System.out.print(sendTime[j] + "       " + realReceiveTimes.get(j) + "      " + (realReceiveTimes.get(j) - sendTime[j]) + "     " + receiveTime[j] + "       " + (receiveTime[j] - sendTime[j]));
                    System.out.println();
                }

                realReceiveTimes.clear();
            }

            ds_s.close();
            ds_r.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}

class RoundTripReceiverHandler extends Thread{

    DatagramPacket dp;
    long[] sendTime;
    long[] receiveTime;

    RoundTripReceiverHandler(DatagramPacket dp, long[] sendTime, long[] receiveTime){
        this.dp = dp;
        this.sendTime = sendTime;
        this.receiveTime = receiveTime;
    }

    public void run(){
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(dp.getData());
            ObjectInput in = null;
            in = new ObjectInputStream(bis);
            Object o = in.readObject();


            if(o instanceof timeDataType){
                timeDataType payload = (timeDataType)o;
                
                int seq = payload.getSeqNumber();
                long st = payload.getSendTime();

                sendTime[seq] = st;
                receiveTime[seq] = System.currentTimeMillis();
                System.out.println("22222222222222222222222");
                // System.out.println("record thread execute get current Time");
            }
        } catch (Exception e) {
            //TODO: handle exception
        }
        
    }
}