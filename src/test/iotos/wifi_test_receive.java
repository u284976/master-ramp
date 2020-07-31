package test.iotos;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class wifi_test_receive { 
	public static void main(String[] args)throws IOException{
		byte[] buf = new byte[51200];
		//服務端在3000埠監聽接收到的資料
		DatagramSocket ds = new DatagramSocket(3000);
		//接收從客戶端傳送過來的資料
		DatagramPacket dp_receive = new DatagramPacket(buf, buf.length);
		System.out.println("server is on，waiting for client to send data......");
		boolean f = true;
		while(f){
			System.out.println("****");
			ds.receive(dp_receive);

			System.out.println("receive!");
		}
		ds.close();
	}
}