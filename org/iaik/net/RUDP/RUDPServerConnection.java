package org.iaik.net.RUDP;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.Network;
import org.iaik.net.factories.TransportLayerFactory;
import org.iaik.net.interfaces.RUDPServerCallback;
import org.iaik.net.interfaces.TransportLayer;
import org.iaik.net.packets.IPPacket;
import org.iaik.net.packets.rudp.*;


public class RUDPServerConnection extends RUDPConnection {
	private ServerState state;

	private RUDPServerCallback serverCallback;
	private boolean connectTimeoutReached;
	private int connectTimeoutms = 5000;
	private Condition connectCondition;
	private Lock connectConditionLock;
	private RUDPPacket connectPacket;
	private Log log;
	
	public RUDPServerConnection(int port, RUDPServerCallback callback) {
		super(port, callback);
		state = ServerState.Closed;

		connectConditionLock = new ReentrantLock();
		connectCondition = connectConditionLock.newCondition();
		
		this.serverCallback = callback;
		log = LogFactory.getLog(this.getClass());
	}
	

	@Override
	public boolean isConnected() {
		return state == ServerState.Connected;
	}

	@Override
	protected void connectPhase() throws InterruptedException {
		RUDPPacket rudpPack;
		IPPacket rudpPackIP;		
		
		connectConditionLock.lock();
		state = ServerState.AwaitingConnection;
		
		//Wait for an incoming SYN-Request
		log.debug("waiting for incoming connection");

		try
		{
			connectCondition.await();
		}
		catch(InterruptedException ex)
		{
			connectConditionLock.unlock();
			throw ex;
		}
		
		//SYN received:
		
		if(maxSegmentSize != ((RUDP_SYNPacket)connectPacket).getMax_segment_size())
		{
			maxSegmentSize = ((RUDP_SYNPacket)connectPacket).getMax_segment_size();
			appReadBuffer = new byte[maxSegmentSize];
		}

		
		
		//Send SYNACK:
		lastSequenceNrSent = 123;
		rudpPack = new RUDP_SYNPacket(true, (byte)(lastSequenceNrSent), (byte)(connectPacket.getSeq_num()), (short)remotePort, (short)port);
		
		rudpPackIP = IPPacket.createDefaultIPPacket(IPPacket.RUDP_PROTOCOL, (short)0, Network.ip, remoteIP, rudpPack.getPacket());
		transportLayer.sendPacket(rudpPackIP);
		log.debug("sending SYNACK");
		state = ServerState.SYNACKSent;

		//Wait for ACK
		Timer connectTimer = new Timer();
		connectTimer.schedule(new ClientConnectTimeout(), connectTimeoutms);
		
		try {
			while(true)
			{
				connectTimeoutReached = false;
				connectCondition.await();
				
				//TODO:additional checks necessary!!!
				if(connectTimeoutReached || connectPacket.getAck_num() == (lastSequenceNrSent))
					break;
				else if(connectPacket.getAck_num() != lastSequenceNrSent)
					log.warn("received packet with invalid ack-nr(" + connectPacket.getAck_num() + ") instead of " + (lastSequenceNrSent));
			}
		} catch (InterruptedException e) {
			connectTimer.cancel();
			connectConditionLock.unlock();
			throw e;
		}
		
		if(connectTimeoutReached)
		{
			//retry...
			log.warn("connectTimeoutReached reached, while waiting for ACK");
			state = ServerState.AwaitingConnection;
			connectConditionLock.unlock();
			return;
		}
		
		state = ServerState.Connected;
		
		connectTimer.cancel();
		connectConditionLock.unlock();
		
		log.debug("We're connected now!!!");
		initForNewConnection();
		serverCallback.clientConnected(remoteIP);
	}
	
	/**
	 * starts the server, so that it will be available for clients.
	 */
	public void startServer() {
		transportLayer.addRUDPConnection(this);
		state = ServerState.AwaitingConnection;
		startThread();
	}
	
	/**
	 * stops the server and closes an active connection (if connected...)
	 */
	public void stopServer() {
		if(isConnected())
		{
			disconnect();
		}
		
		//TODO: maybe we should also handle the state if the Server is currently
		//performing a three-way handshake 
		
		transportLayer.removeRUDPConnection(this);
		state = ServerState.Closed;
	}


	@Override
	protected void connectPhasePacketReceived(RUDPPacket packet, String srcIP) {
		log.debug("received a Packet");
		connectConditionLock.lock();

		if(packet instanceof RUDP_SYNPacket && state == ServerState.AwaitingConnection)
		{
			//Some checks ...
			connectPacket = packet;
			remoteIP = srcIP;
			remotePort = connectPacket.getSrc_port();
			
			log.debug("received SYN Packet");
			connectCondition.signalAll();
		}
		else if(packet instanceof RUDP_ACKPacket && state == ServerState.SYNACKSent)
		{
			//Some checks ...
			connectPacket = packet;
			
			log.debug("received ACK Packet");
			connectCondition.signalAll();
		}
		connectConditionLock.unlock();
	}
	
	
	
	private class ClientConnectTimeout extends TimerTask
	{

		@Override
		public void run() {
			
			connectConditionLock.lock();
			connectTimeoutReached = true;
			connectCondition.signalAll();
			connectConditionLock.unlock();
		}
	}
	
	@Override
	protected void disconnect(boolean sendRST) {
		if(isConnected() && sendRST)
		{
			RUDPPacket rudpPack;
			IPPacket rudpPackIP;
			
			
			rudpPack = new RUDP_RSTPacket((short)remotePort, (short)port, (byte)0,(byte)0);
			
			rudpPackIP = IPPacket.createDefaultIPPacket(IPPacket.RUDP_PROTOCOL, (short)0, Network.ip, remoteIP, rudpPack.getPacket());
			transportLayer.sendPacket(rudpPackIP);
		}
		
		connectConditionLock.lock();
		state = ServerState.AwaitingConnection;
		
		nulDaemon.stop();
		
		//to interrupt the current flow in the thread ...
		interruptThread();
		connectConditionLock.unlock();
	}
}
