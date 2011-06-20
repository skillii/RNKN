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

import jpcap.JpcapCaptor;
import jpcap.PacketReceiver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.StackParameters;
import org.iaik.net.datatypes.interfaces.ARPTable;
import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.interfaces.LinkLayer;
import org.iaik.net.packets.ARPPacket;
import org.iaik.net.packets.EthernetPacket;
import org.iaik.net.packets.IPPacket;
import org.iaik.net.utils.NetUtils;
import org.iaik.net.utils.NetworkBuffer;

/**
 * 
 * @author <a href="mailto:stefan.kraxberger@iaik.tugraz.at">Stefan
 *         Kraxberger</a>
 * @date Jan 11, 2007
 * @version $Rev: 930 $ $Date: 2007/02/14 16:09:19 $
 */
public class JPcapReceiver extends Thread implements PacketReceiver {

	private JpcapCaptor receiver;

	private NetworkBuffer receiveBuffer;

	private ARPTable arpTable;

	private LinkLayer iface;

	private Log log;

	private boolean running;

	private int prevSrc = 0;

	private int prevDst = 0;

	private int prevChecksum = 0;

	/**
	 * This
	 * 
	 * @param iface
	 *            The interface which should be used for capturing
	 */
	public JPcapReceiver(LinkLayer iface, JpcapCaptor captor) {
		log = LogFactory.getLog(this.getClass());

		if (iface == null || iface instanceof LinkLayer)
			this.iface = iface;
		else {
			log.debug("Provided argument must be of type jpcap.NetworkInterface!");
			throw new IllegalArgumentException("Provided argument must be of type jpcap.NetworkInterface!");
		}

		if (receiver == null || receiver instanceof JpcapCaptor)
			this.receiver = captor;
		else {
			log.debug("Provided argument must be of type jpcap.JpcapCaptor!");
			throw new IllegalArgumentException("Provided argument must be of type jpcap.JpcapCaptor!");
		}

		running = false;

		receiveBuffer = iface.getReceiveBuffer();
		arpTable = iface.getARPTable();

	}

	/**
	 * Callback function from the capture device. This function is called if a
	 * packet is available on the network device interface. The packet is
	 * provided as input parameter to this function.
	 */
	public void receivePacket(jpcap.packet.Packet capturedPacket) {
		try {
			EthernetPacket p = EthernetPacket.createEthernetPacket(capturedPacket.header, capturedPacket.data);

			//log.debug("Packet on wire:\n" + p.getInfo());

			/**
			 * Test if the packet is of a recognized type and does belong to us.
			 * If it is an ARP request we save the ARP information and
			 * immediately create an ARP response. The ARP information is stored
			 * in the ARPTable for future use in our stack. If it is a IP packet
			 * the packet is written into the network buffer where the further
			 * processing initated by the linklayer through informing the
			 * network layer that
			 */
			if (p.getFrameType() == EthernetPacket.ETHERTYPE_ARP) {
				ARPPacket arp = new ARPPacket(p);

				if (arp.getTargetProtocolAddress().equalsIgnoreCase(iface.getIPAddress())
						&& !arp.getSenderProtocolAddress().equalsIgnoreCase(iface.getIPAddress())) {
					// TODO: Should we add the ARP already in the link layer to
					// the ARP table? Because the network layer has than nothing
					// else to do than to reply to ARP requests.
					
					arpTable.add(arp);
					
					receiveBuffer.add(arp);

					//log.info("should do");
					//log.debug("asdfasdfasdf");
					//log.info(arp.getInfo());

				} else {
					// TODO: Should we really add every ARP request we get to
					// the ARP table.
					//arpTable.add(arp);
					//log.info("should do");
					//log.debug("asdfasdfasdf");
				}
			} else if (p.getFrameType() == EthernetPacket.ETHERTYPE_IP) {
				IPPacket ip = IPPacket.createIPPacket(p);

				if (ip.getDestinationAddress().equalsIgnoreCase(iface.getIPAddress())
						&& !ip.getSourceAddress().equalsIgnoreCase(iface.getIPAddress())) {
					// TODO: Hack to circumvent double reception of packets from
					// within a VMWare.

					//TODO: hack?? works for me without this hack!
					if (true || prevDst != NetUtils.ipStringToInt(ip.getDestinationAddress())
							|| prevSrc != NetUtils.ipStringToInt(ip.getSourceAddress()) || prevChecksum != ip.getChecksum()) {
						// Store the values locally to use it for comparison
						// later.
						prevDst = NetUtils.ipStringToInt(ip.getDestinationAddress());
						prevSrc = NetUtils.ipStringToInt(ip.getSourceAddress());
						prevChecksum = ip.getChecksum();
						// Add the IP packet to the receive buffer for further
						// processing.
						receiveBuffer.add(ip);

						//log.info(ip.getInfo());
					} else {
						log.debug("Received the same packet twice!");
					}

				} else if (ip.getDestinationAddress().equalsIgnoreCase(StackParameters.BROADCAST_MAC_ADDRESS)
						&& ip.getProtocol() == IPPacket.UDP_PROTOCOL) {
					log.info("DHCP packet received!");

					// TODO:
				}

			} else {
				log.debug("Unknown or not handled packet format!");
				//log.debug(p.getInfo());
			}

		} catch (PacketParsingException pae) {
			log.info("Received packet couldn't be parsed: " + pae.getMessage());
		}
	}

	public void run() {
		log.debug("Start capurting on device.\n" + iface.getInfo());
		if (!running)
			receiver.loopPacket(-1, this);
	}

	public void terminate() {
		receiver.breakLoop();
		running = false;
	}
}
