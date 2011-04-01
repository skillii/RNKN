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

package org.iaik.net.packets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.utils.NetUtils;

/**
 * 
 * @author <a href="mailto:stefan.kraxberger@iaik.tugraz.at">Stefan
 *         Kraxberger</a>
 * @date Jan 11, 2007
 * @version $Rev: 930 $ $Date: 2007/02/05 11:33:53 $
 */
public class EthernetPacket implements Packet {

	private String sourceAddress;

	private String destinationAddress;

	/** Type of the Ethernet frame. (int) could be replaced with (short) */
	private short frameType;

	/** PUP protocol */
	public static final short ETHERTYPE_PUP = 0x0200;

	/** IP protocol */
	public static final short ETHERTYPE_IP = 0x0800;

	/** Chaosnet protocol */
	public static final short ETHERTYPE_CHAOSNET = 0x0804;

	/** X.25 Level 3 protocol */
	public static final short ETHERTYPE_X25 = 0x0805;

	/** Addr. resolution protocol */
	public static final short ETHERTYPE_ARP = 0x0806;

	/** Frame relay protocol */
	public static final short ETHERTYPE_FRAME_RELAY = 0x0808;

	/** Raw Frame relay protocol */
	public static final short ETHERTYPE_RAW_FRAME_RELAY = 0x6559;

	/** reverse Addr. resolution protocol */
	public static final short ETHERTYPE_REVARP = (short) 0x8035;

	/** AppleTalsk (EtherTalk) protocol */
	public static final short ETHERTYPE_APPLETALK = (short) 0x809B;

	/** AppleTalsk ARP protocol (AARP) */
	public static final short ETHERTYPE_APPLETALK_ARP = (short) 0x80F3;

	/** IEEE 802.1Q VLAN tagging */
	public static final short ETHERTYPE_VLAN = (short) 0x8100;

	/** NOVELL IPX protocol */
	public static final short ETHERTYPE_NOVELL_IPX = (short) 0x8137;

	/** NOVELL network protocol */
	public static final short ETHERTYPE_NOVELL = (short) 0x8138;

	/** IPv6 */
	public static final short ETHERTYPE_IPV6 = (short) 0x86dd;

	/** Simple Network Management protocol */
	public static final short ETHERTYPE_SNMP = (short) 0x814C;

	/** Point-to-point protocol */
	public static final short ETHERTYPE_PPP = (short) 0x880B;

	/** General Switch Management protocol */
	public static final short ETHERTYPE_GSMP = (short) 0x880C;

	/** Multi-Protocol Label Switching protocol (unicast) */
	public static final short ETHERTYPE_MPLS_UNI = (short) 0x8847;

	/** Multi-Protocol Label Switching protocol (multiscast) */
	public static final short ETHERTYPE_MPLS_MULTI = (short) 0x8848;

	/** Point-to-Point over Ethernet Discovery protocol */
	public static final short ETHERTYPE_PPPoE_DISCOVERY = (short) 0x8863;

	/** Point-to-Point over Ethernet Session protocol */
	public static final short ETHERTYPE_PPPoE_SESSION = (short) 0x8864;

	/** Real-time ethernet PROFINET */
	public static final short ETHERTYPE_PROFINET = (short) 0x8892;

	/** Real-time ethernet EtherCat */
	public static final short ETHERTYPE_ETHERCAT = (short) 0x88A4;

	/** Lightweight Access Point Protocol protocol */
	public static final short ETHERTYPE_LWAPP = (short) 0x88BB;

	/** Link Layer Discovery Protocol */
	public static final short ETHERTYPE_LLDP = (short) 0x88CC;

	/** Real-time ethernet Sercos III */
	public static final short ETHERTYPE_SERCOS_III = (short) 0x88CD;

	/** used to test interfaces */
	public static final short ETHERTYPE_LOOPBACK = (short) 0x9000;

	/** VLAN Tag Protocol Identifier protocol */
	public static final short ETHERTYPE_VLAN_TPI_1 = (short) 0x9100;

	/** VLAN Tag Protocol Identifier protocol */
	public static final short ETHERTYPE_VLAN_TPI_2 = (short) 0x9200;

