/* Copyright  (c) 2006-2007 Graz University of Technology. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The names "Graz University of Technology" and "IAIK of Graz University of
 *    Technology" must not be used to endorse or promote products derived from
 *    this software without prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE LICENSOR BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *  OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY  OF SUCH DAMAGE.
 */

package org.iaik.net.layers;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.Network;
import org.iaik.net.StackParameters;
import org.iaik.net.datatypes.interfaces.ARPTable;
import org.iaik.net.exceptions.NetworkException;
import org.iaik.net.factories.LinkLayerFactory;
import org.iaik.net.interfaces.InternetLayer;
import org.iaik.net.interfaces.PhysicalSender;
import org.iaik.net.packets.ARPPacket;
import org.iaik.net.packets.EthernetPacket;
import org.iaik.net.packets.IPPacket;
import org.iaik.net.packets.Packet;
import org.iaik.net.utils.NetUtils;
import org.iaik.net.utils.NetworkBuffer;

/**
 * 
 * @author <a href="mailto:stefan.kraxberger@iaik.tugraz.at">Stefan
 *         Kraxberger</a>
 * @date Jan 11, 2007
 * @version $Rev: 930 $ $Date: 2007/02/15 15:49:05 $
 */
public class DefaultInternetLayer extends Thread implements InternetLayer {

	protected NetworkBuffer receiveBuffer;

	private NetworkBuffer sendBuffer;

	private PhysicalSender sender;

	private boolean running;

	private short identification = 0;

	private Properties properties;

	private Log log;

	public DefaultInternetLayer() {

		log = LogFactory.getLog(this.getClass());
		receiveBuffer = new NetworkBuffer();
		sendBuffer = new NetworkBuffer();

	}

