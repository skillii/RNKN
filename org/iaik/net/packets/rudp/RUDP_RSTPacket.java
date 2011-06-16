package org.iaik.net.packets.rudp;

import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.utils.NetUtils;

public class RUDP_RSTPacket extends RUDPPacket {

	@Override
	public String getInfo() {
		StringBuffer info = new StringBuffer();
		info.append("RUDP_RST packet info:\n");
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
	public byte[] getPacket() 
	{
		byte[] pkg = new byte[this.packet_length];
		
		int header_identifier = 72;
		
		pkg[0] = (byte) header_identifier;
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
	
	private RUDP_RSTPacket(byte[] packet) throws PacketParsingException
	{
      this.packet_length = packet[1];
      this.dest_port = NetUtils.bytesToShort(packet, 2);
	  this.src_port = NetUtils.bytesToShort(packet, 4);	
		  
	  this.seq_num = packet[6];
	  this.ack_num = packet[7];
	      
	  this.checksum = NetUtils.bytesToShort(packet, 8);
		
	}
	
	public RUDP_RSTPacket(short dest_port, short src_port, byte seq_num, byte ack_num)
	{
 	  this.dest_port = dest_port;
 	  this.src_port = src_port;
 	  this.seq_num = seq_num;
 	  this.ack_num = ack_num;
 	  
 	  this.rst = true;
 	  this.nul = false;
      this.ack = true;
  	  this.syn = false;
  	  this.eak = false;
  	  
  	  
  	  this.packet_length = 10;
	}
	
	
	
	
	public static RUDP_RSTPacket createRSTPacket(byte[] packet) throws PacketParsingException
	{
      return new RUDP_RSTPacket(packet);		
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
