package org.iaik.net.packets.rudp;

import org.iaik.net.packets.Packet;

public abstract class RUDPPacket implements Packet {
		
	@Override
	public abstract String getInfo();

	@Override
	public abstract byte[] getPacket();

	@Override
	public abstract long getTimeout();

	@Override
	public abstract void setTimeout(long timeout);

}
