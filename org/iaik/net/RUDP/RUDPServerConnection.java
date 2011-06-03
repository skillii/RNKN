package org.iaik.net.RUDP;

public class RUDPServerConnection extends RUDPConnection {
	private ServerState state;
	
	public RUDPServerConnection() {
		state = ServerState.Closed;
	}
	
	void close() {
	}

	@Override
	public boolean isConnected() {
		return state == ServerState.Connected;
	}

	@Override
	public void connectPhase() {
		
	}
}
