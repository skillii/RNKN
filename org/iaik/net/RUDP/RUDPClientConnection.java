package org.iaik.net.RUDP;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.interfaces.RUDPClientCallback;
import org.iaik.net.packets.rudp.*;


public class RUDPClientConnection extends RUDPConnection {
	private ClientState state;
	private RUDPClientCallback clientCallback;
	private final int maxConnectRetries = 3;
	private final int connectTimeoutms = 1000;
	private Semaphore connectSem;
	private Log log;
	private boolean connectTimeoutReached = false;
	
	private RUDP_SYNPacket synAckReceived;

	public RUDPClientConnection(int port, String remoteIP, int remotePort, RUDPClientCallback callback) {
		super(port, callback);
		super.remotePort = remotePort;
		state = ClientState.Disconnected;
		this.clientCallback = callback;
		connectSem = new Semaphore(0);
		log = LogFactory.getLog(this.getClass());
	}
	
	@Override
	public boolean isConnected() {
		return state == ClientState.Connected;
	}

	@Override
	public void connectPhase() {
		
	}
	
	/**
	 * connects to the specified server.
	 */
	public void connect()
	{
		int connectTry;
		//TODO: 3 way handshake:
		
		for(connectTry = 0; connectTry < maxConnectRetries; connectTry++)
		{
			//Send SYN:
			lastSequenceNrSent = 123;
			
			state = ClientState.SYNSent;
			
			Timer connectTimer = new Timer();
			connectTimer.schedule(new ConnectTimeout(), connectTimeoutms);
			
			//Wait for SYNACK
			try {
				while(true)
				{
					connectSem.acquire();
					
					//TODO:additional checks necessary!!!
					
					if(synAckReceived.getAck_num() == lastSequenceNrSent)
						break;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			
			if(connectTimeoutReached)
			{
				//retry...
				state = ClientState.Disconnected;
				continue;
			}
			
			connectTimer.cancel();
			
			//SYNACK received, so Send ACK:
			
			state = ClientState.Connected;
		}
	}
	
	private class ConnectTimeout extends TimerTask
	{

		@Override
		public void run() {
			connectTimeoutReached = true;
			connectSem.release();
		}
	
	}

	@Override
	protected void connectPhasePacketReceived(RUDPPacket packet) {
		if(packet instanceof RUDP_SYNPacket)
		{
			RUDP_SYNPacket synPacket = (RUDP_SYNPacket)packet;
			if(synPacket.isAck())
			{
				//some additional checks!
				synAckReceived = synPacket;
				connectSem.release();
			}
		}
	}
}
