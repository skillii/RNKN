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
	  
	  try
	  {
	    this.packet_length = Short.parseShort(Integer.toString(11 + payload.length)); 
	  }
	  catch(NumberFormatException e)
	  {
	    System.out.println("NumberFormatException!!");	  
	  }
	}
	
	private RUDP_DTAPacket(byte packet[]) throws PacketParsingException
	{
      this.packet_length = NetUtils.bytesToShort(packet, 1);
	  this.dest_port = NetUtils.bytesToShort(packet, 3);
	  this.src_port = NetUtils.bytesToShort(packet, 5);	
	  
      this.seq_num = packet[7];
      this.ack_num = packet[8];
      
      this.payload = new byte[packet.length - 11];
      System.arraycopy(packet, 11, this.payload, 0, packet.length - 11);
      
      short checksum_should = NetUtils.bytesToShort(packet, 9);   
      
      packet[9] = 0;
      packet[10] = 0;
      
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
		
		pkg[0] = (byte)header_identifier;
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.packet_length), 1);
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.dest_port), 3);
        NetUtils.insertData(pkg, NetUtils.shortToBytes(this.src_port), 5);
        pkg[7] = this.seq_num;
        pkg[8] = this.ack_num;
    
        //Set Checksum 0 for now
        pkg[9] = 0;
        pkg[10] = 0;
        
        //NetUtils.insertData(pkg, this.payload, 10);
        System.out.println(Integer.toString((int)this.packet_length) + " " + Integer.toString(this.payload.length));
        
        System.arraycopy(this.payload, 0, pkg, 11, this.payload.length);
        
            
        short calc_checksum = NetUtils.calcIPChecksum(pkg, 0, pkg.length);
        
		this.checksum = calc_checksum;
		
		NetUtils.insertData(pkg, NetUtils.shortToBytes(this.checksum), 9);
		
		return pkg;
	}
	
	public byte[] getPayload()
	{
	  return this.payload;	
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
