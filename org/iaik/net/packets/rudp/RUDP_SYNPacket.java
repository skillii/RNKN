package org.iaik.net.packets.rudp;

import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.utils.NetUtils;

public class RUDP_SYNPacket extends RUDPPacket {
	
	
	short retransmission_timeout;
	short cumulative_ack_timeout;
	short null_segment_timeout;
	short null_receipt_timeout;
	
	byte max_retrans;
	byte max_cum_ack;
	byte max_out_of_seq;
	byte max_outstanding_seg;
	short max_segment_size;
	
	
	public short getRetransmission_timeout() {
		return retransmission_timeout;
	}


	public void setRetransmission_timeout(short retransmissionTimeout) {
		retransmission_timeout = retransmissionTimeout;
	}


	public short getCumulative_ack_timeout() {
		return cumulative_ack_timeout;
	}


	public void setCumulative_ack_timeout(short cumulativeAckTimeout) {
		cumulative_ack_timeout = cumulativeAckTimeout;
	}


	public short getNull_segment_timeout() {
		return null_segment_timeout;
	}


	public void setNull_segment_timeout(short nullSegmentTimeout) {
		null_segment_timeout = nullSegmentTimeout;
	}


	public short getNull_receipt_timeout() {
		return null_receipt_timeout;
	}


	public void setNull_receipt_timeout(short nullReceiptTimeout) {
		null_receipt_timeout = nullReceiptTimeout;
	}


	public byte getMax_retrans() {
		return max_retrans;
	}


	public void setMax_retrans(byte maxRetrans) {
		max_retrans = maxRetrans;
	}


	public byte getMax_cum_ack() {
		return max_cum_ack;
	}


	public void setMax_cum_ack(byte maxCumAck) {
		max_cum_ack = maxCumAck;
	}


	public byte getMax_out_of_seq() {
		return max_out_of_seq;
	}


	public void setMax_out_of_seq(byte maxOutOfSeq) {
		max_out_of_seq = maxOutOfSeq;
	}


	public byte getMax_outstanding_seg() {
		return max_outstanding_seg;
	}


	public void setMax_outstanding_seg(byte maxOutstandingSeg) {
		max_outstanding_seg = maxOutstandingSeg;
	}


	public short getMax_segment_size() {
		return max_segment_size;
	}


	public void setMax_segment_size(short maxSegmentSize) {
		max_segment_size = maxSegmentSize;
	}



	/***
	 * Parses a received packet back into a RUDP_SYNPacket object
	 * 
	 * @param packet The packet to be parsed
	 */
	private RUDP_SYNPacket(byte[] packet) throws PacketParsingException
	{
	  byte identifier = packet[0];
	  
	  if((identifier & 0x40) != 0)
	    this.ack = true;
	  else
		this.ack = false;
	
	  this.syn = true;
	  this.eak = false;
	  this.rst = false;
	  this.nul = false;

	  this.packet_length = packet[1];
	  this.dest_port = NetUtils.bytesToShort(packet, 2);
      this.src_port = NetUtils.bytesToShort(packet, 4);
	  
      this.seq_num = packet[6];
      this.ack_num = packet[7];
      this.max_out_of_seq = packet[8];
      this.max_outstanding_seg = packet[9];
      
      this.max_segment_size  = NetUtils.bytesToShort(packet,10);
      this.retransmission_timeout = NetUtils.bytesToShort(packet, 12);
      this.cumulative_ack_timeout = NetUtils.bytesToShort(packet, 14);
      this.null_segment_timeout = NetUtils.bytesToShort(packet, 16);
      this.null_receipt_timeout = NetUtils.bytesToShort(packet, 18);
      this.max_retrans = packet[20];
      this.max_cum_ack = packet[21];
      
      this.checksum = NetUtils.bytesToShort(packet, 22);
      
		
	}
	
	
	/***
	 * Creates a SYN Packet object from an excisting byte array.
	 * @param packet The packet to be parsed
	 * @return A RUDP_SYNPacket Object
	 */
	
	public static RUDP_SYNPacket createSYNPacket(byte[] packet) throws PacketParsingException
	{
      return new RUDP_SYNPacket(packet);		
	}
	
	/***
	 * Sets the values for the RUDP SYN Packet
	 * to construct
	 * @param ack Determines if an ackknowledgment number is to be set
	 * @param seq_num The sequence number for the packet
	 * @param ack_num The acknowledgement number to be set
	 */
	public RUDP_SYNPacket(boolean ack, byte seq_num, byte ack_num, short dest_port, short src_port)
	{
	  this.syn = true;
	  this.ack = ack;
	  
	  if(ack)
	    this.ack_num = ack_num;	  
	  
	  this.eak = false;
	  this.rst = false;
	  this.nul = false;

	  
	  this.seq_num = seq_num;
	  this.packet_length = Byte.parseByte(Integer.toString(24));
	  
	  this.dest_port = dest_port;
	  this.src_port = src_port;
	 
	  
	  //TODO Find appropriate defaults
	  
	  //DEFAULT VALUES
		
	  this.max_outstanding_seg = 3;
      this.max_segment_size = 1024;
	  this.retransmission_timeout = 2000;
      this.cumulative_ack_timeout = 0;
	  this.null_segment_timeout = 0;
	  this.null_receipt_timeout = 0;
		
	  this.max_retrans = 3;
	  this.max_cum_ack = 3;
	  this.max_out_of_seq = 5;
		
	}

	@Override
	public String getInfo() {
		StringBuffer info = new StringBuffer();
		info.append("RUDP_SYN packet info:\n");
		info.append("__________________________________________________________\n");
		info.append("Destination Port        : " + NetUtils.toInt(this.dest_port) + "\n");
		info.append("Source Port             : " + NetUtils.toInt(this.src_port) + "\n");
		info.append("Checksum              : " + NetUtils.toInt(checksum) + "\n");
		info.append("Sequence number       : " + NetUtils.toInt(this.seq_num) + "\n");
		info.append("Ack number            : " + NetUtils.toInt(this.ack_num) + "\n");
		info.append("__________________________________________________________\n");

		return info.toString();
	}

	@Override
	public byte[] getPacket() {
	
		byte[] pkg = new byte[this.packet_length];
		
		int header_identifier = 128;
		
		if(this.ack)
		  header_identifier += 64;
		
		pkg[0] = (byte)header_identifier;
		//NetUtils.insertData(pkg, NetUtils.intToBytes(header_identifier), 0);

        pkg[1] = this.packet_length;
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.dest_port), 2);
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.src_port), 4);
        pkg[6] = this.seq_num;
        pkg[7] = this.ack_num;
        pkg[8] = this.max_out_of_seq;
        pkg[9] = this.max_outstanding_seg;
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.max_segment_size), 10);
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.retransmission_timeout), 12);
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.cumulative_ack_timeout), 14);
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.null_segment_timeout), 16);
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.null_receipt_timeout), 18);
        pkg[20] = this.max_retrans;
        pkg[21] = this.max_cum_ack;
        
        //Set Checksum 0 for now
        pkg[22] = 0;
        pkg[23] = 0;

        
        short calc_checksum = NetUtils.calcIPChecksum(pkg, 0, pkg.length);
        
		this.checksum = calc_checksum;
		
		NetUtils.insertData(pkg, NetUtils.shortToBytes(this.checksum), 22);
		
		return pkg;
	}

	@Override
	public long getTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setTimeout(long timeout) {
		// TODO Auto-generated method stub
		
	}

}
