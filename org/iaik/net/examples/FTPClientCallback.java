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

	
	public FTPClientCallback(Condition transferCondition, Lock transferConditionLock)
	{
	}
	
	public void setConnection(RUDPClientConnection conn)
	{
	  this.conn = conn;	
	}
	
	
	@Override
	public void DataReceived() {
		byte[] tempData;
		 
		
		if(this.cbstate == ClientCallbackState.PuttingFile)
		{
		  //Kann nur ein Errorpaket zurückkommen...
          this.cbstate = ClientCallbackState.Error;
		    
		  tempData = conn.getReceivedData(4096);
		    
		  try 
		  {
		    FTPCmdError cmd = (FTPCmdError)FTPCommand.parseFTPCommand(tempData);
		    this.errorMessage = cmd.getMessage(); 
		      
		    transferConditionLock.lock();
			transferCondition.signal();
			transferConditionLock.unlock();
		   } 
		   catch (PacketParsingException e) 
		   {
		     this.errorMessage = "Failed parsing FTP command";
		      	
		     transferConditionLock.lock();
			 transferCondition.signal();
			 transferConditionLock.unlock();
		   }	
		}
		
		if(this.cbstate == ClientCallbackState.AwaitingFile)
		{
		  tempData = conn.getReceivedData(4096);
		  
		  byte identifier = tempData[0];
		   
		  //Yeehaw baby, the file header is boomboxing
		  if((identifier & 0x08) != 0)
		  {
		     //Find out how big the file is
			 //(We assume this information arrived in the first RUDP Packet as well)
			 this.bytesToRead = NetUtils.bytesToInt(tempData, 1);
			 
			 //Now read the rest of the data that came with the first package
			 this.dataRead = new byte[tempData.length - 5];
			 System.arraycopy(tempData, 5, this.dataRead, 0, tempData.length - 5);
			 
			 //Reduce counter of bytes to read
			 this.bytesToRead -= tempData.length - 5;
			 
			 this.cbstate = ClientCallbackState.TransferingFile;
		  }
		  
		  //We didn't get the file, but got an FTP error back
		  else if((identifier & 0x10) != 0)
		  {
		    this.cbstate = ClientCallbackState.Error;
		    
		    tempData = conn.getReceivedData(4096);
		    
		    try 
		    {
		      FTPCmdError cmd = (FTPCmdError)FTPCommand.parseFTPCommand(tempData);
		      this.errorMessage = cmd.getMessage(); 
		      
		      transferConditionLock.lock();
			  transferCondition.signal();
			  transferConditionLock.unlock();
			} 
		    catch (PacketParsingException e) 
		    {
		      this.errorMessage = "Failed parsing FTP command";
		      	
		      transferConditionLock.lock();
			  transferCondition.signal();
			  transferConditionLock.unlock();
			}
		  }
		  
		}
		
		if(this.cbstate == ClientCallbackState.TransferingFile || 
		   this.cbstate == ClientCallbackState.TransferingFileList)
		{
			/*We're transfering a file, the whole package consist of file data
			  Furthermore this means the package doesn't have an identifier */
			
			tempData = conn.getReceivedData(4096);
			
			byte[] temp = new byte[tempData.length + this.dataRead.length];
			
			//Copy old data and insert new data
			System.arraycopy(this.dataRead, 0, temp, 0, this.dataRead.length);
			System.arraycopy(tempData, 0, temp, this.dataRead.length + 1, temp.length);
			
			this.bytesToRead -= tempData.length;
			
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
		
		if(this.cbstate == ClientCallbackState.AwaitingFileList)
		{
		   tempData = conn.getReceivedData(4096);
		   
		   byte identifier = tempData[0];
		   
		   if((identifier & 0x01) != 0)
		   {
		     this.bytesToRead = NetUtils.bytesToInt(tempData, 1);
		     
		     
		     this.dataRead = new byte[this.bytesToRead];
		     System.arraycopy(tempData, 0, this.dataRead, 5, tempData.length - 5);
		       
		     this.bytesToRead -= tempData.length;
		     
		     if(this.bytesToRead > 0)
		     {
		       //This means the file list didn't make it in one package
		       this.cbstate = ClientCallbackState.TransferingFileList;
		     }
		     else
		     {
		       //Filelist complete, wake up the client	 
		       this.cbstate = ClientCallbackState.FileListComplete;
		       transferConditionLock.lock();
			   transferCondition.signal();
			   transferConditionLock.unlock();
		     }
		     
		   }
		   else if((identifier & 0x10) != 0)
		   {
		     this.cbstate = ClientCallbackState.Error;
			    
			 tempData = conn.getReceivedData(4096);
			    
			 try 
			 {
			   FTPCmdError cmd = (FTPCmdError)FTPCommand.parseFTPCommand(tempData);
			   this.errorMessage = cmd.getMessage(); 
			      
			   transferConditionLock.lock();
			   transferCondition.signal();
			   transferConditionLock.unlock();
		     } 
			 catch (PacketParsingException e) 
			 {
			   this.errorMessage = "Failed parsing FTP command";
			      	
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
