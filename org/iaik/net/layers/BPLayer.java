package org.iaik.net.layers;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.RUDP.RUDPConnection;
import org.iaik.net.datatypes.interfaces.ARPTable;
import org.iaik.net.exceptions.NetworkException;
import org.iaik.net.interfaces.BPModule;
import org.iaik.net.interfaces.InternetLayer;
import org.iaik.net.interfaces.TransportLayer;
import org.iaik.net.packets.IPPacket;
import org.iaik.net.packets.Packet;
import org.iaik.net.utils.NetworkBuffer;

import com.sun.xml.internal.bind.v2.runtime.property.Property;


public class BPLayer implements InternetLayer, TransportLayer, BPModule {
	TransportLayer transportLayer;
	InternetLayer internetLayer;
	Properties properties;
	
	BPModule firstModule;
	
	Log log;
	
	public BPLayer()
	{
		log = LogFactory.getLog(this.getClass());
	}
	
	/**
	 * from APP to wire
	 */
	@Override
	public void send(Packet packet) {
		//Let's produce some bullshit!
		firstModule.passThrough(packet);
	}
	

	/**
	 * from wire to APP
	 */
	@Override
	public void process(IPPacket packet) {
		transportLayer.process(packet);
	}
	
	@Override
	public void init() throws NetworkException {
		BPModule lastModule = this;
		
		log.debug("loading modules!");
		
		for(int i = 0; i < 10; i++)
		{
			String prop = properties.getProperty("BPModule" + String.format("%02d",i));
			
			
			if(prop != null)
			{
				BPModule loadedModule;
				try {
					loadedModule = (BPModule) Class.forName(prop).newInstance();
				}
				catch (InstantiationException e) {
					e.printStackTrace();
					throw new NetworkException("BPModule" + String.format("%02d",i) +": " + prop + ": InstantiationException");
				}
				catch (IllegalAccessException e) {
					e.printStackTrace();
					throw new NetworkException("BPModule" + String.format("%02d",i) +": " + prop + ": IllegalAccessException");
				}
				catch (ClassNotFoundException e) {
					e.printStackTrace();
					throw new NetworkException("BPModule" + String.format("%02d",i) +": " + prop + ": class not found");
				}
				
				String param = properties.getProperty("BPModule" + String.format("%02d",i) + "param");
				loadedModule.setParameter(param);
				
				
				lastModule.setNextModule(loadedModule);
				
				lastModule = loadedModule;
				
				log.debug("loaded module:" + prop + "with params: " + param);
			}
			else
			{
				break;
			}
		}
		
		lastModule.setNextModule(this);
	}
	
	/**
	 * here packages arrive from the BPModules. -> after filtering 
	 */
	@Override
	public void passThrough(Packet packet)
	{
		internetLayer.send(packet);
	}
	
	///////////////////NOT NECCESSARY::::::::::
	

	@Override
	public void setNextModule(BPModule next) {
		firstModule = next;
		
	}
	
	@Override
	public void checkTimeouts() {
		// TODO Auto-generated method stub
	
	}

	@Override
	public ARPTable getARPTable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public NetworkBuffer getReceiveBuffer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NetworkBuffer getSendBuffer() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void send(byte[] data, int address, byte protocol) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(byte[] data, String address, byte protocol) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setProperties(Properties properties) {
		this.properties = properties;
		
	}

	@Override
	public void setTransportLayer(TransportLayer transportLayer) {
		this.transportLayer = transportLayer;
		
	}

	@Override
	public void terminate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	

	///////////////////////////////////////////Transport layer methods:

	
	@Override
	public void addRUDPConnection(RUDPConnection connection) {
		// TODO Auto-generated method stub
	}


	@Override
	public void removeRUDPConnection(RUDPConnection connection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendPacket(IPPacket packet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInternetLayer(InternetLayer internetLayer) {
		this.internetLayer = internetLayer;
		
	}


	@Override
	public void setParameter(String parameter) {
		// TODO Auto-generated method stub
		
	}
}
