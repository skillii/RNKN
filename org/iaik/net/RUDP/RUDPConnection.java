package org.iaik.net.RUDP;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.Network;
import org.iaik.net.factories.TransportLayerFactory;
import org.iaik.net.interfaces.RUDPCallback;
import org.iaik.net.interfaces.TransportLayer;
import org.iaik.net.packets.IPPacket;
import org.iaik.net.packets.rudp.*;
import org.iaik.net.utils.NetUtils;

public abstract class RUDPConnection implements Runnable, NULDaemonCallback {
	protected int port;
	protected int remotePort;
	private Thread thread;
	protected String remoteIP;
	private RUDPCallback callback;
	protected int lastSequenceNrSent = 0;
	protected TransportLayer transportLayer;
	//---Receive values---
	protected int nextPackageExpected;
	protected int lastPackageRcvd;
	protected int maxSegmentSize;
	protected int receiveBufferLength;
	protected byte[] appReadBuffer = new byte[maxSegmentSize]; 
	protected int appReadBLoad;
	protected RUDP_DTAPacket[] receivePacketBuffer;
	private boolean bStopThread = false;
	
	//NUL stuff
	protected NULDaemon nulDaemon;
	protected final int nullCycleValue = 1000000;
	protected final int nullTimeoutValue = 150000;
	
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
	 * @return byte array containing the data
	 */
	public byte[] getReceivedData(int maxbytes)
	{
		int maxReadingBytes; 
		int returnBufferLength;
		
		log.debug("Entered getReceived Data");
		
		maxReadingBytes = dataToRead();
		
		

		
		// returnBufferLength = min ( maxbytes , maxReadingBytes)
		if(maxbytes > maxReadingBytes)
			returnBufferLength = maxReadingBytes;
		else
			returnBufferLength = maxbytes;
		
		byte[] returnBuffer = new byte[returnBufferLength];			//create ReturnBuffer
		
		// if returnBuffer <= AppReadBuffer fuelle returnBuffer mit appReadBuffer nach vorneschieben
		if(returnBufferLength <= appReadBLoad)
		{
			returnBuffer = NetUtils.insertData(returnBuffer,  appReadBuffer, 0, returnBufferLength);
			appReadBLoad = (maxSegmentSize-returnBufferLength)-1;
			appReadBuffer = NetUtils.insertData(new byte[maxSegmentSize], appReadBuffer, 0, returnBufferLength, appReadBLoad);
		}
		
		// else 
		// 		appReadBuffer in returnBuffer kopieren, ggf. in schleife von packeten weiter in returnBuffer 
		//	letztes Packet in schleife unvollstaendig gelesen rest in appread buffer
		else
		{
			returnBuffer = NetUtils.insertData(returnBuffer,  appReadBuffer, 0, appReadBLoad);			//Load content from appReadBuffer to returnBuffer
			int offset = appReadBLoad;
			appReadBLoad = (maxSegmentSize-returnBufferLength)-1;
			appReadBuffer = NetUtils.insertData(new byte[maxSegmentSize], appReadBuffer, 0, returnBufferLength, appReadBLoad);		//clean  appReadBuffer (fill a empty buffer with nothing)
			int i;
			
			for(i=0; (offset + NetUtils.toInt(receivePacketBuffer[i].getPacket_length()) ) < returnBufferLength; i++ )	//Load whole packages to receiveBuffer
			{
				returnBuffer = NetUtils.insertData(returnBuffer, receivePacketBuffer[i].getPacket(), offset);
				offset += NetUtils.toInt(receivePacketBuffer[i].getPacket_length());
			}
			
			returnBuffer = NetUtils.insertData(returnBuffer, receivePacketBuffer[i].getPacket(), offset, (returnBufferLength-offset));		// Load data from the package which must be splitted
			appReadBLoad = (NetUtils.toInt(receivePacketBuffer[i].getPacket_length()) - (returnBufferLength-offset));						// Load rest of package to appReadBuffer
			appReadBuffer = NetUtils.insertData(new byte[maxSegmentSize], receivePacketBuffer[i].getPacket(), 0, (returnBufferLength+offset), appReadBLoad );
			
			
			// shift the packages through the receivePacketBuffer
			int start, shifty;
			for( start=0, shifty=i; shifty<receiveBufferLength ; shifty++, start++ )
				receivePacketBuffer[start] = receivePacketBuffer[shifty];
			//fill rest with nothing
			for(; start <receiveBufferLength ; start++)
				receivePacketBuffer[start] = null;
		}
		
		//return the data
		return returnBuffer;
	}
	/**
	 * dataToRead() returns the an integer value with the number of Bytes to read
	 * 				
	 * @return		maxReadingBytes
	 */
	
