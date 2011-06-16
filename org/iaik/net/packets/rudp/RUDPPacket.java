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
	
	public boolean isSyn() {
		return syn;
	}

	public void setSyn(boolean syn) {
		this.syn = syn;
	}

	public boolean isEak() {
		return eak;
	}

	public void setEak(boolean eak) {
		this.eak = eak;
	}

	public boolean isRst() {
		return rst;
	}

	public void setRst(boolean rst) {
		this.rst = rst;
	}

	public boolean isNul() {
		return nul;
	}

	public void setNul(boolean nul) {
		this.nul = nul;
	}

	public byte getPacket_length() {
		return packet_length;
	}

	public void setPacket_length(byte packetLength) {
		packet_length = packetLength;
	}

	public byte getSeq_num() {
		return seq_num;
	}

	public void setSeq_num(byte seqNum) {
		seq_num = seqNum;
	}

	public short getDest_port() {
		return dest_port;
	}

	public void setDest_port(short destPort) {
		dest_port = destPort;
	}

	public short getSrc_port() {
		return src_port;
	}

	public void setSrc_port(short srcPort) {
		src_port = srcPort;
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
	  
	  
	  if((identifier & 0x80) != 0)
	  {
	    //We got a SYN Packet here
		return RUDP_SYNPacket.createSYNPacket(packet);
	  }
		  
      //TODO: Insert the rest of the packages here  
		  
	 	
	  return null;	
	}

}
