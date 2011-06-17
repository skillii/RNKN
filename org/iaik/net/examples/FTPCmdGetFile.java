package org.iaik.net.examples;

public class FTPCmdGetFile extends FTPCommand {

	String fileName;
	
	@Override
	public byte[] getCommand() 
	{
      byte[] arrayToString = fileName.getBytes();
		
	  byte[] pkg = new byte[arrayToString.length + 1];
		
	  pkg[0] = this.identifier;
	  System.arraycopy(arrayToString, 0, pkg, 1, arrayToString.length);
		
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
	  
	  byte[] data = new byte[packet.length - 1];
	  
	  System.arraycopy(packet, 1, data, 0, packet.length -1);

	  this.fileName = new String(data);
    }
	
	
	public String getFile()
	{
	  return fileName;	
	}
	
	
	public static FTPCmdGetFile createFTPCmdGetFile(byte[] packet)
	{
	  return new FTPCmdGetFile(packet);
	}

}
