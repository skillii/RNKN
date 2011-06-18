package org.iaik.net.RUDP;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.Network;
import org.iaik.net.factories.TransportLayerFactory;
import org.iaik.net.interfaces.RUDPCallback;
import org.iaik.net.interfaces.TransportLayer;
import org.iaik.net.packets.IPPacket;
import org.iaik.net.packets.rudp.*;

public abstract class RUDPConnection implements Runnable, NULDaemonCallback {
	protected int port;
	protected int remotePort;
	private Thread thread;
	protected String remoteIP;
	private RUDPCallback callback;
	protected int lastSequenceNrSent = 0;
	protected TransportLayer transportLayer;
	//---Receive values---
	protected int nextPackageExpected = 0;
	protected int lastPackageRcvd = 0;
	protected int maxSegmentSize = 4096;
	protected final int receiveBufferLength = 15;
	protected byte[] appReadBuffer = new byte[maxSegmentSize]; 
	protected RUDP_DTAPacket[] receivePacketBuffer = new RUDP_DTAPacket[receiveBufferLength];
	private boolean bStopThread = false;
	
	//NUL stuff
	protected NULDaemon nulDaemon;
	protected final int nullCycleValue = 3000;
	protected final int nullTimeoutValue = 15000;
	
	private Log log;
	
	RUDPConnection(int port, RUDPCallback callback) {
		this.port = port;
		this.callback = callback;
		transportLayer = TransportLayerFactory.getInstance();
		log = LogFactory.getLog(this.getClass());
	}
	
	/**
	 * sends data over the established RUDPConnection
	 */
	public void sendData(byte[] data) {
		
	}
	
	/**
	 * returns received data from this Connection
	 * @param maxbytes maximal count of bytes returned
	 * @return bytearray containing the data
	 */
	public byte[] getReceivedData(int maxbytes) {
		return null;
	}
	
	/**
	 * The Connection to the remote will be closed.
	 * @param sendRST if a RST Packet should be sent
	 */
	protected abstract void disconnect(boolean sendRST);

	/**
	 * The Connection to the remote will be closed.
	 */
	public void disconnect()
	{
		disconnect(true);
	}
	
	/**
	 * all the initialisation stuff, if a new connection is established should be done here!
	 * will be called by derived classes!
	 */
	protected void initForNewConnection()
	{
		nulDaemon = new NULDaemon(remoteIP, remotePort, port, nullCycleValue, nullTimeoutValue, this);
		nulDaemon.start();
	}
	
	@Override
	public void run() {
		while(!bStopThread)
		{
			try
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
					Thread.sleep(100);
				}
			}
			catch(InterruptedException ex)
			{
				continue;
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
	protected abstract void connectPhase() throws InterruptedException;
	
	/**
	 * this method will be called if a packet is received during receive phase.
	 * this is necessary, since the Client Implementation is different from the Server Implementation
	 * during the connect phase.
	 */
	protected abstract void connectPhasePacketReceived(RUDPPacket packet, String srcIP);
	
	/**
	 * This method will be called by TransportLayer, when a new Packet for this Connection arrives.
	 */
	public void packetReceived(RUDPPacket packet, String srcIP)
	{
		if(!isConnected())
		{
			connectPhasePacketReceived(packet, srcIP);
		}
		else
		{
			if(srcIP.equals(remoteIP) == false || packet.getSrc_port() != remotePort)
			{
				log.warn("received packet(" + srcIP + "," + packet.getSrc_port() + ", where remoteIP(" + remoteIP + ") or remotePort(" + remotePort + "doesn't match");
				return;
			}
			nulDaemon.packetReceived();
			
			if(packet instanceof RUDP_ACKPacket)
			{
				//TODO: tell packetSend that we got an ack
			}
			
			else if(packet instanceof RUDP_NULPacket)
			{
				//TODO: reset the hartbeat timer
			}
			
			else if(packet instanceof RUDP_RSTPacket)
			{
				disconnect(false);
				callback.ConnectionClosed(ConnectionCloseReason.RSTbyPeer);
			}
			
			else if(packet instanceof RUDP_DTAPacket)					//last possibility Data Packet
			{
				RUDP_DTAPacket dtaPacket = (RUDP_DTAPacket)packet;
				
				
			}
		}
	}
	
	/**
	 * starts the thread
	 */
	protected void startThread()
	{
		thread = new Thread(this);
		bStopThread = false;
		thread.start();
	}
	
	protected void stopThread()
	{
		bStopThread = true;
		thread.interrupt();
	}
	
	protected void interruptThread()
	{
		thread.interrupt();
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
	
	@Override
	public void TimeoutReached() {
		disconnect(true);
		callback.ConnectionClosed(ConnectionCloseReason.NULTimeout);
	}
}
