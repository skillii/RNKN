package org.iaik.net.packets.rudp;

import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.utils.NetUtils;

public class RUDP_ACKPacket extends RUDPPacket {

	byte advertised_window_size;
	
	
	public RUDP_ACKPacket(short dest_port, short src_port, byte seq_num, byte ack_num,
			              byte advertised_window_size)
	{
      this.ack = true;
      this.syn = false;
	  this.eak = false;
	  this.rst = false;
	  this.nul = false;
	  
	  this.packet_length = 11;
	  this.dest_port = dest_port;
	  this.src_port = src_port;
	  
	  this.seq_num = seq_num;
	  this.ack_num = ack_num;
	  
	  this.advertised_window_size = advertised_window_size;
	}
	
	
	
	private RUDP_ACKPacket(byte[] packet) throws PacketParsingException
	{
		  
		  this.ack = true;
		  this.syn = false;
		  this.eak = false;
		  this.rst = false;
		  this.nul = false;

		  this.packet_length = packet[1];
		  this.dest_port = NetUtils.bytesToShort(packet, 2);
	      this.src_port = NetUtils.bytesToShort(packet, 4);
		  
	      this.seq_num = packet[6];
	      this.ack_num = packet[7];

	      this.advertised_window_size = packet[8];
	      this.checksum = NetUtils.bytesToShort(packet, 9);
	}
	
	public static RUDP_ACKPacket createACKPacket(byte[] packet) throws PacketParsingException
	{
      return new RUDP_ACKPacket(packet);		
	}
	
	

	
	@Override
	public String getInfo() {
		StringBuffer info = new StringBuffer();
		info.append("RUDP_ACK packet info:\n");
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
		
		int header_identifier = 64;
		
		NetUtils.insertData(pkg, NetUtils.intToBytes(header_identifier), 0);
        pkg[1] = this.packet_length;
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.dest_port), 2);
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.src_port), 4);
        pkg[6] = this.seq_num;
        pkg[7] = this.ack_num;

        
        //Set Checksum 0 for now
        pkg[8] = 0;
        pkg[9] = 0;

        
        short calc_checksum = NetUtils.calcIPChecksum(pkg, 0, pkg.length);
        
		this.checksum = calc_checksum;
		
		NetUtils.insertData(pkg, NetUtils.shortToBytes(this.checksum), 8);
		
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