	/** IEEE802.3 length field */
	public static final short ETHERTYPE_802_3 = 0x05DC;

	/** Cabletron Interswitch Message Protocol */
	public static final short ETHERTYPE_CABLETRON = (short) 0x81FD;

	private long timeout = 0;

	private byte[] payload;

	private Log log;

	private EthernetPacket(byte[] header, byte[] payload) throws PacketParsingException {
		log = LogFactory.getLog(this.getClass());
		parseHeader(NetUtils.getFromByteArray(header, 0, 14));

		if (header.length > 14) {
			this.payload = NetUtils.concatData(NetUtils.getFromByteArray(header, 14, header.length - 14), payload);
		} else
			this.payload = payload;

	}

	private EthernetPacket(short type, String sourceAddress, String destinationAddress, byte[] payload) {
		log = LogFactory.getLog(this.getClass());
		this.frameType = type;
		this.sourceAddress = sourceAddress;
		this.destinationAddress = destinationAddress;
		this.payload = payload;
	}

	public static EthernetPacket createEthernetPacket(byte[] header, byte[] payload) throws PacketParsingException {
		return new EthernetPacket(header, payload);
	}

	public static EthernetPacket createEthernetPacket(short type, String sourceAddress, String destinationAddress, byte[] payload) {
		return new EthernetPacket(type, sourceAddress, destinationAddress, payload);
	}

	public byte[] getPacket() {
		byte[] packet = null;

		if (payload instanceof byte[])
			packet = new byte[14 + payload.length];
		else
			packet = new byte[14];

		NetUtils.insertData(packet, NetUtils.shortToBytes(frameType), 0);
		NetUtils.insertData(packet, NetUtils.addressToBytes(sourceAddress, ":"), 2);
		NetUtils.insertData(packet, NetUtils.addressToBytes(destinationAddress, ":"), 8);
		if (payload instanceof byte[])
			NetUtils.insertData(packet, payload, 14);
		return packet;
	}

	public byte[] getPayload() {
		return payload;
	}

	public String getDestinationAddress() {
		return destinationAddress;
	}

	public short getFrameType() {
		return frameType;
	}

	public String getSourceAddress() {
		return sourceAddress;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	private void parseHeader(byte[] header) throws PacketParsingException {

		if (header instanceof byte[]) {

			destinationAddress = NetUtils.toHexString(header, 0, 6);

			sourceAddress = NetUtils.toHexString(header, 6, 6);

			frameType = NetUtils.bytesToShort(header, 12);

			log.debug("Ethernet header successful parsed.");

		} else
			throw new PacketParsingException("Header must not be null!");
	}

	public String getInfo() {
		StringBuffer info = new StringBuffer();
		info.append("Ethernet packet info:\n");
		info.append("__________________________________________________________\n");
		info.append("Ethernet destination address : " + destinationAddress + "\n");
		info.append("Ethernet source address      : " + sourceAddress + "\n");
		info.append("Ethernet frame type          : " + "0x" + ((int) (frameType & 0xFFFF)) + "\n");
		if (frameType <= ETHERTYPE_802_3) {
			info.append("Ethernet frame type info     : IEEE 802.3 packet\n");
		} else {
			switch (frameType) {
			case ETHERTYPE_ARP:
				info.append("Ethernet frame type info     : ARP frame\n");
				break;
			case ETHERTYPE_IP:
				info.append("Ethernet frame type info     : Internet IPv4 frame\n");
				break;
			case ETHERTYPE_IPV6:
				info.append("Ethernet frame type info     : Internet IPv6 frame\n");
				break;
			case ETHERTYPE_LOOPBACK:
				info.append("Ethernet frame type info     : Loopback frame\n");
				break;
			case ETHERTYPE_VLAN:
				info.append("Ethernet frame type info     : VLAN frame\n");
				break;
			case ETHERTYPE_PUP:
				info.append("Ethernet frame type info     : PUP frame\n");
				break;
			case ETHERTYPE_REVARP:
				info.append("Ethernet frame type info     : Reverse ARP frame\n");
				break;
			default:
				info.append("Ethernet frame type info     : Unknown frame type " + frameType + " \n");
			}
			info.append("__________________________________________________________\n");
		}
		return info.toString();

	}

}
