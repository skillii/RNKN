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
	private int connectTimeoutms = 1000;
	private Condition connectCondition;
	private Lock connectConditionLock;
	private RUDPPacket connectPacket;
	private Log log;
	
	public RUDPServerConnection(int port, RUDPServerCallback callback) {
		super(port, callback);
		state = ServerState.Closed;

		connectConditionLock = new ReentrantLock();
		connectCondition = connectConditionLock.newCondition();
		
		
		log = LogFactory.getLog(this.getClass());
	}
	

	@Override
	public boolean isConnected() {
		return state == ServerState.Connected;
	}

	@Override
	protected void connectPhase() {
		RUDPPacket rudpPack;
		IPPacket rudpPackIP;		
		
		//Wait for an incoming SYN-Request
		log.debug("waiting for incoming connection");
		try {
			connectConditionLock.lock();
			connectCondition.await();
			connectConditionLock.unlock();
			
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		
		//Send SYNACK:
		lastSequenceNrSent = 123;
		rudpPack = new RUDP_SYNPacket(true, (byte)lastSequenceNrSent, (byte)0, (short)remotePort, (short)port, new byte[1]);
		
		rudpPackIP = IPPacket.createDefaultIPPacket(IPPacket.RUDP_PROTOCOL, (short)0, Network.ip, remoteIP, rudpPack.getPacket());
		transportLayer.sendPacket(rudpPackIP);
		log.debug("sending SYNACK");

		//Wait for ACK
		Timer connectTimer = new Timer();
		connectTimer.schedule(new ClientConnectTimeout(), connectTimeoutms);
		
		try {
			while(true)
			{
				connectConditionLock.lock();
				connectCondition.await();
				connectConditionLock.unlock();
				
				//TODO:additional checks necessary!!!
				if(connectPacket.getAck_num() == lastSequenceNrSent)
					break;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		
		if(connectTimeoutReached)
		{
			//retry...
			state = ServerState.AwaitingConnection;
			return;
		}
		
		connectTimer.cancel();
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
			//Close Connection...
		}
		
		//TODO: maybe we should also handle the state if the Server is currently
		//performing a three-way handshake 
		
		transportLayer.removeRUDPConnection(this);
		state = ServerState.Closed;
	}


	@Override
	protected void connectPhasePacketReceived(RUDPPacket packet) {
		if(packet instanceof RUDP_SYNPacket)
		{
			connectConditionLock.lock();
			//Some checks ...
			connectPacket = packet;
			log.debug("received SYN Packet");
			connectCondition.signal();
			connectConditionLock.unlock();

		}
	}
	
	private class ClientConnectTimeout extends TimerTask
	{

		@Override
		public void run() {
			
			connectConditionLock.lock();
			connectTimeoutReached = true;
			connectCondition.signal();
			connectConditionLock.unlock();
		}
	}
}
