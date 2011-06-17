package org.iaik.net.examples;


public class FTPCmdListFiles extends FTPCommand {
	
	String fileList;

	@Override
	public byte[] getCommand() {
		
		byte[] arrayToString = fileList.getBytes();
		
		byte[] pkg = new byte[arrayToString.length + 1];
		
		pkg[0] = this.identifier;
		System.arraycopy(arrayToString, 0, pkg, 1, arrayToString.length);
		
		return pkg;
	}
	
	
	public FTPCmdListFiles(String files)
	{
	  this.fileList = files;
	  this.identifier = 0x01;
	}
	
	public FTPCmdListFiles(String[] files)
	{
	  StringBuilder fileBuilder = new StringBuilder();
	  
	  for(String i: files)
	  {
        fileBuilder.append(i);
        fileBuilder.append("\n");
	  }
	  
	  this.fileList = fileBuilder.toString();
	}
	
	
	private FTPCmdListFiles(byte[] packet)
	{
	  this.identifier = packet[0];
	  
	  byte[] data = new byte[packet.length - 1];
	  
	  System.arraycopy(packet, 1, data, 0, packet.length -1);

	  this.fileList = new String(data);
    }
	
	
	public String getFileList()
	{
	  return fileList;	
	}
	
	public static FTPCmdListFiles createFTPCmdListFiles(byte[] packet)
	{
	  return new FTPCmdListFiles(packet);	
	}

}
