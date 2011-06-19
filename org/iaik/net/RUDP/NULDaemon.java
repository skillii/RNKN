package org.iaik.net.RUDP;

import java.util.Timer;
import java.util.TimerTask;

import org.iaik.net.Network;
import org.iaik.net.factories.TransportLayerFactory;
import org.iaik.net.interfaces.TransportLayer;
import org.iaik.net.packets.IPPacket;
import org.iaik.net.packets.rudp.RUDPPacket;
import org.iaik.net.packets.rudp.RUDP_NULPacket;
import org.iaik.net.packets.rudp.RUDP_SYNPacket;

import sun.rmi.runtime.Log;

class NULDaemon {
	private Timer sendTimer;
	private Timer receiveTimer;
	
	private TransportLayer transportLayer;
	private int remotePort;
	private String remoteIP;
	
	private int port;
	
	private int nullCycleValue;
	private int nullTimeoutValue;
	
	private NULDaemonCallback callback;
	
	boolean bReceiverTimerRunning = false;
	
	
	
	public NULDaemon(String remoteIP, int remotePort, int localPort, int nullCycleValue, int nullTimeoutValue, NULDaemonCallback callback)
	{
		this.remoteIP = remoteIP;
		this.remotePort = remotePort;
		this.port = localPort;
		
		this.nullCycleValue = nullCycleValue;
		this.nullTimeoutValue = nullTimeoutValue;
		
		this.callback = callback;
		
		
		sendTimer = new Timer();
		receiveTimer = new Timer();
		

		transportLayer = TransportLayerFactory.getInstance();
	}
	
	public void start()
	{
		synchronized(remoteIP)
		{
			sendTimer = new Timer();
			receiveTimer = new Timer();
			
			sendTimer.schedule(new Sender(), nullCycleValue);
			receiveTimer.schedule(new ReceiveTimeout(), nullTimeoutValue);
			
			bReceiverTimerRunning = true;
		}
	}
	
	public void stop()
	{		
		synchronized(remoteIP)
		{
			sendTimer.cancel();
			sendTimer.purge();
			
			if(bReceiverTimerRunning)
			{
				receiveTimer.cancel();
				receiveTimer.purge();
			}
			
			bReceiverTimerRunning = false;
		}
	}
	
	public void packetReceived()
	{
		synchronized(remoteIP)
		{
			if(bReceiverTimerRunning)
			{
				receiveTimer.cancel();
				receiveTimer.purge();
			}
			receiveTimer = new Timer();
			receiveTimer.schedule(new ReceiveTimeout(), nullTimeoutValue);
			
			bReceiverTimerRunning = true;
		}
	}
	
	public void packetSent()
	{
		synchronized(remoteIP)
		{
			sendTimer.cancel();
			sendTimer.purge();
			sendTimer = new Timer();
			sendTimer.schedule(new Sender(), nullCycleValue);
		}
	}
	
	
	private class Sender extends TimerTask
	{
		@Override
		public void run() {
			synchronized(remoteIP)
			{
				RUDPPacket rudpPack;
				IPPacket rudpPackIP;
				
				rudpPack = new RUDP_NULPacket((short)port, (short)remotePort);
				
				rudpPackIP = IPPacket.createDefaultIPPacket(IPPacket.RUDP_PROTOCOL, (short)0, Network.ip, remoteIP, rudpPack.getPacket());
				transportLayer.sendPacket(rudpPackIP);
				
				sendTimer = new Timer();
				sendTimer.schedule(new Sender(), nullCycleValue);
			}
		}
		
	}
	
	private class ReceiveTimeout extends TimerTask
	{
		@Override
		public void run() {
			synchronized(remoteIP)
			{
				callback.TimeoutReached();
				
				bReceiverTimerRunning = false;
			}
		}
		
	}

}
