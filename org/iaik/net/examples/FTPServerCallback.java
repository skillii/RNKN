package org.iaik.net.examples;

import java.io.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.iaik.net.RUDP.ConnectionCloseReason;
import org.iaik.net.RUDP.RUDPServerConnection;
import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.interfaces.RUDPServerCallback;
import org.iaik.net.utils.NetUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FTPServerCallback implements RUDPServerCallback{


	Condition transferCondition;
	Lock transferConditionLock;
	
	
	Condition threadCondition;
	Lock threadConditionLock;
	File directory;
    FTPServerWorker worker;	
    String message;
    ConnectionCloseReason reason;

	
	public FTPServerCallback(Condition cond, Lock lock, String directory) throws FileNotFoundException
	{
		this.transferCondition = cond;
		this.transferConditionLock = lock;
		this.directory = new File(directory);
		
		if(!this.directory.exists())
		  throw new FileNotFoundException("Directory existiert nicht");

		
		this.threadConditionLock = new ReentrantLock();
		this.threadCondition = this.threadConditionLock.newCondition();
		
		this.worker = new FTPServerWorker(threadCondition, threadConditionLock, this.directory);
		
		worker.start();
	}
	
	public void setConnection(RUDPServerConnection conn)
	{
	  this.worker.setConnection(conn);  
	}
	
	public String getMessage()
	{
	  return this.worker.getMessage();
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
	



	
	@Override
	public void DataReceived() {
		this.threadConditionLock.lock();
		this.threadCondition.signal();
		this.threadConditionLock.unlock();
		
	}

}
