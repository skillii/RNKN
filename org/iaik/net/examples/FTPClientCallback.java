package org.iaik.net.examples;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.iaik.net.examples.FTPClientWorker.ClientCallbackState;

import org.iaik.net.RUDP.ConnectionCloseReason;
import org.iaik.net.RUDP.RUDPClientConnection;
import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.interfaces.RUDPClientCallback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.utils.NetUtils;

public class FTPClientCallback implements RUDPClientCallback {
	

	RUDPClientConnection conn;
	FTPClientWorker worker;
	Condition transferCondition;
	Lock transferConditionLock;
	Condition threadCondition;
	Lock threadConditionLock;
	ConnectionCloseReason discon_reason;

	public FTPClientCallback(Condition transferCondition, Lock transferConditionLock)
	{
	  this.transferCondition = transferCondition;
	  this.transferConditionLock = transferConditionLock;
      this.threadConditionLock = new ReentrantLock();
	  this.threadCondition = transferConditionLock.newCondition();
	  this.worker = new FTPClientWorker(this.threadCondition,this.threadConditionLock,
			                            this.transferCondition,this.transferConditionLock);
	  
	  worker.start();
	}
	
	public void setConnection(RUDPClientConnection conn)
	{
	  this.conn = conn;	
	  this.worker.setConnection(conn);
	}
	
	
	
	
	
	@Override
	public void DataReceived() {
		  //this.threadConditionLock.lock();
          this.threadCondition.signal();
          //this.threadConditionLock.unlock();
	}	
	
	public void setCallbackState(ClientCallbackState cbstate)
	{
	  this.worker.setCallbackState(cbstate);
	}
	
	public ClientCallbackState getCallbackState()
	{
	  return this.worker.getCallbackState();	
	}
	
	public String getErrorMessage()
	{
	  return this.worker.getErrorMessage();	
	}
	
	public byte[] getReceivedData()
	{
	  return this.worker.getReceivedData();	
	}

	@Override
	public void ConnectionClosed(ConnectionCloseReason reason) 
	{
	  this.discon_reason = reason;
	  this.worker.setCallbackState(ClientCallbackState.Disconnected);
	  this.transferCondition.signal();
	}
}
