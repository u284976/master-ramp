package test.iotos;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class wifi_test_sender {
	public static void main(String args[])throws IOException{

		if(args[0].equals("10.0.0.1")){
			boolean done = false;
			while(!done){
			    new packetSend("10.0.0.1").start();
			}
		}else{
			boolean done = false;
			while(!done){
			    new packetSend("10.0.0.2").start();
			}
		}
        
	} 

	
}

class packetSend extends Thread{

	InetAddress address;

	public packetSend(String target){
		try {
			this.address = InetAddress.getByName(target);	
		} catch (Exception e) {
			//TODO: handle exception
		}
	}

	public void run(){

		try {
			DatagramSocket ds = new DatagramSocket();
			byte[] sendByte = new byte[50000];
			DatagramPacket dp_send= new DatagramPacket(sendByte,sendByte.length,address,3000);
			ds.send(dp_send);
			ds.close();
		} catch (Exception e) {
			//TODO: handle exception
		}
		
	}

}