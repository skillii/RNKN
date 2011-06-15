package org.iaik.net.interfaces;

public interface RUDPCallback {
	/**
	 * this callback method will be called new data is available.
	 */
	void DataReceived();
	
	/**
	 * will be called if the connection is closed by the remote
	 */
	void ConnectionClosed();
}
