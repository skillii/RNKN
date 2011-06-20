package org.iaik.net.examples;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.utils.NetUtils;



public class FTPCmdFileData extends FTPCommand {

	
	byte[] data;
	int fileSize;
	Log log;
		
	@Override
	public byte[] getCommand() 
	{
		
	  byte[] pkg = new byte[data.length + 5];
		
	  pkg[0] = this.identifier;
	  byte[] size = NetUtils.intToBytes(this.fileSize);
	  System.arraycopy(size, 0, pkg, 1, size.length);
	  System.arraycopy(data, 0, pkg, 5, data.length);
		
	  return pkg;
	}
	
	public FTPCmdFileData(byte[] data, int dummy)
	{
	  this.log = LogFactory.getLog(this.getClass());
	  this.data = data;
	  this.fileSize = data.length;
	  this.identifier = FTPCommand.FILE_DATA_IDENTIFIER;
	}
	
	public FTPCmdFileData(File file) throws IOException, FileNotFoundException
	{
        InputStream is = new FileInputStream(file);
        
        long length = file.length();
        this.fileSize = (int)length;
        this.identifier = FTPCommand.FILE_DATA_IDENTIFIER;

        this.data = new byte[(int)length];
            
        int offset = 0;
        int numRead = 0;
        while (offset < data.length
               && (numRead=is.read(data, offset, data.length-offset)) >= 0)
        {
            offset += numRead;
        }

        if (offset < data.length) 
        {
          throw new IOException("File konnte nicht vollstaendig gelesen werden :  "+file.getName());
        }
    
        is.close();	
	}
	
	
	/*
	public FTPCmdFileData(File file)
	{
      ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
	  ObjectOutputStream out;
	  
	  try
	  {
		out = new ObjectOutputStream(bos);
		out.writeObject(file);	
		out.close();
	  } 
	  catch (IOException e) 
	  {
		log.info("Couldn't serialize file!!");
	  }
	 
	  this.data = bos.toByteArray();
	}*/
	
	private FTPCmdFileData(byte[] packet)
	{
	  this.log = LogFactory.getLog(this.getClass());	
	  this.identifier = packet[0];
	  
	  this.data = new byte[packet.length - 5];

	  System.arraycopy(packet, 5, this.data, 0, packet.length - 5);

	  this.fileSize = data.length;
    }
	
	
	public File getDataFile()
	{
		try
		{
		  ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(this.data));
          File return_val = (File) in.readObject();
	      
          in.close();
	     
          return return_val;
		} 
		catch (IOException e) 
		{
		  log.info("Couldn't deserialize file object");
		} 
		catch (ClassNotFoundException e) 
		{
		  log.info("Class for deserialization not found");
		}
	 
        return null;
	}
	
	public byte[] getDataBytes()
	{
	  return this.data;	
	}
	
	
	public static FTPCmdFileData createFTPCmdFileData(byte[] packet)
	{
	  return new FTPCmdFileData(packet);	
	}
	
	public int getFileSize()
	{
	  return this.fileSize;	
	}

}
