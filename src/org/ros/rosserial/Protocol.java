package org.ros.rosserial;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import org.ros.message.*;

import org.ros.message.Message;
import org.ros.MessageListener;
import org.ros.Node;
import org.ros.Publisher;
import org.ros.Subscriber;


import org.ros.message.rosserial_msgs.*;
import org.ros.message.std_msgs.Time;
import org.ros.rosserial.RosSerial.PARSE_STATE;
import org.ros.rosserial.RosSerial.ROSTopic;

import sun.nio.ByteBuffered;

import java.nio.ByteBuffer;


public class Protocol {

	//SPECIAL IDS
	//All IDS greater than 100 are Publishers/Subscribers
	static final int TOPIC_PUBLISHERS = 0;
	static final int TOPIC_SUBSCRIBERS = 1;
	static final int TOPIC_TIME = 10;


	//ROS , publishers, subscriber,  and topic info 
	Node node;
	private Hashtable<Integer, TopicInfo> id_to_topic;
	private Hashtable<String,  Integer> topic_to_id;
	
	private Hashtable<Integer, Publisher>  publishers;
	private Hashtable<Integer, Subscriber> subscribers;
	private Hashtable<Integer, Class > msg_classes;
	
	
	//Packet Handler interface is used to 
	//Send the packet over
	public interface PacketHandler{
		void send(byte[] data);
	}
	PacketHandler packetHandler;
	
	//This is a helper inner class
	//All it does is take the subscriber callbacks
	//serialize the message, attaches the id/length,
	//and forwards it to the packetHandler
	private class MessageListenerForwarding<MessageType> implements org.ros.MessageListener<MessageType>{
		
		Protocol protocol;
		int id;
		
		public MessageListenerForwarding(int topic_id, Protocol p){
			protocol = p;
			id= topic_id;
		}
		
		public void onNewMessage( MessageType t) {
			byte[] data = protocol.constructMessage(id, (Message) t );
			protocol.packetHandler.send(data);
	      }
	}
	
	
	
	
	public Protocol(Node nh, PacketHandler handler){
		this.node = nh;
		this.packetHandler = handler;
	}
	
	
	//To find out when the are new subscriptions
	//or publications, register a listener
	public interface TopicRegistrationListener {
		void onNewTopic(TopicInfo t);
	}
	TopicRegistrationListener newPubListener;
	TopicRegistrationListener newSubListener;
	void setOnNewPublication(TopicRegistrationListener listener){
		newPubListener = listener;
	}
	void setOnNewSubcription(TopicRegistrationListener listener){
		newSubListener = listener;
	}
	
	

	public void negotiateTopics(){
		byte request[] = {(byte) 0, (byte) 0,(byte) 0, (byte) 0};
		packetHandler.send(request);
	}
	

	
	public byte[] constructMessage (int id, org.ros.message.Message m){		
		int l = m.serializationLength();
		byte[] data = new byte[l+4];
		ByteBuffer buff = ByteBuffer.wrap(data, 4, l);

		data[0] = (byte) id;
		data[1] = (byte) (id >>8);
		data[2] = (byte) l;
		data[3] = (byte) (l >> 8);
		
		m.serialize(buff,0);
		
		return data;
	}
	
	private void addTopic(TopicInfo topic, boolean is_publisher){
		String name = topic.topic_name;
		String type = topic.message_type;
		Integer id = topic.topic_id;
				
		try{
			String[] type_parts = type.split("/");
			Class msg_class = ClassLoader.getSystemClassLoader().loadClass("org.ros.message."+type_parts[0]+"."+type_parts[1]);
			
			msg_classes.put(topic.topic_id, msg_class);
			topic_to_id.put(topic.topic_name, topic.topic_id);
			id_to_topic.put(id, topic);
			
			if (is_publisher){
				Publisher pub = node.createPublisher(name, type );
				publishers.put(id,pub);
			}
			else{
				Subscriber sub = node.createSubscriber(name, type, new MessageListenerForwarding(id,this) );
				subscribers.put(id,sub);
			}	
		}
		catch (Exception e){
			e.printStackTrace();
			}
	}
	
	
	TopicInfo[] getSubscriptions(){
		TopicInfo[] topics =  new TopicInfo[subscribers.size()];
		
		Enumeration<Integer> e = subscribers.keys();
		
		for (int i =0; e.hasMoreElements(); i ++){
			Integer id =  e.nextElement();
			topics[i]  = id_to_topic.get(id);
		}
		return topics;
	}
	TopicInfo[] getPublications(){
		TopicInfo[] topics =  new TopicInfo[publishers.size()];
		
		Enumeration<Integer> e = publishers.keys();
		
		for (int i =0; e.hasMoreElements(); i ++){
			Integer id =  e.nextElement();
			topics[i]  = id_to_topic.get(id);
		}
		return topics;
	}
	
	public boolean parsePacket(byte[] data){
		int topic_id = (int)data[0] | (int)(data[1]) <<  8;
		int data_len = (int)data[2] | (int)(data[3]) <<  8 ;

		ByteBuffer msg_data = ByteBuffer.wrap(data, 4, data_len);
		
		switch(topic_id){
			case TOPIC_PUBLISHERS:
				{
				TopicInfo m =  new TopicInfo();
				m.deserialize(msg_data);
				addTopic( m,  true);
				break;
				}
			case TOPIC_SUBSCRIBERS:
				{
				TopicInfo m =  new TopicInfo();
				m.deserialize(msg_data);
				addTopic(m,  false);
				break;
				}
			case TOPIC_TIME:
				org.ros.message.Time t = node.getCurrentTime();
				org.ros.message.std_msgs.Time t_msg = new org.ros.message.std_msgs.Time();
				t_msg.data = t;
				packetHandler.send(constructMessage(TOPIC_TIME,t_msg));
				break;
			default:
				try{
					Message msg = (Message) msg_classes.get(topic_id).newInstance();
					msg.deserialize(msg_data);
					publishers.get(topic_id).publish(msg);
				}
				catch(IllegalAccessException e){
					System.out.println("Illegal access for : " + id_to_topic.get(topic_id).topic_name);
					e.printStackTrace();
				}
				catch(InstantiationException e){
					System.out.println("Could not instantiate : " + id_to_topic.get(topic_id).topic_name);
					e.printStackTrace();
				}
				break;
		}
		
		return false;
	}
	
}
