package org.iaik.net.examples;

import org.iaik.net.utils.NetUtils;

public class FTPCmdGetFile extends FTPCommand {

	String fileName;
	int pack_size;
	
	@Override
	public byte[] getCommand() 
	{
      byte[] arrayToString = fileName.getBytes();
		
	  byte[] pkg = new byte[arrayToString.length + 5];
	  
	  this.pack_size = arrayToString.length;
		
	  pkg[0] = this.identifier;
	  
	  byte[] size_in_bytes = NetUtils.intToBytes(this.pack_size);
	  System.arraycopy(size_in_bytes, 0, pkg, 1, size_in_bytes.length);
	  System.arraycopy(arrayToString, 0, pkg, 5, arrayToString.length);
		
	  return pkg;
	}
	
	public FTPCmdGetFile(String file)
	{
	  this.fileName = file;
	  this.identifier = 0x02;
	}
	
	
	private FTPCmdGetFile(byte[] packet)
	{
	  this.identifier = packet[0];
	  
	  byte[] data = new byte[packet.length - 5];
	  
	  System.arraycopy(packet, 5, data, 0, packet.length -1);

	  
	  this.pack_size = data.length;
	  this.fileName = new String(data);
    }
	
	
	public String getFile()
	{
	  return fileName;	
	}
	
	public int getSize()
	{
	  return this.pack_size;	
	}
	
	public static FTPCmdGetFile createFTPCmdGetFile(byte[] packet)
	{
	  return new FTPCmdGetFile(packet);
	}

}
