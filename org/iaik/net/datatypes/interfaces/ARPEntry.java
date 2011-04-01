package org.iaik.net.datatypes.interfaces;

public interface ARPEntry {
	public String getIPAddress();

	public void setIPAddress(String ipAddress);

	public String getMACAddress();

	public void setMACAddress(String macAddress);

	public boolean isValid();

	public void setValid(boolean valid);

	public String getInfo();
}
