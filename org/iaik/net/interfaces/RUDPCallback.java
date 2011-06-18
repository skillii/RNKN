package org.iaik.net.interfaces;

import org.iaik.net.RUDP.ConnectionCloseReason;

public interface RUDPCallback {
	/**
	 * this callback method will be called new data is available.
	 */
	void DataReceived();
	
	/**
	 * will be called if the connection is closed by the remote
	 * 
	 * @param reason the reason why the connection has been closed
	 */
	void ConnectionClosed(ConnectionCloseReason reason);
}
