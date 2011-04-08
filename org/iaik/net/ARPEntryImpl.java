package org.iaik.net;

import org.iaik.net.datatypes.interfaces.*;

public class ARPEntryImpl implements ARPEntry{

	private String  ip;
	private String mac;
	private boolean valid;
	
	ARPEntryImpl()
	{
		ip=null;
		mac=null;
		valid=false;
	}
	
	ARPEntryImpl(String ipAdd, String macAdd, boolean val)
	{
		ip = ipAdd;
		mac = macAdd;
		valid = val;
	}
	
	@Override
	public String getIPAddress() {
		
		return ip;
	}

	@Override
	public String getInfo() {
		
		return mac;
	}

	@Override
	public String getMACAddress() {
		
		return mac;
	}

	@Override
	public boolean isValid() {
		
		return valid;
	}

	@Override
	public void setIPAddress(String ipAddress) {
		ip = ipAddress;
		
	}

	@Override
	public void setMACAddress(String macAddress) {
		mac = macAddress;
		
	}

	@Override
	public void setValid(boolean vali) {
		valid = vali;
		
	}
	
	public boolean equals( ARPEntryImpl entry)
	{
		if(this.getIPAddress().equals(entry.getIPAddress()) && this.getMACAddress().equals(entry.getMACAddress()) && (this.isValid() == entry.isValid()))
			return true;
		else return false;
			
	}
	

}
