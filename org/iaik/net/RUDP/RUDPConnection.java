package org.iaik.net.RUDP;

public abstract class RUDPConnection implements Runnable {
	void sendData() {
		
	}
	
	void getReceivedData() {
		
	}
	
	void disconnect() {
		
	}

	@Override
	public void run() {
		while(true)
		{
			if(!isConnected())
			{
				//the connect phase differs from server to client, so
				//we make a polymorph call here.
				connectPhase();
				continue;
			}
			else
			{
				//now we're connected so here we can do the data-send stuff...
			}
		}
	}
	
	public abstract boolean isConnected();
	public abstract void connectPhase();
	
}
