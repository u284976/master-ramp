package test.iotos;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import test.iotos.messagetype.timeDataType;

public class singlePacketSender {
    

    static int number_of_packet = 5;

    public static void main(String[] args){
        try {
            InetAddress address = InetAddress.getByName("10.0.89.9");

            
            DatagramSocket ds = new DatagramSocket();


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
                
                ds.send(dp);
            }
            
            
			ds.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}