package org.iaik.net.examples;

import org.iaik.net.utils.NetUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FTPCmdListFiles extends FTPCommand {
	
	String fileList;
	int sizeOfData;
	Log log;

	@Override
	public byte[] getCommand() {
		
		byte[] arrayToString = fileList.getBytes();
		int size = arrayToString.length;
		
		byte[] pkg = new byte[arrayToString.length + 5];
		
		pkg[0] = this.identifier;
		
		byte[] size_in_bytes = NetUtils.intToBytes(size);

		System.arraycopy(size_in_bytes, 0, pkg, 1, size_in_bytes.length);
		System.arraycopy(arrayToString, 0, pkg, 5, arrayToString.length);
		
		return pkg;
	}
	
	
	public FTPCmdListFiles(String files)
	{
	  this.log = LogFactory.getLog(this.getClass());
	  this.fileList = files;
	  this.identifier = FTPCommand.LIST_FILE_IDENTIFIER;
	}
	
	
	public void setFiles(String[] files)
	{
      StringBuilder fileBuilder = new StringBuilder();
		  
	  for(String i: files)
	  {
	    fileBuilder.append(i);
	    fileBuilder.append("\n");
	  }
	  
	  this.fileList = fileBuilder.toString();
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
	  
	  log.info("size of data is " + Integer.toString(this.sizeOfData));
	  
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
