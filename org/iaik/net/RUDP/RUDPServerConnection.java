package org.iaik.net.RUDP;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.iaik.net.factories.TransportLayerFactory;
import org.iaik.net.interfaces.RUDPServerCallback;
import org.iaik.net.interfaces.TransportLayer;
import org.iaik.net.packets.rudp.*;


public class RUDPServerConnection extends RUDPConnection {
	private ServerState state;
	private TransportLayer transportLayer;
	private RUDPServerCallback serverCallback;
	private boolean connectTimeoutReached;
	private int connectTimeoutms = 1000;
	private Semaphore connectSem;
	private RUDPPacket connectPacket;
	
	public RUDPServerConnection(int port, RUDPServerCallback callback) {
		super(port, callback);
		state = ServerState.Closed;
		transportLayer = TransportLayerFactory.getInstance();
		connectSem = new Semaphore(0);
	}
	

	@Override
	public boolean isConnected() {
		return state == ServerState.Connected;
	}

	@Override
	public void connectPhase() {
		//Wait for an incoming SYN-Request
		try {
			connectSem.acquire();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//Send SYNACK:
		lastSequenceNrSent = 123;

		//Wait for ACK
		Timer connectTimer = new Timer();
		connectTimer.schedule(new ClientConnectTimeout(), connectTimeoutms);
		
		try {
			while(true)
			{
				connectSem.acquire();
				
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
			//Some checks ...
			connectPacket = packet;
			connectSem.release();

		}
	}
	
	private class ClientConnectTimeout extends TimerTask
	{

		@Override
		public void run() {
			connectTimeoutReached = true;
			connectSem.release();
		}
	}
}
