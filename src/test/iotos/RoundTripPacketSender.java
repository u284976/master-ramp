package test.iotos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import test.iotos.messagetype.timeDataType;

public class RoundTripPacketSender {
    

    static int number_of_packet = 5;

    public static void main(String[] args){
        try {
            InetAddress address = InetAddress.getByName("192.168.72.8");

            
            DatagramSocket ds_s = new DatagramSocket();
            DatagramSocket ds_r = new DatagramSocket(3001);
            new RoundTripSenderHandler(ds_r).start();;

            ds_s.setSendBufferSize(1400*5);

            timeDataType payload = new timeDataType();
            payload.setPayloadSize(1000);

            for(int i=0 ; i<number_of_packet ; i++){
                payload.setSeqNumber(i);
                payload.setSendTime(System.currentTimeMillis());

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = null;
                byte[] sendByte;
                out = new ObjectOutputStream(bos);   
                out.writeObject(payload);
                out.flush();
                sendByte = bos.toByteArray();
                bos.close();

                DatagramPacket dp = new DatagramPacket(sendByte,sendByte.length,address,3000);
                
                ds_s.send(dp);
            }
            
            
			ds_s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}

class RoundTripSenderHandler extends Thread{

    DatagramSocket ds;

    RoundTripSenderHandler(DatagramSocket ds){
        this.ds = ds;
        System.out.println("listen echo...");
    }

    public void run(){

        try {
            boolean f = true;
            while(f){
                byte[] buf = new byte[1400];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
    
                ds.receive(dp);


                ByteArrayInputStream bis = new ByteArrayInputStream(dp.getData());
                ObjectInput in = null;
                in = new ObjectInputStream(bis);
                Object o = in.readObject();


                if(o instanceof timeDataType){
                    timeDataType payload = (timeDataType)o;
                    
                    int seq = payload.getSeqNumber();
                    long st = payload.getSendTime();
                    long current = System.currentTimeMillis();

                    System.out.println(seq + "      " + st + "      " + current + "     " + (current-st));
                }
            }
            
        } catch (Exception e) {
            //TODO: handle exception
        }
    }
}