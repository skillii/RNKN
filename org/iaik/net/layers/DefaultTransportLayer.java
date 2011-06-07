package org.iaik.net.layers;
import java.util.Properties;
import java.util.Vector;

import org.iaik.net.RUDP.*;
import org.iaik.net.exceptions.*;
import org.iaik.net.interfaces.*;
import org.iaik.net.packets.*;
import org.iaik.net.packets.rudp.RUDPPacket;

public class DefaultTransportLayer implements TransportLayer {

	private Properties properties;
	InternetLayer internetLayer;
	
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
	}

	@Override
	public void process(IPPacket packet) {
		if(packet.getProtocol() == IPPacket.RUDP_PROTOCOL)
		{
			//TODO: packet parsing stuff...
			
			int port = 0;
			
			for(int i = 0; i < connections.size(); i++)
			{
				if(connections.get(i).getPort() == port)
				{
					RUDPPacket rudpPacket;
					try {
						rudpPacket = RUDPPacket.parsePacket(packet.getPayload());
					} catch (PacketParsingException e) {
						//TODO: what should we dooo????
						e.printStackTrace();
						return;
					}
					connections.get(i).packetReceived(rudpPacket);
				}
			}
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
		this.connections.add(connection);
		
	}

	@Override
	public void removeRUDPConnection(RUDPConnection connection) {
		this.connections.remove(connection);
		
	}

	@Override
	public void sendPacket(IPPacket packet) {
		internetLayer.send(packet);
	}
}
