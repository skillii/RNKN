package org.iaik.net.RUDP;

import org.iaik.net.packets.rudp.RUDP_DTAPacket;

public class SlidingWindowPacket
{
	RUDP_DTAPacket dataPacket;
	long timeout;
	
	public SlidingWindowPacket(RUDP_DTAPacket dataPacket, long timeout)
	{
		this.dataPacket = dataPacket;
		this.timeout = timeout;
	}
	
	public void setDataPacket(RUDP_DTAPacket dataPacket)
	{
		this.dataPacket = dataPacket;
	}
	
	public void setTimeout(long timeout)
	{
		this.timeout = timeout;
	}
	
	public RUDP_DTAPacket getDataPacket()
	{
		return this.dataPacket;
	}
	
	public long getTimeout()
	{
		return this.timeout;
	}
}
