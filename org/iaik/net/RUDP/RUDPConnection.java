package org.iaik.net.RUDP;

import org.iaik.net.interfaces.RUDPCallback;
import org.iaik.net.packets.rudp.RUDPPacket;

public abstract class RUDPConnection implements Runnable {
	protected int port;
	protected int remotePort;
	private Thread thread;
	protected String remoteIP;
	private RUDPCallback callback;
	protected int lastSequenceNrSent = 0;
	
	
	RUDPConnection(int port, RUDPCallback callback) {
		this.port = port;
		this.callback = callback;
	}
	
	/**
	 * sends data over the established RUDPConnection
	 */
	void sendData(byte[] data) {
		
	}
	
	/**
	 * returns received data from this Connection
	 * @param maxbytes maximal count of bytes returned
	 * @return bytearray containing the data
	 */
	byte[] getReceivedData(int maxbytes) {
		return null;
	}
	
	/**
	 * the Connection to the remote will be closed.
	 */
	void disconnect() {
		
	}

	@Override
	public void run() {
		while(true)
		{
			if(!isConnected())
			{
				//the connect phase differs from server to client, so
				//we make a polymorph call here.
				connectPhase();
				//maybe i shouldn't do the connecting stuff in this thread? and better just make a blocking
				//connect(...) call??? easier?
				continue;
			}
			else
			{
				//now we're connected so here we can do the data-send stuff...
			}
		}
	}
	
	/**
	 * returns the connection state
	 * 
	 * @return true if connected
	 */
	public abstract boolean isConnected();
	
	/**
	 * will be called by the connection Thread while isConnected() returns false
	 * should perform for instance in RUDPServerConnection the 3 way Handshake for incoming
	 * Client requests
	 */
	public abstract void connectPhase();
	
	/**
	 * this method will be called if a packet is received during receive phase.
	 * this is necessary, since the Client Implementation is different from the Server Implementation
	 * during the connect phase.
	 */
	protected abstract void connectPhasePacketReceived(RUDPPacket packet);
	
	/**
	 * This method will be called by TransportLayer, when a new Packet for this Connection arrives.
	 */
	public void packetReceived(RUDPPacket packet)
	{
		if(!isConnected())
		{
			connectPhasePacketReceived(packet);
		}
			
	}
	
	/**
	 * starts the thread
	 */
	protected void startThread()
	{
		thread = new Thread(this);
		thread.start();
	}
	
	/**
	 * returns the local port of the connection (TransportLayer needs this to
	 * decide which packet belongs to which RUDPConnection)
	 * 
	 * @return the port number of the connection
	 */
	public int getPort()
	{
		return port;
	}
}
