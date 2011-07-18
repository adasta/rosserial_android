package org.ros;

import java.net.InetAddress;

import org.ros.*;
import org.ros.internal.node.address.InetAddressFactory;

public class RosUtils {

	public static Node createExternalMaster(String node_name, String masterURI){
		Node node;
	      NodeConfiguration nodeConfiguration = NodeConfiguration.createDefault();
	      java.net.URI muri =java.net.URI.create( masterURI);
	   
	      nodeConfiguration.setMasterUri(muri);
	      InetAddress host = org.ros.internal.node.address.InetAddressFactory.createNonLoopback();
	      nodeConfiguration.setHost(host.getHostAddress());
		
	    node = new DefaultNode(node_name, nodeConfiguration);
		return node;
	}
}
