package org.ros.rosserial;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ros.Node;

public class ROSSerial implements Runnable{

	OutputStream ostream;
	BufferedInputStream istream;
	
	Node node;
	public Protocol protocol;
	
	boolean running = false;
	
	public ROSSerial (Node nh, InputStream input, OutputStream output){
		ostream = output;
		istream = new BufferedInputStream(input,100);
		node = nh;
		
		protocol = new Protocol(node, sendHandler);
		
	}
	
	
	Protocol.PacketHandler sendHandler = new Protocol.PacketHandler() {
		@Override
		public void send(byte[] data) {
			//calculate the checksum
			int chk = 0;
			for(int i =0; i< data.length; i++) chk+= 0xff & data[i];
			chk = 255-chk%256;
			
			byte[] flags = { (byte) 0xff, (byte) 0xff};
			try{
				ostream.write(flags);
				ostream.write(data);
				ostream.write((byte)chk);
			}
			catch (IOException e) {
				System.out.println("Exception sending :" + BinaryUtils.byteArrayToHexString(data));
			}
		}
	};

	
	public void run(){
		
		protocol.negotiateTopics();
		resetPacket();
		
		while (running){
			try{
				int b = istream.read();
				if (b!= -1){
		    		handleByte((byte) (0xff & b) );
		    	}
			}
			catch(IOException e){
				e.printStackTrace();
				System.out.println("IO Exception, exiting rosserial run thread");
				return;
			}
			catch(Exception e ){
				e.printStackTrace();
			}
		}
	}
	
	
	private enum PACKET_STATE {FLAGA, FLAGB, HEADER, DATA, CHECKSUM};
	PACKET_STATE packet_state;
	byte[] data= new byte[512];
	int data_len =0;
	int byte_index=0;;
	
	private void resetPacket(){
		byte_index =0;
		data_len =0;
		packet_state = PACKET_STATE.FLAGA;
	}
	
	boolean handleByte(byte b){
		switch (packet_state){
		case FLAGA:
			if (b == (byte) 0xff) packet_state =  PACKET_STATE.FLAGB;
			break;
		case FLAGB:
			if (b == (byte) 0xff) packet_state =  PACKET_STATE.HEADER;
			else {
				resetPacket();
				return false;
			}
			break;
		case HEADER:
			data[byte_index] = b;
			byte_index++;
			if (byte_index==4){
				int len = data[3] |  (data[4] << 8) ;
				data_len = len+4; //add in the header length
				packet_state = PACKET_STATE.DATA;
			}
			break;
		case DATA:
			data[byte_index] = b;
			byte_index++;
			if (byte_index == data_len){
				packet_state = PACKET_STATE.CHECKSUM;
			}
			break;
		case CHECKSUM:
			int chk = 0;
			for (int i=0; i< data_len; i++) chk+= (0xff & data[i]);
			if (chk%256 != 255){
				resetPacket();
				return false;
			}
			protocol.parsePacket(data);
			resetPacket();
			break;
		}
		return true;
	}
	
}
