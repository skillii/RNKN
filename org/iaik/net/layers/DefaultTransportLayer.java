package org.iaik.net.layers;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.RUDP.*;
import org.iaik.net.exceptions.*;
import org.iaik.net.interfaces.*;
import org.iaik.net.packets.*;
import org.iaik.net.packets.rudp.RUDPPacket;

public class DefaultTransportLayer implements TransportLayer {

	private Properties properties;
	InternetLayer internetLayer;
	private Log log;
	
	Vector<RUDPConnection> connections;
	
	@Override
	public void checkTimeouts() {
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public void init() throws NetworkException {
		connections = new Vector<RUDPConnection>();
		log = LogFactory.getLog(this.getClass());
	}

	@Override
	public void process(IPPacket packet) {
		log.debug("incoming packet, protocol:" + packet.getProtocol());
		
		if(packet.getProtocol() == IPPacket.RUDP_PROTOCOL)
		{
			//TODO: packet parsing stuff...
			
			RUDPPacket rudpPacket;
			
			try {
				rudpPacket = RUDPPacket.parsePacket(packet.getPayload());
			} catch (PacketParsingException e) {
				log.warn("failed to parse packet!");
				e.printStackTrace();
				return;
			}
			
			int port = rudpPacket.getDest_port();
			
			boolean found = false;
			
			synchronized(connections)
			{
				int i;
				for(i = 0; i < connections.size(); i++)
				{
					if(connections.get(i).getPort() == port)
					{
						log.debug("found a connection for that incoming packet: " + rudpPacket.toString());
						connections.get(i).packetReceived(rudpPacket, packet.getSourceAddress());
						found = true;
						break;
					}
				}
			}
			
			if(!found)
				log.warn("found no connection for that incoming packet, port: " + rudpPacket.getDest_port());
		}
	}


	@Override
	public void setProperties(Properties properties) {
		this.properties = properties;
		
	}

	@Override
	public void terminate() {
		// TODO Auto-generated method stub
	}

	@Override
	public void setInternetLayer(InternetLayer internetLayer) {
		this.internetLayer = internetLayer;		
	}

	@Override
	public void addRUDPConnection(RUDPConnection connection) {
		synchronized(connections)
		{
			this.connections.add(connection);
		}
		
	}

	@Override
	public void removeRUDPConnection(RUDPConnection connection) {
		synchronized(connections)
		{
			this.connections.remove(connection);
		}
		
	}

	@Override
	public void sendPacket(IPPacket packet) {
		internetLayer.send(packet);
	}
}
