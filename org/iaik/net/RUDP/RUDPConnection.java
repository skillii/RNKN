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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.TimerTask;


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
	
	
	//---Sender Stuff---
	protected int lastPackageAcked;
	protected int lastPackageSent;
	protected int lastPackageWritten;
	protected final int seqNrsAvailable = 128;
	protected final int sendBufferLength = 16;
	protected final int ackTimeout = 3000;  // ACK-Timeout in ms
	protected final int ackTimeoutCheckInterval = 100;  // ACK-Timeout Check Interval in ms
	protected int appWriteBufferUsed;
	protected byte[] appWriteBuffer;  // Nagle-Buffer for incomplete packages
	protected int sendPacketBufferElements;
	protected SlidingWindowPacket[] sendPacketBuffer;  // Ringbuffer
	protected int unackedPackets;
	protected Semaphore sendBufferFullSem;
	protected Semaphore sendBufferEmptySem;
	protected int senderAdvertisedWindow;
	protected Lock advLock;
	protected Condition advWinFree;
	
	
	//NUL stuff
	protected NULDaemon nulDaemon;
	protected final int nullCycleValue = 3000000;
	protected final int nullTimeoutValue = 15000000;

	
	private Log log;
	
	
	RUDPConnection(int port, RUDPCallback callback) {
		this.port = port;
		this.callback = callback;
		transportLayer = TransportLayerFactory.getInstance();
		log = LogFactory.getLog(this.getClass());
	}
	
	/**
	 * sends data over the established RUDPConnection
     *
	 * @param data byte array to send
	 * generates data packets with Nagle algorithm
	 * if the send buffer is full, the thread is blocked
	 * sending itself happens in the run method
	 */
	public void sendData(byte[] data) {
		if(data == null)
			return;
		
		int i = 0;
		RUDP_DTAPacket dataPacket;
		byte[] payload;
		
		log.debug("sendData called, data length = " + data.length);
		
		
		if(appWriteBufferUsed > 0)  // Nagle buffer is not empty -> fill it up
		{
			if(appWriteBufferUsed + data.length >= maxSegmentSize)  // we can fill the buffer
			{
				log.debug("sending: nagle buffer in use, we can fill and send");
				
				payload = Arrays.copyOf(appWriteBuffer, maxSegmentSize);
				
				for(i = 0; i + appWriteBufferUsed < maxSegmentSize; i++)
					payload[i + appWriteBufferUsed] = data[i];
				
				appWriteBufferUsed = 0;
				
				dataPacket = new RUDP_DTAPacket((short)remotePort, (short)port, payload, (byte)0, (byte)0);
				
				addToSendBuffer(dataPacket);
				
				if(appWriteBufferUsed + data.length == maxSegmentSize)  // we could exactly fill the buffer
				{
					log.debug("sending: nagle buffer could be filled exactly, finished");
					return;  // no more packets to send, so FEIERABEND! TODO: is this neccessary??
				}
			}
			else  // Nagle buffer again isnt full
			{
				for(i = 0; i < data.length; i++)
				{
					appWriteBuffer[i + appWriteBufferUsed] = data[i];
					appWriteBufferUsed += data.length;
				}
				
				if(unackedPackets > 0)  // theres data in flight -> we wait for more data before sending
				{
					log.debug("sending: nagle buffer isnt full and theres data in flight, we wait");
					return;
				}
				else  // we have no unacked Packets -> send the buffer and packet
				{
					log.debug("sending: nagle buffer isnt full but there are no unacked pckgs, we send");
					
					payload = Arrays.copyOfRange(appWriteBuffer, 0, appWriteBufferUsed);
					appWriteBufferUsed = 0;
					
					dataPacket = new RUDP_DTAPacket((short)remotePort, (short)port, payload, (byte)0, (byte)0);
					
					addToSendBuffer(dataPacket);
					return;
				}
			}
		}
		else if(data.length < maxSegmentSize)  // Nagle buffer is empty and data chunk is too small -> put in Nagle buffer
		{
			if(unackedPackets > 0)  // theres data in flight -> Nagle says store
			{
				log.debug("sending: nagle buffer is empty, chunk too small to send and data in flight, we wait");
				
				appWriteBuffer = Arrays.copyOf(data, appWriteBuffer.length);
				appWriteBufferUsed = data.length;
				return;
			}
			else  // Nagle says send
			{
				log.debug("sending: nagle buffer is empty, chunk too small and no unacked pckgs, we send");
				
				payload = data.clone();
				
				dataPacket = new RUDP_DTAPacket((short)remotePort, (short)port, payload, (byte)0, (byte)0);
				
				addToSendBuffer(dataPacket);
				return;
			}
		}
		
		for(; i < data.length; i += maxSegmentSize)
		{
			if(data.length - i < maxSegmentSize)  // we cant produce a complete packet -> Nagle is our friend
			{
				if(unackedPackets > 0)  // theres data in flight -> Nagle says store
				{
					log.debug("sending: last package of data goes in the nagle buffer");
					
					appWriteBuffer = Arrays.copyOfRange(data, i, i + appWriteBuffer.length);
					appWriteBufferUsed = data.length;
				}
				else  // Nagle says send
				{
					log.debug("sending: last package of data goes on the wire");
					
					payload = Arrays.copyOfRange(data, i, data.length);
					
					dataPacket = new RUDP_DTAPacket((short)remotePort, (short)port, payload, (byte)0, (byte)0);
					
					addToSendBuffer(dataPacket);
				}
			}
			else  // standard package sending
			{
				log.debug("sending: full package goes on the wire");
				
				payload = Arrays.copyOfRange(data, i, i + maxSegmentSize);
				
				dataPacket = new RUDP_DTAPacket((short)remotePort, (short)port, payload, (byte)0, (byte)0);
				
				addToSendBuffer(dataPacket);
			}
		}
	}
	
	/**
	 * tries to add a packet to the send buffer
	 * if the send buffer is full, the calling thread is blocked
	 * implements the sliding window algorithm
	 * @param packet the packet to send
	 */
	public void addToSendBuffer(RUDP_DTAPacket packet)
	{
		log.debug("addToSendBuffer: trying to add, seqnr " + packet.getSeq_num());
		
		try
		{
			sendBufferFullSem.acquire();
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
		
		synchronized(sendPacketBuffer)
		{
			// calculate sequence number from parameters
			lastPackageWritten++;
			if(lastPackageWritten >= seqNrsAvailable)
				lastPackageWritten = 0;
			
			packet.setSeq_num((byte)lastPackageWritten);
		
			SlidingWindowPacket swPacket = new SlidingWindowPacket(packet, 0);
			
			log.debug("addToSendBuffer: adding packet to FIFO, seqnr " + packet.getSeq_num());
			
			// add to send FIFO
			sendPacketBuffer[lastPackageWritten % sendBufferLength] = swPacket;
			
			sendPacketBufferElements++;
		}
		
		sendBufferEmptySem.release();
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
			//appReadBLoad = (maxSegmentSize-returnBufferLength)-1;
			appReadBLoad = appReadBLoad -returnBufferLength;
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
			
			for(i=0; (offset + receivePacketBuffer[i].getPayload().length) < returnBufferLength; i++ )	//Load whole packages to receiveBuffer
			{
				returnBuffer = NetUtils.insertData(returnBuffer, receivePacketBuffer[i].getPayload(), offset);
				offset += receivePacketBuffer[i].getPayload().length;
			}
			
			returnBuffer = NetUtils.insertData(returnBuffer, receivePacketBuffer[i].getPayload(), offset, (returnBufferLength-offset));		// Load data from the package which must be splitted
			appReadBLoad = (receivePacketBuffer[i].getPayload().length - (returnBufferLength-offset));						// Load rest of package to appReadBuffer
			appReadBuffer = NetUtils.insertData(new byte[maxSegmentSize], receivePacketBuffer[i].getPayload(), 0, ( receivePacketBuffer[i].getPayload().length-appReadBLoad), appReadBLoad );
			
			i++;
			// shift the packages through the receivePacketBuffer
			int start, shifty;
			for( start=0, shifty=i; shifty<receiveBufferLength ; shifty++, start++ )
				receivePacketBuffer[start] = receivePacketBuffer[shifty];
			//fill rest with nothing
			for(; start <receiveBufferLength ; start++)
				receivePacketBuffer[start] = null;
			updateRecvValues();
		}
		
		log.debug("return Data");
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
		if(nextPackageExpected == (lastPackageRcvd+1))
		{
			 if(nextPackageExpected == 0)
				 return maxReadingBytes;
			 
			int i=0;
			do
			{
				maxReadingBytes += receivePacketBuffer[i].getPayload().length;
				 i++;
			}while(i < nextPackageExpected);
		}
		else
			for(int i=0; i < (nextPackageExpected-1); i++)			// stops at nextPackageExpected - 2 
				maxReadingBytes += receivePacketBuffer[i].getPayload().length;
		
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
		// receiver init
		nextPackageExpected = 0;
		lastPackageRcvd = 0;
		maxSegmentSize = 100;
		receiveBufferLength = 15;
		appReadBuffer = new byte[maxSegmentSize]; 
		appReadBLoad = 0;
		receivePacketBuffer = new RUDP_DTAPacket[receiveBufferLength];

		// sender init
		lastPackageAcked = lastPackageSent = lastPackageWritten = lastSequenceNrSent;
		appWriteBufferUsed = 0;  // write Buffer is empty in the beginning
		unackedPackets = 0;
		appWriteBuffer = new byte[maxSegmentSize];
		sendPacketBuffer = new SlidingWindowPacket[sendBufferLength];
		sendPacketBufferElements = 0;
		sendBufferFullSem = new Semaphore(sendBufferLength);
		sendBufferEmptySem = new Semaphore(0);
		senderAdvertisedWindow = sendBufferLength;
		advLock = new ReentrantLock();
		advWinFree = advLock.newCondition();
		
		Timer sentPacketTimeoutTimer = new Timer();
		sentPacketTimeoutTimer.schedule(new SentPackageTimeoutChecker(), ackTimeoutCheckInterval);

		// NUL packet init
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
					sendBufferEmptySem.acquire();
					
					advLock.lock();
					
					while(true)
					{
						int currentWindow;
						
						if(lastPackageAcked <= lastPackageSent)
							currentWindow = lastPackageSent - lastPackageAcked;
						else
							currentWindow = seqNrsAvailable - (lastPackageAcked - lastPackageSent);
					
						if(currentWindow > senderAdvertisedWindow)
						{
							log.debug("senderThread: advertised window reached, blocking");
							advWinFree.await();
						}
						else
						{
							log.debug("senderThread: advertised window free, proceeding");
							break;
						}
					}
					
					SlidingWindowPacket swPacket;
					
					synchronized(sendPacketBuffer)
					{
						lastPackageSent++;
						if(lastPackageSent >= seqNrsAvailable)
							lastPackageSent = 0;
						
						swPacket = sendPacketBuffer[lastPackageSent % sendBufferLength];
						
						//sendPacketBufferElements--;
						
						swPacket.setTimeout(System.currentTimeMillis() + ackTimeout);
						
						unackedPackets++;
					}
					
					IPPacket rudpDataPacketIP = IPPacket.createDefaultIPPacket(IPPacket.RUDP_PROTOCOL, (short)0, Network.ip, remoteIP, swPacket.getDataPacket().getPacket());
					
					transportLayer.sendPacket(rudpDataPacketIP);
					
					log.debug("senderThread: sent packet, seqnr " + swPacket.dataPacket.getSeq_num());
					
					advLock.unlock();
					
					//sendBufferFullSem.release();
					
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
				RUDP_ACKPacket ackPacket = (RUDP_ACKPacket) packet;
				
				if(ackPacket.getAck_num() > lastPackageAcked && ackPacket.getAck_num() < lastPackageSent)
				{
					log.debug("receiver: got an ack for seqnr " + ackPacket.getAck_num());
					
					synchronized(sendPacketBuffer)
					{
						while(lastPackageAcked <= ackPacket.getAck_num())  // all packages < ackPacket are acked
						{
							lastPackageAcked++;
							if(lastPackageAcked >= seqNrsAvailable)
								lastPackageAcked = 0;
						
							log.debug("receiver: removing acked packet from window, seqnr " + lastPackageAcked);
							
							unackedPackets--;
							sendPacketBufferElements--;
							
							sendBufferFullSem.release();
						}
					}
				}
				else
				{
					log.debug("receiver: got an ack for a packet outside of window, ignoring");
				}
				
				advLock.lock();
				senderAdvertisedWindow = (int) ackPacket.getAvertisedWindowSize();
				if(senderAdvertisedWindow > 0)
				{
					log.debug("receiver: got a new advertised window size > 0, signaling. advw " + senderAdvertisedWindow);
					advWinFree.signal();
				}
				advLock.unlock();
					
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
					updateRecvValues();
					
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
						updateRecvValues();
						
						// ACK Packages
						if(oldNPE != nextPackageExpected)		// nextExpectedPacket changed -> dataReceived aufrufen & ACK senden
						{
							advertisedWindow = calcAdvWinSize();
							sendACK(receivePacketBuffer[nextPackageExpected-1],advertisedWindow);
						}
					}				

				}
				log.debug("callback for DATARECEIVED");
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
	 * Updates the Values nextPackageExpected & lastPackageRcvd
	 */
	private void updateRecvValues()
	{
		for(nextPackageExpected = 0; (receivePacketBuffer[nextPackageExpected] != null) && (nextPackageExpected != receiveBufferLength-1); nextPackageExpected++);
		for(lastPackageRcvd = (receiveBufferLength - 1); (receivePacketBuffer[lastPackageRcvd] == null) && (lastPackageRcvd != 0); lastPackageRcvd--);
		log.debug("Update of nextPackageExpected: " + nextPackageExpected);
		log.debug("Update of lastPackageRcvd: " + lastPackageRcvd);
		if((nextPackageExpected == (receiveBufferLength-1)) && (receivePacketBuffer[nextPackageExpected] != null))
			nextPackageExpected++;
		if((lastPackageRcvd == 0) && (receivePacketBuffer[lastPackageRcvd] == null))
			lastPackageRcvd--;
			
		
	}
	/**
	 * Calculates the real differenc between new and first package 
	 * 
	 * @ b1   sequenzenr. of new package
	 * @ b2   sequenzenr. of old package
	 * 
	 * @return difference
	 */
	
	private int sequenceDiff(byte b1, byte b2)
	{
		int ib1 = NetUtils.toInt(b1);
		int ib2 = NetUtils.toInt(b2);
		int diff;
		
		if((ib1-ib2) < -63)
		{
			diff = 128 - ib2 + ib1;
			
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


	/**
	 * this class is used to check if timeouts of sent but unacked
	 * packages are reached
	 * if a timeout is reached, the package is re-sent
	 */
	public class SentPackageTimeoutChecker extends TimerTask
	{

		@Override
		public void run()
		{
			int packetNr;
			
			synchronized(sendPacketBuffer)
			{
				// run through all unacked packets and check timeouts
				for(packetNr = lastPackageAcked + 1; packetNr < lastPackageAcked + 1 + unackedPackets; packetNr++)
				{
					if(System.currentTimeMillis() > sendPacketBuffer[packetNr % sendBufferLength].timeout)
					{
						// whoops, timeout reached! ALARM!! re-sent and set new timeout
						IPPacket rudpDataPacketIP = IPPacket.createDefaultIPPacket(IPPacket.RUDP_PROTOCOL, (short)0, Network.ip, remoteIP, sendPacketBuffer[packetNr % sendBufferLength].getDataPacket().getPacket());
						sendPacketBuffer[packetNr % sendBufferLength].timeout = System.currentTimeMillis() + ackTimeout;
						
						log.debug("sentpackagetimeoutchecker: found unacked package with timeout, retrying. seqnr" + packetNr);
						
						transportLayer.sendPacket(rudpDataPacketIP);
					}
				}
			}
		}
	}
	
	@Override
	public void TimeoutReached() {
		disconnect(true);
		callback.ConnectionClosed(ConnectionCloseReason.NULTimeout);
	}
}
