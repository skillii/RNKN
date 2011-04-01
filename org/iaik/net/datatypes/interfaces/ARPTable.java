package org.iaik.net.datatypes.interfaces;

import org.iaik.net.packets.ARPPacket;

public interface ARPTable {
	public void add(ARPEntry entry);
	
	public void add(ARPPacket packet);
	
	public void remove(ARPEntry entry);

	public boolean contains(ARPEntry entry);

	public boolean contains(ARPPacket packet);

	public int indexOf(ARPEntry p);

	public int indexOf(ARPPacket p);

	public ARPEntry findEntryForIP(String ipaddress);

	public ARPEntry findEntryForMAC(String macaddress);

	public long getTableSize();

	public String resolveIPAddress(String ipaddress);
}
