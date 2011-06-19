package org.iaik.net.examples;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.iaik.net.RUDP.ConnectionCloseReason;
import org.iaik.net.RUDP.RUDPClientConnection;
import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.interfaces.RUDPClientCallback;
import org.iaik.net.utils.NetUtils;

public class FTPClientCallback implements RUDPClientCallback {
	
	public enum ClientCallbackState{
		AwaitingFile,
		AwaitingFileList,
		Error,
		FileListComplete,
		FileComplete,
		TransferingFile,
		TransferingFileList,
		PuttingFile}
	
	ClientCallbackState cbstate;
	RUDPClientConnection conn;
	Condition transferCondition;
	Lock transferConditionLock;
	String errorMessage;
	ConnectionCloseReason discon_reason;
	
	byte[] dataRead;
	
	int bytesToRead;
	int countRead;

	
	public FTPClientCallback(Condition transferCondition, Lock transferConditionLock)
	{
	}
	
	public void setConnection(RUDPClientConnection conn)
	{
	  this.conn = conn;	
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
	    
	    if((identifier & FTPCommand.FILE_DATA_IDENTIFIER) != 0)
	    {
	      //Find out how big the file is
		  //(We assume this information arrived in the first RUDP Packet as well)
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
	      while(true)
	      {
	        if(this.conn.dataToRead() >= size)
	        {
	          byte[] tempData = conn.getReceivedData(size);	
	          
	          try 
			  {
			    FTPCmdError cmd = (FTPCmdError)FTPCommand.parseFTPCommand(tempData);
			    this.errorMessage = cmd.getMessage(); 
			      
			    transferConditionLock.lock();
				transferCondition.signal();
				transferConditionLock.unlock();
				break;
			  } 
			  catch (PacketParsingException e) 
			  {
			    this.errorMessage = "Failed parsing FTP command";
			      	
			    transferConditionLock.lock();
				transferCondition.signal();
				transferConditionLock.unlock();
				break;
			  }	
	        }
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
	
	
	@Override
	public void DataReceived() {

		
		if(this.conn.dataToRead() > 5)
		{
			
		  if(this.cbstate == ClientCallbackState.PuttingFile)
		  {
            this.receivedPacketWhileUploading();
		  }
		
		  if(this.cbstate == ClientCallbackState.AwaitingFile)
		  {
		    this.awaitingFileTransfer(); 
		  }
		  
		  if(this.cbstate == ClientCallbackState.AwaitingFileList)
		  {  
		    this.awaitingFileList();   
		  }
		  
		  if(this.cbstate == ClientCallbackState.TransferingFile || 
		     this.cbstate == ClientCallbackState.TransferingFileList)
		  {
			/*We're transfering a file, the whole package consist of file data
			  Furthermore this means the package doesn't have an identifier */
			
			byte[] tempData = conn.getReceivedData(this.bytesToRead);
			
			System.arraycopy(tempData, 0, this.dataRead, this.countRead, tempData.length);
			
			this.bytesToRead -= tempData.length;
			this.countRead += tempData.length;
			
			if(this.bytesToRead == 0)
			{
			  //If we're done tell the Client!
			  if(this.cbstate == ClientCallbackState.TransferingFile)	
			    this.cbstate = ClientCallbackState.FileComplete;
			  else if(this.cbstate == ClientCallbackState.TransferingFileList)
				this.cbstate = ClientCallbackState.FileListComplete;
			  transferConditionLock.lock();
			  transferCondition.signal();
			  transferConditionLock.unlock();
			}
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


	@Override
	public void ConnectionClosed(ConnectionCloseReason reason) {
		this.discon_reason = reason;
		
	}

}
