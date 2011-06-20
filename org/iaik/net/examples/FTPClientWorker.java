package org.iaik.net.examples;

import java.util.Timer;
import java.util.concurrent.locks.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.RUDP.ConnectionCloseReason;
import org.iaik.net.RUDP.RUDPConnection;
import org.iaik.net.utils.NetUtils;

public class FTPClientWorker extends Thread {
	
	
	public enum ClientCallbackState{
		AwaitingFile,
		AwaitingFileList,
		AwaitingErrorMessage,
		Error,
		FileListComplete,
		FileComplete,
		TransferingFile,
		TransferingFileList,
		PuttingFile,
		Disconnected}
	
	ClientCallbackState cbstate;

	Condition threadCondition;
	Lock threadConditionLock;

	Condition transferCondition;
	Lock transferConditionLock;
	boolean reduceDataThroughput;

	RUDPConnection conn;
	
	String errorMessage;
	Log log;
	
	byte[] dataRead;
	

	int bytesToRead;
	int countRead;
	
	public FTPClientWorker(Condition threadCondition, Lock threadConditionLock,
			               Condition transferCondition, Lock transferConditionLock)
	{
	  this.threadCondition = threadCondition;
	  this.threadConditionLock = threadConditionLock;
	  this.transferCondition = transferCondition;
	  this.transferConditionLock = transferConditionLock;
	  
	  this.log = LogFactory.getLog(this.getClass());
	  
	  this.reduceDataThroughput = true;
	}
	
	public void setConnection(RUDPConnection conn)
	{
	  this.conn = conn;	
	}
	