	public void init() throws NetworkException {
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public Properties getProperties() {
		return properties;
	}

	public void run() {
		running = true;
		/*
		 * TODO: Find a better place to instantiate the real JPcap sender. But
		 * currently it is only possible here because the layers are created
		 * before the sender is instantiated.
		 */
		sender = LinkLayerFactory.getInstance().getSender();

		while (running) {

			Packet receivedPacket = receiveBuffer.getNextPacket();

			if (receivedPacket instanceof Packet) {
				if (receivedPacket instanceof ARPPacket) {
					processARP((ARPPacket) receivedPacket);
				} else if (receivedPacket instanceof IPPacket) {
					IPPacket packet = (IPPacket) receivedPacket;

					if (!isValid(packet)) {
						log.info("Packet dropped because the checksum was not valid!");
					} else if (packet.getFlags() == IPPacket.MORE_FRAGMENTS_BIT || packet.getOffset() != 0) {
						/*
						 * TODO: Check if the usage of Exception classes in this
						 * context is useful. Don't think so, because the stack
						 * should not be interrupted. We should only write some
						 * logging information.
						 */
						try {
							reassemble(packet);
						} catch (NetworkException ne) {
							throw new RuntimeException(ne.getMessage());
						}
					} else
						processIP(packet);

				} else {
					/*
					 * TODO: create an interface in order that arbitrary
					 * protocols can be implemented and processed. It will be
					 * necessary to also provide some mechanism in the link
					 * layer.
					 */
				}
			}
			/*
			 * TODO: Create a mechanism which checks if some other data is
			 * available which could be processed and prepared to be sent.
			 */
			checkTimeouts();
			/*
			 * Would it be a good idea to send all the packets currently in the
			 * send buffer or do it one at time and thereafter check if a packet
			 * is available for processing?
			 */
			Packet sendPacket = sendBuffer.getNextPacket();

			if (sendPacket instanceof Packet) {
				sendPacket(sendPacket);
			}
			/*
			 * Wait some time in order give other processes also time for
			 * computation. The other solution would be to only wake up this
			 * thread if either a packet is available for sending or processing.
			 * But this would bring some problems with continued inspection of
			 * timeout because it is not guaranteed that this thread gets
			 * invoked in constant or short enough time intervals.
			 */
			try {
				sleep(StackParameters.POOLING_WAIT_TIME);
			} catch (Exception e) {
				log.error("Wait interrupted!\n" + e.getMessage());
			}
		}
	}

	/**
	 * Returns the receiving buffer which is used by this implementation.
	 * 
	 * @return The receive buffer as {@link NetworkBuffer} object.
	 */
	public NetworkBuffer getReceiveBuffer() {
		return receiveBuffer;
	}

	/**
	 * Returns the ARP table which is used by this implementation.
	 * 
	 * @return The ARP table as {@link ARPTable} object.
	 */
	public ARPTable getARPTable() {
		return null;
	}

	/**
	 * Provides the reassembly algorithm for the IP layer (network layer). This
	 * function takes an received IP packet which has either the MORE_FRAGMENT
	 * bit set or has an offset value greater than zero.
	 * 
	 * @param packet
	 *            A received fragemented IP packet.
	 * @throws NetworkException
	 *             Thrown if the packet couldn't be reassembled.
	 */
	public void reassemble(IPPacket packet) throws NetworkException {

	}

	/**
	 * Verifies if the received IP packet is valid. This includes calculating
	 * and verifying the checksum and other possible checks which are specified
	 * in the RFC.
	 * 
	 * @param packet
	 *            The IP packet which should be verified.
	 * @return True if the packet is correct otherwise false.
	 */
	public boolean isValid(IPPacket packet) {
		return true;
	}

	/**
	 * This function is used for providing the lower layers of the network stack
	 * with the packets which should be sent of the network interface. This
	 * function takes an {@link Packet} object and stores it in the send buffer
	 * which is polled by the linklayer.
	 * 
	 * @param packet
	 *            The packet which should be sent over the network interface to
	 *            the communication counterpart.
	 */
	public void send(Packet packet) {
		if (packet instanceof ARPPacket) {

			sendBuffer.add(EthernetPacket.createEthernetPacket(EthernetPacket.ETHERTYPE_ARP, Network.mac, ((ARPPacket) packet)
					.getTargetHardwareAddress(), packet.getPacket()));

		} else if (packet instanceof IPPacket) {
			/*
			 * MAC address resolution using ARP and the destination IP.
			 */
			if (packet.getTimeout() == 0 || packet.getTimeout() + StackParameters.ARP_TIMEOUT > System.currentTimeMillis()) {

				String destinationMACAddress = resolveAddress(((IPPacket) packet).getDestinationAddress());

				if (!(destinationMACAddress instanceof String)) {
					/*
					 * The address couldn't be resolved, therefore put the
					 * packet back into the receive buffer and set the timeout
					 * of this packet in order that it does not last forever in
					 * the receive buffer if the address couldn't be resolved
					 * during subsequent calls to this function.
					 */
					if (packet.getTimeout() == 0)
						packet.setTimeout(System.currentTimeMillis());

					receiveBuffer.add(packet);
					log.info("Destination address couldn't be resolved: " + ((IPPacket) packet).getDestinationAddress());

					yield();
				} else {
					/*
					 * Creation of the Ethernet packet with the resolved
					 * destination MAC address.
					 */
					sendBuffer.add(EthernetPacket.createEthernetPacket(EthernetPacket.ETHERTYPE_IP, Network.mac, destinationMACAddress,
							packet.getPacket()));
				}
			} else {
				log.info("Timeout for packet sending has been reached, discarding packet!");
			}

		} else {
			log.info("Couldn't send packet because the type is not recognized!");
		}

	}

	/**
	 * This function is used by the upper layer to send packets over the network
	 * interface. Therefore, we assume that the provided payload should be
	 * encapsulated in {@link IPPacket}s. The remoteIP and the protocol
	 * parameters are used to create a new {@link IPPacket} with the provided
	 * payload as data.
	 * 
	 * @param payload
	 *            The payload which should be transmitted by an {@link IPPacket}
	 *            .
	 * @param remoteIP
	 *            The IP address of the remote host.
	 * @param protocol
	 *            The protocol type of the packet which is transmitted by the
	 *            created {@link IPPacket}.
	 */
	public void send(byte[] payload, int destinationIP, byte protocol) {
		IPPacket packet = IPPacket.createDefaultIPPacket(protocol, identification++, Network.ip, NetUtils.ipIntToString(destinationIP),
				payload);

		send(packet);
	}

	public void send(byte[] payload, String destinationIP, byte protocol) {
		IPPacket packet = IPPacket.createDefaultIPPacket(protocol, identification++, Network.ip, destinationIP, payload);

		send(packet);

	}

	/**
	 * Processes an received ARP packet. The normal response is an ARP reply
	 * with the MAC address for this implementation. This function creates an
	 * ARP reply packet and uses the {@link #send(Packet)} function to send it
	 * down the network stack to the sender.
	 * 
	 * @param packet
	 *            The received ARP request.
	 */
	public void processARP(ARPPacket packet) {
	}

	/**
	 * Processes an received IP packet. If the IP packet contains an ICMP, IGMP
	 * or OSPF packet they are processed directly in this function. A response
	 * is created which is then sent to the sender of this IP packet using the
	 * {@link #send(Packet)} function. If the IP packet contains an other type
	 * of packet it is provided as input to the transport layer.
	 * 
	 * @param packet
	 *            The received IP packet.
	 */
	public void processIP(IPPacket packet) {

	}

	/**
	 * Returns the network send buffer object.
	 * 
	 * @return The send buffer as {@link NetworkBuffer} object.
	 */
	public NetworkBuffer getSendBuffer() {
		return sendBuffer;
	}

	/**
	 * Stops this thread and closes all open handles to the different buffers
	 * and sets this thread to not running.
	 */
	public void terminate() {
		running = false;
		sendBuffer = null;
		receiveBuffer = null;
	}

	/**
	 * This function resolves the provided IP address to an underlying MAC
	 * address and returns it. If the IP address relies on the same subnet the
	 * real MAC address is returned. If the remote IP resides outside the subnet
	 * the gateway address is returned. If there exists currently no cached
	 * entry for the specified remote IP address an ARP request is created and
	 * null is returned. The calling method should than block sending this
	 * packet until the ARP request has been resolved.
	 * 
	 * @param remoteIP
	 *            The remote address which should be resolved.
	 * @return The resolved MAC address of the specified IP address.
	 */
	private String resolveAddress(String remoteIP) {

		return null;
	}

	/**
	 * Determines if the specified remote IP address is on the same subnet. If
	 * it is on the same subnet this function returns true otherwise false.
	 * 
	 * @param remoteAddr
	 *            The remote IP address which should be checked.
	 * @return If the remote address is on the same subnet or if no gateway is
	 *         given true is returned.
	 */
	private boolean isSameSubnet(String remoteAddr) {
		if (!(Network.gateway instanceof String))
			return true;

		return ((NetUtils.ipStringToInt(Network.ip) & NetUtils.ipStringToInt(Network.netmask)) ^ (NetUtils.ipStringToInt(remoteAddr) & NetUtils
				.ipStringToInt(Network.netmask))) == 0;
	}

	/**
	 * Can be used to verify and check timeouts from other layers for which this
	 * layer has interest.
	 */
	public void checkTimeouts() {
	}

	/**
	 * Sends the provided packet over the network device interface.
	 * 
	 * @param p
	 *            The packet which should be sent over this interface.
	 */
	public void sendPacket(Packet packet) {
		sender.send(packet);
	}
}
