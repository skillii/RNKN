package org.iaik.net.examples;

import java.io.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.iaik.net.RUDP.ConnectionCloseReason;
import org.iaik.net.RUDP.RUDPServerConnection;
import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.interfaces.RUDPServerCallback;
import org.iaik.net.utils.NetUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FTPServerCallback implements RUDPServerCallback{

	
	public enum ServerCallbackState{
		NotAwaitingData,
		AwaitingGetFileName,
		AwaitingFileData,
		AwaitingPutFileName,
		AwaitingFileListData
	}
	
	RUDPServerConnection conn;
	String message;
	File directory;
	Condition transferCondition;
	Lock transferConditionLock;
	ServerCallbackState state;
	
	int bytesToRead;
	int countRead;
	
	ConnectionCloseReason reason;
	
	byte[] dataRead;
	
	Log log;
	
	public FTPServerCallback(Condition cond, Lock lock, String directory) throws FileNotFoundException
	{
		this.transferCondition = cond;
		this.transferConditionLock = lock;
		this.directory = new File(directory);
		
		if(!this.directory.exists())
		  throw new FileNotFoundException("Directory existiert nicht");
		
		this.state = ServerCallbackState.NotAwaitingData;
		
		log = LogFactory.getLog(this.getClass());
	}
	
	public void setConnection(RUDPServerConnection conn)
	{
	  this.conn = conn;
	}
	
	public String getMessage()
	{
	  return this.message;
	}
	
	
	@Override
	public void clientConnected(String ip) {
		this.message = "User with IP " + ip + " connected";
		
		this.transferConditionLock.lock();
		this.transferCondition.signal();
		this.transferConditionLock.unlock();
		
	}

	@Override
	public void ConnectionClosed(ConnectionCloseReason reason) {
	    transferConditionLock.lock();
	    transferCondition.signal();
	    transferConditionLock.unlock();
	    
	    this.reason = reason;
		
	}
	
	private void initNewTransfer(int size)
	{
	  this.bytesToRead = size;
	  this.countRead = 0;
	  this.dataRead = new byte[size];	
	}
	
	private boolean fileExistsInDirectory(String filename)
	{
	  String[] files = this.directory.list();
		
	  for(String i : files)
	  {
	    if(i.equals(filename))	
	      return true;	 
	  }

	  return false;	
	}
	
	private File getFileInDirectory(String filename)
	{
	  File[] files = this.directory.listFiles();
	  
	  for(File i : files)
	  {
	    if(i.getName().equals(filename))
	      return i;
	  }
	  
	  return null;
	}
	
	
	private void checkAndSendFile(String name)
	{
		if(!fileExistsInDirectory(name))
		{
		  FTPCmdError err = new FTPCmdError("Error:File doesn't exist in directory\n");
		  this.state = ServerCallbackState.NotAwaitingData;
		  
		  System.out.println("Sent file " + name + "\n");
		  this.conn.sendData(err.getCommand());
		}		
		else
		{
			
		  File file = getFileInDirectory(name);	
		  if(file != null)
		  {
		    FTPCmdFileData dataPacket;
			
		    try 
		    {
		      dataPacket = new FTPCmdFileData(file);
			    this.conn.sendData(dataPacket.getCommand());
			} 
		    catch (FileNotFoundException e) 
		    {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		    catch (IOException e)
		    {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
		  }
		  else
		  {
		    System.out.println("BUGGY ISLAND, file not found\n");	  
		  }
		  
		  this.state = ServerCallbackState.NotAwaitingData;	
			
		}
	}
	
	
	
	
	private void findOutPacketType()
	{
	  byte[] tempData = this.conn.getReceivedData(5);
	  byte identifier = tempData[0];
	  int size = NetUtils.bytesToInt(tempData, 1);
	  
	  log.info("Finding out type of Command");
	  log.info("Identifier " + Byte.toString(identifier));
	  log.info("Size " + Integer.toString(size));
	  
	  if((identifier & FTPCommand.GET_FILE_IDENTIFIER) != 0)
	  {
	    if(this.conn.dataToRead() >= size)
	    {
	      log.info("Get File Command received"); 	
	      byte[] data = this.conn.getReceivedData(size);
	      
	      try 
	      {
			FTPCmdGetFile cmd = (FTPCmdGetFile)FTPCommand.parseFTPCommand(data);
			
			String name = cmd.getFile();
			
			checkAndSendFile(name);
		  } 
	      catch (PacketParsingException e)
	      {
		    System.out.println("Couldn't parse file request packet\n");
	      }
		}
	    else
	    {
          initNewTransfer(size);
          this.state = ServerCallbackState.AwaitingGetFileName;
	    }
	  }
	   
	  if((identifier & FTPCommand.LIST_FILE_IDENTIFIER) != 0)
	  {
		log.info("List file command received");  
		  
        if(this.conn.dataToRead() >= size)
		{
          byte[] data = this.conn.getReceivedData(size);
  	      
          FTPCmdListFiles cmd = new FTPCmdListFiles("dummy");
  		  cmd.setFiles(this.directory.list());
  			
  		  this.conn.sendData(cmd.getCommand());
     	}
		else
		{
		  this.state = ServerCallbackState.AwaitingFileListData;
	      initNewTransfer(size);
		}
	  }
	  	
	  
	  //TODO Implement this ;)
	  if((identifier & FTPCommand.PUT_FILE_IDENTIFIER) != 0)
	  {
		  
		System.out.println("PUT COMMAND\n");  
	    if(this.conn.dataToRead() >= size)
		{
		   
		   
		}
		/*else
	      initNewTransfer(size);
        */		  
	  }
	  
	  
	}

	@Override
	public void DataReceived() {
		
		log.info("Data received called");
		log.info("Data to read = " + Integer.toString(this.conn.dataToRead()));
		
		if(this.state == ServerCallbackState.NotAwaitingData)
		{
		  if(this.conn.dataToRead() >= 5)
		  {	  
	        findOutPacketType();
		  }
		}
		else if(this.state == ServerCallbackState.AwaitingGetFileName ||
				this.state == ServerCallbackState.AwaitingFileListData)
		{
          byte[] tempData = conn.getReceivedData(this.bytesToRead);
			
		  System.arraycopy(tempData, 0, this.dataRead, this.countRead, tempData.length);
			
		  this.bytesToRead -= tempData.length;
		  this.countRead += tempData.length;
			
		  if(this.bytesToRead == 0)
		  {   
		    if(this.state == ServerCallbackState.AwaitingGetFileName)
		    {
			  try 
			  {
			    FTPCmdGetFile cmd = (FTPCmdGetFile)FTPCommand.parseFTPCommand(this.dataRead);
					
				String name = cmd.getFile();
					
				checkAndSendFile(name);
			  } 
			  catch (PacketParsingException e)
			  {
			    System.out.println("Couldn't parse file request packet\n");
			  }
		    	
		    }
		    if(this.state == ServerCallbackState.AwaitingFileListData)
		    {
		      try 
		  	  {
		  	    FTPCmdListFiles cmd = (FTPCmdListFiles)FTPCommand.parseFTPCommand(this.dataRead);
		  			
		  	    cmd.setFiles(this.directory.list());
		  			
		  	    this.conn.sendData(cmd.getCommand());
		  	  }
		  	  catch(PacketParsingException e)
		  	  {
		  	    System.out.println("Couldn't parse list file packet\n");	  
		  	  }
		    }
			  
		  }
		}
	
		
		
	}

}
