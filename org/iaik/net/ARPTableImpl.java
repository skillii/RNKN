package org.iaik.net;

import java.util.Vector;

import org.iaik.net.datatypes.interfaces.*;
import org.iaik.net.packets.ARPPacket;


public class ARPTableImpl implements ARPTable{

	private Vector<ARPEntry> list;
	
	public ARPTableImpl()
	{
		
		list = new Vector<ARPEntry>();
	}
	
	@Override
	public void add(ARPEntry entry) {

		list.add(entry);

	}

	@Override
	public void add(ARPPacket packet) {
		ARPEntry e = new ARPEntryImpl();
		e.setMACAddress(packet.getSenderHardwareAddress());
		e.setIPAddress(packet.getSenderProtocolAddress());
		e.setValid(true);
		list.add(e);
	}

	@Override
	public boolean contains(ARPEntry entry) {
		if(	list.indexOf(entry) != -1)
			return true;
		return false;
	}

	@Override
	public boolean contains(ARPPacket packet) {
		ARPEntry e = new ARPEntryImpl();
		e.setMACAddress(packet.getSenderHardwareAddress());
		e.setIPAddress(packet.getSenderProtocolAddress());
		e.setValid(true);
		if(list.indexOf(e) != -1)
			return true;
		
		return false;
	}

	@Override
	public ARPEntry findEntryForIP(String ipaddress) {
		if(list.isEmpty())
			return null;
		int i=0;
		ARPEntry e = new ARPEntryImpl();
		while(i < list.size())
		{
			e = list.get(i);
			if(ipaddress.equals(e.getIPAddress()))
				return e;
			i++;
		}
		return null;
	}

	@Override
	public ARPEntry findEntryForMAC(String macaddress) {
		if(list.isEmpty())
			return null;
		int i=0;
		ARPEntry e;
		while(i < list.size())
		{
			e = list.get(i);
			if(macaddress.equals(e.getMACAddress()))
				return e;
			i++;
		}
		return null;
	}

	@Override
	public long getTableSize() {
		return list.size();
	}

	@Override
	public int indexOf(ARPEntry p) {
		return list.indexOf(p);
	}

	@Override
	public int indexOf(ARPPacket p) {
		ARPEntry e = new ARPEntryImpl();
		e.setMACAddress(p.getSenderHardwareAddress());
		e.setIPAddress(p.getSenderProtocolAddress());
		e.setValid(true);
		return list.indexOf(e);
	}

	@Override
	public void remove(ARPEntry entry) {
		list.remove(entry);
	}

	@Override
	public String resolveIPAddress(String ipaddress) {
		if(list.isEmpty())
			return null;
		int i=0;
		ARPEntry e = new ARPEntryImpl();
		while(i < list.size())
		{
			e = list.get(i);
			if(ipaddress.equals(e.getIPAddress()))
				return e.getMACAddress();
			i++;
		}
		return null;
	}
	

}
