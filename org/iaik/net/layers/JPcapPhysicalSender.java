package org.iaik.net.layers;

import jpcap.JpcapSender;

import org.iaik.net.interfaces.LinkLayer;
import org.iaik.net.interfaces.PhysicalSender;
import org.iaik.net.packets.EthernetPacket;
import org.iaik.net.packets.Packet;
import org.iaik.net.utils.NetUtils;

public class JPcapPhysicalSender implements PhysicalSender {

	private JpcapSender sender;

	private LinkLayer linklayer;

	public JPcapPhysicalSender(LinkLayer linklayer, JpcapSender sender) {
		this.linklayer = linklayer;
		this.sender = sender;
	}

	public void send(Packet p) {
		// obtain a packet from the send buffer;
		jpcap.packet.Packet packet = convertPacket(p);
		sender.sendPacket(packet);
	}

	// TODO: change the address resolution for the ethernet packet. The hardware
	// address
	// for the destination node should be found by using the ARP table.
	public jpcap.packet.Packet convertPacket(Packet p) {
		EthernetPacket ethernet = (EthernetPacket) p;
		jpcap.packet.EthernetPacket ether = new jpcap.packet.EthernetPacket();
		jpcap.packet.Packet send = new jpcap.packet.Packet();

		ether.src_mac = NetUtils.addressToBytes(linklayer.getMACAddress(), ":");
		ether.dst_mac = NetUtils.addressToBytes(ethernet.getDestinationAddress(), ":");
		ether.frametype = ethernet.getFrameType();

		send.data = ethernet.getPayload();
		send.datalink = ether;

		return send;
	}

}
