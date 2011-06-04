package org.iaik.net.RUDP;

import org.iaik.net.factories.TransportLayerFactory;
import org.iaik.net.interfaces.RUDPServerCallback;
import org.iaik.net.interfaces.TransportLayer;

public class RUDPServerConnection extends RUDPConnection {
	private ServerState state;
	private TransportLayer transportLayer;
	private RUDPServerCallback serverCallback;
	
	public RUDPServerConnection(int port, RUDPServerCallback callback) {
		super(port, callback);
		state = ServerState.Closed;
		transportLayer = TransportLayerFactory.getInstance();
	}
	

	@Override
	public boolean isConnected() {
		return state == ServerState.Connected;
	}

	@Override
	public void connectPhase() {
		//TODO: 3 way handshake ...
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
}
