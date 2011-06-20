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

import com.sun.corba.se.pept.transport.Connection;



public class RUDPClientConnection extends RUDPConnection {
	private ClientState state;
	private RUDPClientCallback clientCallback;
	private final int maxConnectRetries = 10;
	private final int connectTimeoutms = 5000;
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
		startThread();
	}
	
	@Override
	public boolean isConnected() {
		return state == ClientState.Connected;
	}

	@Override
	protected void connectPhase() throws InterruptedException {
		connectConditionLock.lock();
		do
		{
			try
			{
				connectCondition.await();
			}
			catch(InterruptedException ex)
			{
				connectConditionLock.unlock();
				throw ex;
			}
		}
		while(!isConnected());
		connectConditionLock.unlock();
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
		
		connectConditionLock.lock();
		
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

					connectCondition.await();

					log.debug("woke up");
					
					//TODO:additional checks necessary!!!
					
					if(connectTimeoutReached || synAckReceived.getAck_num() == (lastSequenceNrSent))
						break;
					else if(synAckReceived.getAck_num() != lastSequenceNrSent)
						log.warn("received packet with invalid ack-nr(" + synAckReceived.getAck_num() + ") instead of " + (lastSequenceNrSent+1));
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				TransportLayerFactory.getInstance().removeRUDPConnection(this);
				connectConditionLock.unlock();
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
			
			//SYNACK received!
			
			if(maxSegmentSize != synAckReceived.getMax_segment_size())
			{
				maxSegmentSize = synAckReceived.getMax_segment_size();
				appReadBuffer = new byte[maxSegmentSize];
			}
			
			
			//now Send ACK:
			//TODO: calculate advertised window size
			rudpPack = new RUDP_ACKPacket((short)remotePort, (short)port, (byte)(++lastSequenceNrSent), (byte)(synAckReceived.getSeq_num()), (byte)5);
			
			rudpPackIP = IPPacket.createDefaultIPPacket(IPPacket.RUDP_PROTOCOL, (short)0, Network.ip, remoteIP, rudpPack.getPacket());
			transportLayer.sendPacket(rudpPackIP);
			
			nextSeqExpected = (byte)(synAckReceived.getSeq_num() + 1);
			

			state = ClientState.Connected;
			
			initForNewConnection();
			
			connectCondition.signal();

			break;
		}
		
		connectConditionLock.unlock();
		
		if(state != ClientState.Connected)
		{
			log.debug("failed to connect to server after " + maxConnectRetries + " tries");
			TransportLayerFactory.getInstance().removeRUDPConnection(this);
			
			throw new RUDPException("failed to connect to server after " + maxConnectRetries + " tries");
		}

		log.debug("We're connected now!!!");

	}
	
	private class ConnectTimeout extends TimerTask
	{

		@Override
		public void run() {
			connectTimeoutReached = true;
			
			log.debug("in ConnectTimout-Timer ....");
			
			connectConditionLock.lock();
			connectCondition.signalAll();
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
				connectCondition.signalAll();
				connectConditionLock.unlock();
			}
			else
				log.warn("connectPhasePacketReceived: received SYN packet without ACK???");
		}
		else
			log.warn("connectPhasePacketReceived: received packet is not of type RUDP_SYNPacket!");
	}

	
	@Override
	protected void disconnect(boolean sendRST) {
		if(isConnected() && sendRST)
		{
			//send RST Packet
			RUDPPacket rudpPack;
			IPPacket rudpPackIP;
			
			
			rudpPack = new RUDP_RSTPacket((short)remotePort, (short)port, (byte)0,(byte)0);
			
			rudpPackIP = IPPacket.createDefaultIPPacket(IPPacket.RUDP_PROTOCOL, (short)0, Network.ip, remoteIP, rudpPack.getPacket());
			transportLayer.sendPacket(rudpPackIP);
		}
		
		if(isConnected())
			TransportLayerFactory.getInstance().removeRUDPConnection(this);
		
		connectConditionLock.lock();
		state = ClientState.Disconnected;
		
		nulDaemon.stop();
		
		//to interrupt the current flow in the thread ...
		interruptThread();
		connectConditionLock.unlock();
	}
}
