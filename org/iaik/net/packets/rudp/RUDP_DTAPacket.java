package org.iaik.net.packets.rudp;

import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.utils.NetUtils;

public class RUDP_DTAPacket extends RUDPPacket {

	
	byte[] payload;
	
	
	public RUDP_DTAPacket(short dest_port,short src_port, byte[] payload, byte seq_num,
			              byte ack_num)
	{
      this.ack = false;
	  this.syn = false;
	  this.eak = false;
	  this.rst = false;
	  this.nul = false;	
	  
	  this.seq_num = seq_num;
	  this.ack_num = ack_num;
	  
	  this.src_port = src_port;
	  this.dest_port = dest_port;
	  
	  this.payload = new byte[payload.length];
	  System.arraycopy(payload, 0, this.payload, 0, payload.length);
	  
	  this.packet_length = Byte.parseByte(Integer.toString(9 + payload.length)); 
	  
	}
	
	private RUDP_DTAPacket(byte packet[]) throws PacketParsingException
	{
      this.packet_length = packet[1];
	  this.dest_port = NetUtils.bytesToShort(packet, 2);
	  this.src_port = NetUtils.bytesToShort(packet, 4);	
	  
      this.seq_num = packet[6];
      this.ack_num = packet[7];
      
      this.payload = new byte[packet.length - 10];
      System.arraycopy(packet, 10, this.payload, 0, packet.length - 10);
      
      short checksum_should = NetUtils.bytesToShort(packet, 8);   
      
      packet[8] = 0;
      packet[9] = 0;
      
      short calc_checksum = NetUtils.calcIPChecksum(packet, 0, packet.length);
      
      if(checksum_should != calc_checksum)
        throw new PacketParsingException("Checksum failed!");
		
      
      this.checksum = calc_checksum;
		
	}
	
	
	public static RUDP_DTAPacket createDTAPacket(byte[] packet) throws PacketParsingException
	{
      return new RUDP_DTAPacket(packet);		
	}
	
	@Override
	public String getInfo() {
		StringBuffer info = new StringBuffer();
		info.append("RUDP_DTA packet info:\n");
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
		
		int header_identifier = 0;
		
		NetUtils.insertData(pkg, NetUtils.intToBytes(header_identifier), 0);
        pkg[1] = this.packet_length;
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.dest_port), 2);
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.src_port), 4);
        pkg[6] = this.seq_num;
        pkg[7] = this.ack_num;
    
        //Set Checksum 0 for now
        pkg[8] = 0;
        pkg[9] = 0;
        
        NetUtils.insertData(pkg, this.payload, 10);

            
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
