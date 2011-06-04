package org.iaik.net.RUDP;

import org.iaik.net.interfaces.RUDPClientCallback;


public class RUDPClientConnection extends RUDPConnection {
	private ClientState state;
	private RUDPClientCallback clientCallback;

	public RUDPClientConnection(int port, String remoteIP, int remotePort, RUDPClientCallback callback) {
		super(port, callback);
		super.remotePort = remotePort;
		state = ClientState.Disconnected;
		this.clientCallback = callback;
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
		//TODO: 3 way handshake:
	}
}
