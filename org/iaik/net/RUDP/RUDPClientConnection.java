package org.iaik.net.RUDP;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import org.iaik.net.Network;
import org.iaik.net.packets.IPPacket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.exceptions.RUDPException;
import org.iaik.net.factories.TransportLayerFactory;
import org.iaik.net.interfaces.RUDPClientCallback;
import org.iaik.net.packets.rudp.*;



public class RUDPClientConnection extends RUDPConnection {
	private ClientState state;
	private RUDPClientCallback clientCallback;
	private final int maxConnectRetries = 1;
	private final int connectTimeoutms = 10000;
	private Condition connectCondition;
	private Lock connectConditionLock;
	private Log log;
	private boolean connectTimeoutReached = false;
	
	private RUDP_SYNPacket synAckReceived;

	public RUDPClientConnection(int port, String remoteIP, int remotePort, RUDPClientCallback callback) {
		super(port, callback);
		super.remotePort = remotePort;
		super.remoteIP = remoteIP;
		state = ClientState.Disconnected;
		this.clientCallback = callback;
		
		connectConditionLock = new ReentrantLock();
		connectCondition = connectConditionLock.newCondition();
		
		log = LogFactory.getLog(this.getClass());
	}
	
	@Override
	public boolean isConnected() {
		return state == ClientState.Connected;
	}

	@Override
	protected void connectPhase() {
		
	}
	
	/**
	 * connects to the specified server.
	 */
	public void connect() throws RUDPException
	{
		int connectTry;
		
		RUDPPacket rudpPack;
		IPPacket rudpPackIP;
		
		TransportLayerFactory.getInstance().addRUDPConnection(this);
		
		for(connectTry = 0; connectTry < maxConnectRetries; connectTry++)
		{
			log.debug("connecting to client... try: " + connectTry);
			//Send SYN:
			lastSequenceNrSent = 123;
			rudpPack = new RUDP_SYNPacket(false, (byte)lastSequenceNrSent, (byte)0, (short)remotePort, (short)port);
			
			rudpPackIP = IPPacket.createDefaultIPPacket(IPPacket.RUDP_PROTOCOL, (short)0, Network.ip, remoteIP, rudpPack.getPacket());
			transportLayer.sendPacket(rudpPackIP);

			log.debug("SYN Packet sent, waiting for SYNACK");
			
			state = ClientState.SYNSent;
			
			Timer connectTimer = new Timer();
			connectTimer.schedule(new ConnectTimeout(), connectTimeoutms);
			
			//Wait for SYNACK
			try {
				while(true)
				{
					connectTimeoutReached = false;
					connectConditionLock.lock();
					connectCondition.await();
					connectConditionLock.unlock();
					log.debug("woke up");
					
					//TODO:additional checks necessary!!!
					
					if(connectTimeoutReached || synAckReceived.getAck_num() == (lastSequenceNrSent+1))
						break;
					else if(synAckReceived.getAck_num() != lastSequenceNrSent)
						log.warn("received packet with invalid seq-nr(" + synAckReceived.getAck_num() + ") instead of " + (lastSequenceNrSent+1));
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				TransportLayerFactory.getInstance().removeRUDPConnection(this);
				return;
			}
			
			if(connectTimeoutReached)
			{
				//retry...
				log.debug("timeout reached...");
				state = ClientState.Disconnected;
				continue;
			}
			
			connectTimer.cancel();
			
			//SYNACK received, so Send ACK:
			//TODO: calculate advertised window size
			rudpPack = new RUDP_ACKPacket((short)remotePort, (short)port, (byte)(++lastSequenceNrSent), (byte)(synAckReceived.getSeq_num()+1), (byte)5);
			
			rudpPackIP = IPPacket.createDefaultIPPacket(IPPacket.RUDP_PROTOCOL, (short)0, Network.ip, remoteIP, rudpPack.getPacket());
			transportLayer.sendPacket(rudpPackIP);
			
			state = ClientState.Connected;
			break;
		}
		
		if(state != ClientState.Connected)
		{
			log.debug("failed to connect to server after " + maxConnectRetries + " tries");
			TransportLayerFactory.getInstance().removeRUDPConnection(this);
			throw new RUDPException("failed to connect to server after " + maxConnectRetries + " tries");
		}
		else
		{
			log.debug("We're connected now!!!");
		}
	}
	
	private class ConnectTimeout extends TimerTask
	{

		@Override
		public void run() {
			connectTimeoutReached = true;
			
			log.debug("in ConnectTimout-Timer ....");
			
			connectConditionLock.lock();
			connectCondition.signal();
			connectConditionLock.unlock();
		}
	
	}

	@Override
	protected void connectPhasePacketReceived(RUDPPacket packet, String srcIP) {
		if(packet instanceof RUDP_SYNPacket)
		{
			RUDP_SYNPacket synPacket = (RUDP_SYNPacket)packet;
			if(synPacket.isAck())
			{
				//some additional checks!
				synAckReceived = synPacket;
				log.debug("connectPhasePacketReceived: received a SYNACK packet");
				
				connectConditionLock.lock();
				connectCondition.signal();
				connectConditionLock.unlock();
			}
			else
				log.warn("connectPhasePacketReceived: received SYN packet without ACK???");
		}
		else
			log.warn("connectPhasePacketReceived: received packet is not of type RUDP_SYNPacket!");
	}
}
