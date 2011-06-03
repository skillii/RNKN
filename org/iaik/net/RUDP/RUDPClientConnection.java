package org.iaik.net.RUDP;

public class RUDPClientConnection extends RUDPConnection {
	private ClientState state;

	public RUDPClientConnection() {
		state = ClientState.Disconnected;
	}
	
	@Override
	public boolean isConnected() {
		return state == ClientState.Connected;
	}

	@Override
	public void connectPhase() {
		
	}
}
