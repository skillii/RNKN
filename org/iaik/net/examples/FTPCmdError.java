package org.iaik.net.examples;

import org.iaik.net.utils.NetUtils;

public class FTPCmdError extends FTPCommand {
	
	
	String message;
	int pack_size;

	@Override
	public byte[] getCommand() 
	{
	  byte[] arrayToString = message.getBytes();
			
	  byte[] pkg = new byte[arrayToString.length + 5];
	  
	  this.pack_size = arrayToString.length;
			
      pkg[0] = this.identifier;
      NetUtils.insertData(pkg, NetUtils.intToBytes(this.pack_size), 1);
      System.arraycopy(arrayToString, 0, pkg, 5, arrayToString.length);
			
	  return pkg;
	}
		
	public FTPCmdError(String message)
	{
      this.message = message;
	  this.identifier = 0x10;
	}
		
		
	private FTPCmdError(byte[] packet)
	{
      this.identifier = packet[0];
		  
	  byte[] data = new byte[packet.length - 5];
		  
	  
	  this.pack_size = NetUtils.bytesToInt(packet, 1);
	  System.arraycopy(packet, 5, data, 0, packet.length - 5);

	  this.message = new String(data);
	}
		
		
	public String getMessage()
	{
      return this.message;	
	}
		
	
	public int getSize()
	{
	  return this.pack_size;
	}
		
	public static FTPCmdError createFTPCmdError(byte[] packet)
    {
      return new FTPCmdError(packet);	
	}


}