	public int dataToRead()
	{
		int maxReadingBytes = appReadBLoad;
		
		// maxReadingBytes berechnen mit appReadBuffer + schleife ueber packete von payload( nextExpPAC -1 || nextEXPPAC == lastRCVD)
		if(nextPackageExpected == lastPackageRcvd)
			for(int i=0; i < nextPackageExpected; i++)
				maxReadingBytes += NetUtils.toInt(receivePacketBuffer[i].getPacket_length());
		else
			for(int i=0; i < (nextPackageExpected-1); i++)			// stops at i = nextPackageExpected - 2 
				maxReadingBytes += NetUtils.toInt(receivePacketBuffer[i].getPacket_length());
		
		return maxReadingBytes;
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
		nextPackageExpected = 0;
		lastPackageRcvd = 0;
		maxSegmentSize = 4096;
		receiveBufferLength = 15;
		appReadBuffer = new byte[maxSegmentSize]; 
		appReadBLoad = 0;
		receivePacketBuffer = new RUDP_DTAPacket[receiveBufferLength];
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
			
			else if(packet instanceof RUDP_DTAPacket)					// a Data Package
			{
				log.debug("Entered  Packed Buffering");
				
				RUDP_DTAPacket dtaPacket = (RUDP_DTAPacket)packet;
				int diff;
				int advertisedWindow;
				
				if(receivePacketBuffer[0] == null)
				{
					log.debug("Packed Buffering: store first Packed");
					receivePacketBuffer[0] = dtaPacket;
					advertisedWindow = calcAdvWinSize();
					sendACK(packet,advertisedWindow);
					
				}
				else
				{
					log.debug("Packed Buffering: calc diff");
					// neue pos sequenzdiff
					diff = sequenceDiff(dtaPacket.getSeq_num(), receivePacketBuffer[0].getSeq_num());

					// diff<0 send ack throw away
					if(diff < 0)
					{
						log.debug("Packed Buffering: Got Old Packed, throw away but send ACK");
						advertisedWindow = calcAdvWinSize();
						sendACK(packet,advertisedWindow);
						return;						
					}
					
					// diff > max buffline throw away
					if(diff > receiveBufferLength)
					{
						log.error("Packet Buffer: Offerflow!");
						return;
					}
					// speichern, updaten von nextExpectedPacket und lastRcvdpacket
					else
					{
						receivePacketBuffer[diff] = dtaPacket;
						int oldNPE = nextPackageExpected;
						//Update nextPackageExpected & lastPackageRcvd
						for(nextPackageExpected = 0; (receivePacketBuffer[nextPackageExpected] != null) || (nextPackageExpected == receiveBufferLength); nextPackageExpected++);
						for(lastPackageRcvd = receiveBufferLength; (receivePacketBuffer[lastPackageRcvd] == null) || (lastPackageRcvd == -1); lastPackageRcvd--);
						nextPackageExpected--;
						lastPackageRcvd++;
						// ACK Packages
						if(oldNPE != nextPackageExpected)		// nextExpectedPacket changed -> dataReceived aufrufen & ACK senden
						{
							advertisedWindow = calcAdvWinSize();
							sendACK(receivePacketBuffer[nextPackageExpected],advertisedWindow);
						}
					}				
					log.debug("callback for DATARECEIVED");
					
					
				}
				callback.DataReceived();
				
				
			}
		}
	}
	
	/**
	 * Calculate AdvertisedWindowSize
	 */
	protected int calcAdvWinSize()
	{
		return (receiveBufferLength - ((nextPackageExpected -1) -lastPackageRcvd));
	}
	
	/**
	 * Calculates the real differenc between new and first package 
	 * 
	 * @ b1   sequenzenr. of new package
	 * @ b2   sequenzenr. if old package
	 * 
	 * @return difference
	 */
	
	private int sequenceDiff(byte b1, byte b2)
	{
		int ib1 = NetUtils.toInt(b1);
		int ib2 = NetUtils.toInt(b2);
		int diff;
		
		if((ib1-ib2) < -127)
		{
			diff = 255 - ib2 + ib1;
			
		}
		else 
		{
			diff = ib1 - ib2;
		}
		
		return diff;
	}
	
	/**
	 * Send an Acknowledge package
	 * @param packet 		package to be acknowledged
	 * @param adveWinSize	Advertised Window size
	 */
	private void sendACK(RUDPPacket packet, int adveWinSize)
	{
		RUDPPacket rudpPack;
		IPPacket rudpPackIP;
		rudpPack = new RUDP_ACKPacket((short)remotePort, (short)port, (byte)0, (byte)(packet.getSeq_num()), (byte)adveWinSize);
		
		rudpPackIP = IPPacket.createDefaultIPPacket(IPPacket.RUDP_PROTOCOL, (short)0, Network.ip, remoteIP, rudpPack.getPacket());
		transportLayer.sendPacket(rudpPackIP);
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