	public void run()
	{
	  while(true)
	  {
 
        try {
            this.threadConditionLock.lock();
            System.out.println("Waiting");
			this.threadCondition.await();
			this.threadConditionLock.unlock();
			Thread.sleep((int)(Math.random() * 4000));
			receivedData();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }		  
	}
	
	private void receivedPacketWhileUploading()
	{
		byte[] tempData;
		int packet_size;
	    
		
		//Es sollte nur ein Error Paket kommen..
		
        this.cbstate = ClientCallbackState.Error;
	    
	    tempData = conn.getReceivedData(5);
	    packet_size = NetUtils.bytesToInt(tempData, 1);
	    byte identifier = tempData[0];
	    
        this.receivedErrorMessage(packet_size, identifier);
	}
	
	private void awaitingFileTransfer()
	{
		byte[] tempData = conn.getReceivedData(5);
		  
	    byte identifier = tempData[0];
	    int size = NetUtils.bytesToInt(tempData, 1);
	    
	    this.log.info("Size of data to read" + size);
	    
	    if((identifier & FTPCommand.FILE_DATA_IDENTIFIER) != 0)
	    {
	      //Find out how big the file is
		  //(We assume this information arrived in the first RUDP Packet as well)
		  this.bytesToRead = size;
		  this.countRead = 0;
		  
		  tempData = this.conn.getReceivedData(this.bytesToRead);
		  
		  //Now read the rest of the data that came with the first package
		  this.log .info("Bytes to still read: " + this.bytesToRead); 
		  this.dataRead = new byte[this.bytesToRead];
		 
		  System.arraycopy(tempData, 0, this.dataRead, 0, tempData.length);
		 
		  //Reduce counter of bytes to read
		  this.bytesToRead -= tempData.length;
		  this.countRead += tempData.length;
		  
		  this.log.info("Bytes to read after first package: " + this.bytesToRead);
		 
		  if(this.bytesToRead > 0)
		    this.cbstate = ClientCallbackState.TransferingFile;
		  else
		  {
			this.cbstate = ClientCallbackState.FileComplete;
			
		    transferConditionLock.lock();
			transferCondition.signal();
			transferConditionLock.unlock();
		  }
	    }
	    else
	    {
	      receivedErrorMessage(size,identifier);	
	    }
	}
	
	private void awaitingFileList()
	{
	  byte[] tempData = conn.getReceivedData(5);
		   
	  byte identifier = tempData[0];
	  int size = NetUtils.bytesToInt(tempData, 1); 
	     
	  if((identifier & FTPCommand.LIST_FILE_IDENTIFIER) != 0)
	  {

	    this.bytesToRead = size;
	    this.countRead = 0;
		 
		tempData = this.conn.getReceivedData(this.bytesToRead);
		  
		//Now read the rest of the data that came with the first package
		this.dataRead = new byte[this.bytesToRead];
		 
		System.arraycopy(tempData, 0, this.dataRead, 0, tempData.length);
		 
		//Reduce counter of bytes to read
		this.bytesToRead -= tempData.length;
		this.countRead += tempData.length;
		 
		if(this.bytesToRead > 0)
		    this.cbstate = ClientCallbackState.TransferingFileList;
		else
		{
	      this.cbstate = ClientCallbackState.FileListComplete;
			
		  transferConditionLock.lock();
		  transferCondition.signal();
		  transferConditionLock.unlock();
		}
	  }
	  
	  else
	  {
	    receivedErrorMessage(size, identifier);	  
	  }
	  
	}
		
	private void receivedErrorMessage(int size, byte identifier)
	{
		if((identifier & FTPCommand.ERROR_IDENTIFIER) != 0)
	    {
	      if(this.conn.dataToRead() >= size)
	      {
            byte[] tempData = conn.getReceivedData(size);	
	        this.cbstate = ClientCallbackState.Error;
            
            this.errorMessage = tempData.toString();
			      
			transferConditionLock.lock();
		    transferCondition.signal();
			transferConditionLock.unlock();
		  }
	      else
	      {
	        this.cbstate = ClientCallbackState.AwaitingErrorMessage;
	        this.bytesToRead = size;
	        this.countRead = 0;
	        this.dataRead = new byte[this.bytesToRead];
	      }
	    }
		else
		{
		  this.errorMessage = "Received unexpected or unknown package!";	
		  transferConditionLock.lock();
		  transferCondition.signal();
		  transferConditionLock.unlock();
		}
	}
	
	
	private void receivedData()
	{
        this.log.debug("Clientworker receive called");
		if(this.conn.dataToRead() > 5)
		{
			
		  if(this.cbstate == ClientCallbackState.PuttingFile)
		  {
            this.receivedPacketWhileUploading();
		  }
		  
		  else if(this.cbstate == ClientCallbackState.TransferingFile || 
		          this.cbstate == ClientCallbackState.TransferingFileList ||
		          this.cbstate == ClientCallbackState.AwaitingErrorMessage)
		  {
			/*We're transfering a file, the whole package consist of file data
			  Furthermore this means the package doesn't have an identifier */
			log.info("Reading data, " + Integer.toString(this.conn.dataToRead()) + " bytes available");
			  
			byte[] tempData;
			
			if(this.reduceDataThroughput)
			  tempData = conn.getReceivedData(8096);
			else
			  tempData = conn.getReceivedData(this.bytesToRead);
			
			System.arraycopy(tempData, 0, this.dataRead, this.countRead, tempData.length);
			
			this.bytesToRead -= tempData.length;
			this.countRead += tempData.length;
			
			log.info("Bytes to read: " + this.bytesToRead);
			
			if(this.bytesToRead == 0)
			{
			  //If we're done tell the Client!
			  if(this.cbstate == ClientCallbackState.TransferingFile)	
			    this.cbstate = ClientCallbackState.FileComplete;
			  else if(this.cbstate == ClientCallbackState.TransferingFileList)
				this.cbstate = ClientCallbackState.FileListComplete;
			  else if(this.cbstate == ClientCallbackState.AwaitingErrorMessage)
			  {
				this.cbstate = ClientCallbackState.Error;
				this.errorMessage = this.dataRead.toString();
			  }
			  
			  //Signal the client
			  transferConditionLock.lock();
			  transferCondition.signal();
			  transferConditionLock.unlock();
			}
		  }	
			
		  else if(this.cbstate == ClientCallbackState.AwaitingFile)
		  {
		    this.awaitingFileTransfer(); 
		  }

		  
		  else if(this.cbstate == ClientCallbackState.AwaitingFileList)
		  {  
		    this.awaitingFileList();   
		  }
	   }
	}
	
	public void setCallbackState(ClientCallbackState cbstate)
	{
	  this.cbstate = cbstate;
	}
	
	public ClientCallbackState getCallbackState()
	{
	  return this.cbstate;	
	}
	
	public String getErrorMessage()
	{
	  return this.errorMessage;	
	}
	
	public byte[] getReceivedData()
	{
	  return this.dataRead;	
	}
		
}
