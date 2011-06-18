package org.iaik.net.examples;

import org.iaik.net.utils.NetUtils;

public class FTPCmdPutFile extends FTPCommand {

    String fileName;
	int pack_size;
    
	@Override
	public byte[] getCommand() 
	{
      byte[] arrayToString = fileName.getBytes();
		
	  byte[] pkg = new byte[arrayToString.length + 5];
		
	  pkg[0] = this.identifier;
	  this.pack_size = arrayToString.length;
	  
	  NetUtils.insertData(pkg, NetUtils.intToBytes(this.pack_size), 1);  
	  
	  System.arraycopy(arrayToString, 0, pkg, 5, arrayToString.length);
		
	  return pkg;
	}
	
	public FTPCmdPutFile(String file)
	{
	  this.fileName = file;
	  this.identifier = 0x04;
	}
	
	
	private FTPCmdPutFile(byte[] packet)
	{
	  this.identifier = packet[0];
	  
	  byte[] data = new byte[packet.length - 5];
	  
	  System.arraycopy(packet, 5, data, 0, packet.length -1);

	  this.fileName = new String(data);
	  this.pack_size = data.length;
    }
	
	
	public String getFile()
	{
	  return fileName;	
	}
	
	
	public static FTPCmdPutFile createFTPCmdPutFile(byte[] packet)
	{
	  return new FTPCmdPutFile(packet);	
	}

}
