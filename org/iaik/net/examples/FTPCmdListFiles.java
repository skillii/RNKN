package org.iaik.net.examples;

import org.iaik.net.utils.NetUtils;


public class FTPCmdListFiles extends FTPCommand {
	
	String fileList;
	int sizeOfData;

	@Override
	public byte[] getCommand() {
		
		byte[] arrayToString = fileList.getBytes();
		int size = arrayToString.length;
		
		byte[] pkg = new byte[arrayToString.length + 5];
		
		pkg[0] = this.identifier;
		NetUtils.insertData(pkg, NetUtils.intToBytes(size), 1);
		System.arraycopy(arrayToString, 0, pkg, 5, arrayToString.length);
		
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

	  this.sizeOfData = NetUtils.bytesToInt(packet, 1);
	  
	  byte[] data = new byte[this.sizeOfData];
	  
      System.arraycopy(packet, 5, data, 0, this.sizeOfData);

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
