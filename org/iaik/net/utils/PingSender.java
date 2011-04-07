/*
 * This class represents a sender for ping requests.
 * A new thread that sends a ping request packet is created.
 * The thread waits for a reply or a timeout and prints a
 * message to the console.
 * This class is implemented as a singleton, so only one ping
 * can be sent at a time.
 * The PingSender tries to send 4 pings and gives statistics.
 * The timeout for a ping reply is set to 1 second.
 */

package org.iaik.net.utils;

import org.iaik.net.Network;
import org.iaik.net.factories.InternetLayerFactory;
import org.iaik.net.interfaces.InternetLayer;
import org.iaik.net.packets.ICMPPacket;
import org.iaik.net.packets.IPPacket;

public class PingSender extends Thread
{
	private static PingSender instance_;
	private String destinationAddress_;
	private boolean replyReceived_;
	private boolean running_;
	private boolean timeout_;
	private ICMPPacket icmpreply_;
	private IPPacket echoreply_;
	public static final int NR_OF_PINGS = 4;
	public static final short IDENTIFIER = 1;
	public static final int PING_TIMEOUT_MILLIS = 1000;
	
	public static synchronized PingSender getInstance()
	{
		if(PingSender.instance_ == null)
			PingSender.instance_ = new PingSender();
		
		return PingSender.instance_;
	}
	
	public void sendPing(String destinationAddress)
	{
		if(running_)
		{
			System.out.println("there is already a PING running, stand by and have a coffee until it is finished");
		}
		else
		{
			System.out.println();
			System.out.println("PING: Now executing an ICMP echo request on " + destinationAddress + " for " + NR_OF_PINGS + " times:");
			destinationAddress_ = destinationAddress;
			
			running_ = true;
			
			this.start();
		}
	}
	
	public void replyCallback(ICMPPacket icmpreply, IPPacket echoreply)
	{
		if(!running_)
		{
			System.out.println();
			System.out.println("PING: we received an echo reply even though we arent running... timeout leftover?");
			System.out.println("PING: anyway, source ip is " + echoreply.getSourceAddress() + ", destination ip is " + echoreply.getDestinationAddress() + ", sequence number is " + icmpreply.getSequenceNumber());
			System.out.println("PING: we will ignore this package.");
			System.out.println();
		}
		else
		{
			icmpreply_ = icmpreply;
			echoreply_ = echoreply;
			replyReceived_ = true;
		}
	}
	
	private boolean equalPayloads(byte[] payload1, byte[] payload2)
	{
		if(payload1.length != payload2.length)
			return false;
		
		for(int i = 0; i < payload1.length; i++)
		{
			if(payload1[i] != payload2[i])
			{
				return false;
			}
		}
		
		return true;
	}
	
	public void run()
	{
		InternetLayer inetLayer = InternetLayerFactory.getInstance();
		ICMPPacket icmprequest;
		IPPacket echorequest;
		
		int i;
		long sentMillis;
		
		for(i = 0; i < NR_OF_PINGS; i++)
		{
			timeout_ = false;
			
			icmprequest = ICMPPacket.createICMPPacket(ICMPPacket.ECHO_REQUEST, (byte)0, IDENTIFIER, (short)i, ICMPPacket.DEFAULT_PAYLOAD);
			echorequest = IPPacket.createDefaultIPPacket(IPPacket.ICMP_PROTOCOL, (short)0, Network.ip, destinationAddress_, icmprequest.getPacket());
			inetLayer.send(echorequest);
			sentMillis = System.currentTimeMillis();
			System.out.print("Sent Echo Request #" + i + " to " + destinationAddress_ + ": ");
			
			while(!replyReceived_)
			{
				if(System.currentTimeMillis() - sentMillis > PING_TIMEOUT_MILLIS)
				{
					timeout_ = true;
					break;
				}
				yield();
			}
			
			replyReceived_ = false;
			
			if(timeout_)
			{
				System.out.println("Echo Reply timed out!");
			}
			else
			{
				if(!echoreply_.isValid() || !icmpreply_.isValid())
				{
					System.out.println("ERROR: received an invalid packet!");
				}
				else if(!echoreply_.getSourceAddress().equals(destinationAddress_))
				{
					System.out.println("ERROR: received a reply from " + echoreply_.getSourceAddress());
				}
				else if(icmpreply_.getIdentifier() != IDENTIFIER)
				{
					System.out.println("ERROR: wrong identifier field in received packet (" + icmpreply_.getIdentifier() + ")");
				}
				else if(icmpreply_.getSequenceNumber() != i)
				{
					System.out.println("ERROR: wrong sequence number in received packet (" + icmpreply_.getSequenceNumber() + ")");
				}
				else if(!equalPayloads(icmpreply_.getPayload(), icmprequest.getPayload()))
				{
					System.out.println("ERROR: payload mismatch!");
				}
				else
				{
					System.out.println("Received Echo Reply, sequence number is " + icmpreply_.getSequenceNumber());
				}
			}
		}
		
		running_ = false;
	}
}
