package org.iaik.net.interfaces;

import java.util.Properties;

import org.iaik.net.datatypes.interfaces.ARPTable;
import org.iaik.net.exceptions.NetworkException;
import org.iaik.net.packets.Packet;
import org.iaik.net.utils.NetworkBuffer;

public interface InternetLayer extends Runnable {

	public void init() throws NetworkException;

	public void setProperties(Properties properties);

	public Properties getProperties();

	public void checkTimeouts();

	public void terminate();

	public NetworkBuffer getReceiveBuffer();

	public NetworkBuffer getSendBuffer();

	public ARPTable getARPTable();

	public void send(Packet packet);

	public void send(byte[] data, int address, byte protocol);

	public void send(byte[] data, String address, byte protocol);
}
