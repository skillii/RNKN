package org.iaik.net.examples;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.iaik.net.RUDP.RUDPClientConnection;
import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.interfaces.RUDPClientCallback;
import org.iaik.net.RUDP.RUDPConnection;

public class FTPClientCallback implements RUDPClientCallback {
	
	public enum ClientCallbackState{
		AwaitingFile,
		AwaitingFileList,
		Error,
		FileListComplete,
		FileComplete,
		Other}
	
	ClientCallbackState cbstate;
	RUDPClientConnection conn;
	Condition transferCondition;
	Lock transferConditionLock;
	String errorMessage;
	byte[] dataRead;
	int bytesToRead;

	
	public FTPClientCallback(RUDPClientConnection conn, Condition transferCondition, Lock transferConditionLock)
	{
	  this.conn = conn;	
	}
	
	
	@Override
	public void ConnectionClosed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void DataReceived() {
		byte[] tempData;
		
		if(this.cbstate == ClientCallbackState.AwaitingFile)
		{
		  tempData = conn.getReceivedData(1024);
		  
		  byte identifier = tempData[0];
		   
		  if((identifier & 0x08) != 0)
		  {
			  
		  }
		  
		  if((identifier & 0x10) != 0)
		  {
		    this.cbstate = ClientCallbackState.Error;
		    
		    tempData = conn.getReceivedData(1024);
		    
		    try 
		    {
		      FTPCommand.parseFTPCommand(tempData);
			} 
		    catch (PacketParsingException e) 
		    {
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
	
	public String getErrorMessage()
	{
	  return this.errorMessage;	
	}

}
