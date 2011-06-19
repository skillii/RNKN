package org.iaik.net.RUDP;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.Network;
import org.iaik.net.factories.TransportLayerFactory;
import org.iaik.net.interfaces.RUDPCallback;
import org.iaik.net.interfaces.TransportLayer;
import org.iaik.net.packets.IPPacket;
import org.iaik.net.packets.rudp.*;
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
	protected int nextPackageExpected = 0;
	protected int lastPackageRcvd = 0;
	protected int maxSegmentSize = 100;
	protected final int receiveBufferLength = 15;
	protected byte[] appReadBuffer = new byte[maxSegmentSize]; 
	protected RUDP_DTAPacket[] receivePacketBuffer = new RUDP_DTAPacket[receiveBufferLength];
	private boolean bStopThread = false;
	
	
	//---Sender Stuff---
	protected int lastPackageAcked;
	protected int lastPackageSent;
	protected int lastPackageWritten;
	protected final int seqNrsAvailable = 256;
	protected final int sendBufferLength = 16;
	protected final int ackTimeout = 1000;  // ACK-Timeout in ms
	protected final int ackTimeoutCheckInterval = 100;  // ACK-Timeout Check Interval in ms
	protected int appWriteBufferUsed;
	protected byte[] appWriteBuffer;  // Nagle-Buffer for incomplete packages
	protected int sendPacketBufferElements;
	protected SlidingWindowPacket[] sendPacketBuffer;  // Ringbuffer
	protected int unackedPackets;
	protected Semaphore sendBufferFullSem;
	protected Semaphore sendBufferEmptySem;
	protected int senderAdvertisedWindow;
	protected Lock advLock = new ReentrantLock();
	protected Condition advWinFree = advLock.newCondition();
	
	
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
		
		if(appWriteBufferUsed > 0)  // Nagle buffer is not empty -> fill it up
		{
			if(appWriteBufferUsed + data.length >= maxSegmentSize)  // we can fill the buffer
			{
				payload = Arrays.copyOf(appWriteBuffer, maxSegmentSize);
				
				for(i = 0; i + appWriteBufferUsed < maxSegmentSize; i++)
					payload[i + appWriteBufferUsed] = data[i];
				
				appWriteBufferUsed = 0;
				
				dataPacket = new RUDP_DTAPacket((short)remotePort, (short)port, payload, (byte)0, (byte)0);
				
				addToSendBuffer(dataPacket);
				
				if(appWriteBufferUsed + data.length == maxSegmentSize)  // we could exactly fill the buffer
				{
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
					return;
				}
				else  // we have no unacked Packets -> send the buffer and packet
				{
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
				appWriteBuffer = Arrays.copyOf(data, appWriteBuffer.length);
				appWriteBufferUsed = data.length;
				return;
			}
			else  // Nagle says send
			{
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
					appWriteBuffer = Arrays.copyOfRange(data, i, i + appWriteBuffer.length);
					appWriteBufferUsed = data.length;
				}
				else  // Nagle says send
				{
					payload = Arrays.copyOfRange(data, i, data.length);
					
					dataPacket = new RUDP_DTAPacket((short)remotePort, (short)port, payload, (byte)0, (byte)0);
					
					addToSendBuffer(dataPacket);
				}
			}
			else  // standard package sending
			{
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
			
			// add to send FIFO			
			
			
			sendPacketBuffer[lastPackageWritten % sendBufferLength] = swPacket;
			
			sendPacketBufferElements++;
		}
		
		sendBufferEmptySem.release();
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
					
					while(lastPackageSent - lastPackageAcked > senderAdvertisedWindow)
						advWinFree.await();
					
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
					synchronized(sendPacketBuffer)
					{
						while(lastPackageAcked <= ackPacket.getAck_num())  // all packages < ackPacket are acked
						{
							lastPackageAcked++;
							if(lastPackageAcked >= seqNrsAvailable)
								lastPackageAcked = 0;
							
							unackedPackets--;
							sendPacketBufferElements--;
							
							sendBufferFullSem.release();
						}
					}
				}
				
				advLock.lock();
				senderAdvertisedWindow = (int) ackPacket.getAvertisedWindowSize();
				if(senderAdvertisedWindow > 0)
					advWinFree.signal();
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
