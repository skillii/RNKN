package org.iaik.net.examples;

import org.iaik.net.exceptions.PacketParsingException;

/*
 * Identifier Protocol
 * 
 * 0x01 FTPCmdListFiles
 * 0x02 FTPCmdGetFile
 * 0x04 FTPCmdPutFile
 * 0x08 FTPCmdFileData
 * 0x10 FTPCmdError
 * 
 */



public abstract class FTPCommand {
	byte identifier;
	
	
    public abstract byte[] getCommand();
    
    
    /**
     * This method returns an instance of the class the packet equals to.
     * @param packet The packet to be parsed
     * @return The class instance or null
     */
      
    public static FTPCommand parseFTPCommand(byte[] packet) throws PacketParsingException
    {
      byte identifier = packet[0];
      
      if((identifier & 0x01) == 1)
        return FTPCmdListFiles.createFTPCmdListFiles(packet);
      
      if((identifier & 0x02) == 1)
        return FTPCmdGetFile.createFTPCmdGetFile(packet);
      
      
      if((identifier & 0x04) == 1)
        return FTPCmdGetFile.createFTPCmdGetFile(packet);
      
      if((identifier & 0x08) == 1)
    	return null;
      
      if((identifier & 0x10) == 1)
    	return null;
      
      
      throw new PacketParsingException("Couldn't identify command type");

    }
}
