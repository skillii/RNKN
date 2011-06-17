package org.iaik.net.packets.rudp;

import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.utils.NetUtils;

public class RUDP_NULPacket extends RUDPPacket {

	
	
	public RUDP_NULPacket(short src_port, short dest_port)
	{
      this.nul = true;
      this.ack = false;
 	  this.syn = false;
 	  this.eak = false;
 	  this.rst = false;
	
 	  this.packet_length = 10;
 	  this.seq_num = 0;
 	  this.ack_num = 0;
 	  
 	  this.src_port = src_port;
 	  this.dest_port = dest_port;
 	}
	
	private RUDP_NULPacket(byte[] packet) throws PacketParsingException
	{
	      this.packet_length = packet[1];
		  this.dest_port = NetUtils.bytesToShort(packet, 2);
		  this.src_port = NetUtils.bytesToShort(packet, 4);	
		  
	      this.seq_num = packet[6];
	      this.ack_num = packet[7];
	      
	      short checksum_should = NetUtils.bytesToShort(packet, 8); 
	      
	      packet[8] = 0;
	      packet[9] = 0;
	      
	      short calc_checksum = NetUtils.calcIPChecksum(packet, 0, packet.length - 2);
	      
	      if(checksum_should != calc_checksum)
	        throw new PacketParsingException("Checksum failed!");
	      
	      this.checksum = calc_checksum;
	}
	
	
	public static RUDPPacket createNULPacket(byte[] packet) throws PacketParsingException
	{
	  return new RUDP_NULPacket(packet);	
	}
 	
	
	@Override
	public String getInfo() {
		StringBuffer info = new StringBuffer();
		info.append("RUDP_NUL packet info:\n");
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
		
		NetUtils.insertData(pkg, NetUtils.intToBytes(header_identifier), 0);
        pkg[1] = this.packet_length;
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.dest_port), 2);
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.src_port), 4);
        pkg[6] = 0;
        pkg[7] = 0;
    
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
