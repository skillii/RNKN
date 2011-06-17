package org.iaik.net.examples;

public class FTPCmdError extends FTPCommand {
	
	
	String message;

	@Override
	public byte[] getCommand() 
	{
	  byte[] arrayToString = message.getBytes();
			
	  byte[] pkg = new byte[arrayToString.length + 1];
			
      pkg[0] = this.identifier;
      System.arraycopy(arrayToString, 0, pkg, 1, arrayToString.length);
			
	  return pkg;
	}
		
	public FTPCmdError(String message)
	{
      this.message = message;
	  this.identifier = 0x10;
	}
		
		
	private FTPCmdError(byte[] packet)
	{
      this.identifier = packet[0];
		  
	  byte[] data = new byte[packet.length - 1];
		  
	  System.arraycopy(packet, 1, data, 0, packet.length -1);

	  this.message = new String(data);
	}
		
		
	public String getMessage()
	{
      return this.message;	
	}
		
		
	public static FTPCmdError createFTPCmdError(byte[] packet)
    {
      return new FTPCmdError(packet);	
	}


}
