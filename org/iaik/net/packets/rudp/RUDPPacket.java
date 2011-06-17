package org.iaik.net.packets.rudp;

import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.packets.Packet;
import org.iaik.net.utils.NetUtils;

public abstract class RUDPPacket implements Packet {
	
	
    //Fields of the header are common for all types
	//of RUDP packets
	boolean syn;
	public boolean isAck() {
		return ack;
	}

	public byte getAck_num() {
		return ack_num;
	}

	boolean ack;
	boolean eak;
	boolean rst;
	boolean nul;

    //bytes - 8 bit
 	byte packet_length;
	byte seq_num;
	byte ack_num;
	
	//short - 16 bit
	short checksum;
	short dest_port;
	short src_port;
		
	@Override
	public abstract String getInfo();

	@Override
	public abstract byte[] getPacket();

	@Override
	public abstract long getTimeout();

	@Override
	public abstract void setTimeout(long timeout);
	
	public static RUDPPacket parsePacket(byte[] packet) throws PacketParsingException
	{
	  byte identifier = packet[0];
	  
	  //SYN Packet
	  if((identifier & 0x80) == 1)
	  {
		return RUDP_SYNPacket.createSYNPacket(packet);
	  }
	  
	  //RST Packet
	  if((identifier & 0x10) == 1)
	  {
		return RUDP_RSTPacket.createRSTPacket(packet);  
	  }
	  
	  //NUL Packet
	  if((identifier & 0x08) == 1)
	  {
		return RUDP_NULPacket.createNULPacket(packet);
	  }
		
	  if((identifier & 0x40) == 1)
	  {
	    return RUDP_ACKPacket.createACKPacket(packet);
	  }  
		  
	 	
	  return RUDP_DTAPacket.createDTAPacket(packet);	
	}

}
