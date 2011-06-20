/* Copyright  (c) 2006_2007 Graz University of Technology. All rights reserved.
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

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.utils.NetUtils;

/**
 * This class represents an IP packet containing all the relevant information
 * and also providing methods for manipulating and creating IP packets.
 * 
 * @author <a href="mailto:stefan.kraxberger@iaik.tugraz.at">Stefan
 *         Kraxberger</a>
 * @date Jan 11, 2007
 * @version $Rev: 930 $ $Date: 2007/02/12 17:56:28 $
 */
public class IPPacket implements Packet {

	public static final byte RESERVED_BIT = 0x04;

	public static final byte DONT_FRAGMENT_BIT = 0x02;

	public static final byte MORE_FRAGMENTS_BIT = 0x01;

	public static final byte DEFAULT_BIT = 0x0;

	public static final byte HOPOPT_PROTOCOL = 0; // IPv6 Hop_by_Hop Option

	// RFC 1883

	public static final byte ICMP_PROTOCOL = 1; // Internet Control Message

	// Protocol RFC 792

	public static final byte IGMP_PROTOCOL = 2; // Internet Group Management

	// Protocol RFC 1112

	public static final byte GGP_PROTOCOL = 3; // Gateway_to_Gateway Protocol

	// RFC 823

	public static final byte IP_PROTOCOL = 4; // IP in IP (encapsulation) RFC

	// 2003

	public static final byte ST_PROTOCOL = 5; // Stream RFC 1190, RFC 1819

	public static final byte TCP_PROTOCOL = 6; // Transmission Control Protocol

	// RFC 793

	public static final byte CBT_PROTOCOL = 7; // CBT

	public static final byte EGP_PROTOCOL = 8; // Exterior Gateway Protocol RFC

	// 888

	public static final byte IGP_PROTOCOL = 9; // Interior Gateway Protocol

	// (any private interior gateway
	// (used by Cisco for their
	// IGRP))

	public static final byte BBN_RCC_MON_PROTOCOL = 10; // BBN RCC Monitoring

	public static final byte NVP_II_PROTOCOL = 11; // Network Voice Protocol

	// RFC 741

	public static final byte PUP_PROTOCOL = 12; // Xerox PUP

	public static final byte ARGUS_PROTOCOL = 13; // ARGUS

	public static final byte EMCON_PROTOCOL = 14; // EMCON

	public static final byte XNET_PROTOCOL = 15; // Cross Net Debugger IEN

	// 158

	public static final byte CHAOS_PROTOCOL = 16; // Chaos

	public static final byte UDP_PROTOCOL = 17; // User Datagram Protocol RFC

	// 768

	public static final byte MUX_PROTOCOL = 18; // Multiplexing IEN 90

	public static final byte DCN_MEAS_PROTOCOL = 19; // DCN Measurement

	// Subsystems

	public static final byte HMP_PROTOCOL = 20; // Host Monitoring Protocol RFC

	// 869

	public static final byte PRM_PROTOCOL = 21; // Packet Radio Measurement

	public static final byte XNS_IDP_PROTOCOL = 22; // XEROX NS IDP

	public static final byte TRUNK_1_PROTOCOL = 23; // Trunk_1

	public static final byte TRUNK_2_PROTOCOL = 24; // Trunk_2

	public static final byte LEAF_1_PROTOCOL = 25; // Leaf_1

	public static final byte LEAF_2_PROTOCOL = 26; // Leaf_2

	public static final byte RDP_PROTOCOL = 27; // Reliable Data Protocol RFC

	// 908

	public static final byte IRTP_PROTOCOL = 28; // Internet Reliable

	// Transaction Protocol RFC
	// 938

	public static final byte ISO_TP4_PROTOCOL = 29; // ISO Transport Protocol

	// Class 4 RFC 905

	public static final byte NETBLT_PROTOCOL = 30; // Bulk Data Transfer

	// Protocol RFC 969

	public static final byte MFE_NSP_PROTOCOL = 31; // MFE Network Services

	// Protocol

	public static final byte MERIT_INP_PROTOCOL = 32; // MERIT Internodal

	// Protocol RFC 4340

	public static final byte DCCP_PROTOCOL = 33; // Datagram Congestion Control
	// Protocol

	public static final byte THIRD_PC_PROTOCOL = 34; // Third Party Connect

	// Protocol

	public static final byte IDPR_PROTOCOL = 35; // Inter_Domain Policy

	// Routing Protocol RFC 1479

	public static final byte XTP_PROTOCOL = 36; // XTP

	public static final byte DDP_PROTOCOL = 37; // Datagram Delivery Protocol

	public static final byte IDPR_CMTP_PROTOCOL = 38; // IDPR Control Message

	// Transport Protocol

	public static final byte TP_PP_PROTOCOL = 39; // TP++ Transport Protocol

	public static final byte IL_PROTOCOL = 40; // IL Transport Protocol

	public static final byte IPv6_PROTOCOL = 41; // IPv6

	public static final byte SDRP_PROTOCOL = 42; // Source Demand Routing

	// Protocol

	public static final byte IPv6_Route_PROTOCOL = 43; // Routing Header for

	// IPv6

	public static final byte IPv6_Frag_PROTOCOL = 44; // Fragment Header for

	// IPv6

	public static final byte IDRP_PROTOCOL = 45; // Inter_Domain Routing

	// Protocol

	public static final byte RSVP_PROTOCOL = 46; // Resource Reservation

	// Protocol

	public static final byte GRE_PROTOCOL = 47; // Generic Routing Encapsulation

	public static final byte MHRP_PROTOCOL = 48; // Mobile Host Routing

	// Protocol

	public static final byte BNA_PROTOCOL = 49; // BNA

	public static final byte ESP_PROTOCOL = 50; // Encap Security Payload RFC

	// 2406

	public static final byte AH_PROTOCOL = 51; // Authentication Header RFC

	// 2402

	public static final byte I_NLSP_PROTOCOL = 52; // Integrated Net Layer

	// Security Protocol TUBA

	public static final byte SWIPE_PROTOCOL = 53; // IP with Encryption

	public static final byte NARP_PROTOCOL = 54; // NBMA Address Resolution

	// Protocol RFC 1735

	public static final byte MOBILE_PROTOCOL = 55; // IP Mobility

	public static final byte TLSP_PROTOCOL = 56; // Transport Layer Security

	// Protocol (using Kryptonet
	// key management)

	public static final byte SKIP_PROTOCOL = 57; // SKIP

	public static final byte IPv6_ICMP_PROTOCOL = 58; // ICMP for IPv6 RFC

	// 1883

	public static final byte IPv6_NoNxt_PROTOCOL = 59; // No Next Header for

	// IPv6 RFC 1883

	public static final byte IPv6_Opts_PROTOCOL = 60; // Destination Options

	// for IPv6 RFC 1883

	public static final byte HIP_PROTOCOL = 61; // Any host internal protocol

	public static final byte CFTP_PROTOCOL = 62; // CFTP

	public static final byte LN_PROTOCOL = 63; // Any local network

	public static final byte SAT_EXPAK_PROTOCOL = 64; // SATNET and Backroom

	// EXPAK

	public static final byte KRYPTOLAN_PROTOCOL = 65; // Kryptolan

	public static final byte RVD_PROTOCOL = 66; // MIT Remote Virtual Disk

	// Protocol

	public static final byte IPPC_PROTOCOL = 67; // Internet Pluribus Packet

	// Core

	public static final byte DFS_PROTOCOL = 68; // Any distributed file system

	public static final byte SAT_MON_PROTOCOL = 69; // SATNET Monitoring

	public static final byte VISA_PROTOCOL = 70; // VISA Protocol

	public static final byte IPCV_PROTOCOL = 71; // Internet Packet Core

	// Utility

	public static final byte CPNX_PROTOCOL = 72; // Computer Protocol Network

	// Executive

	public static final byte CPHB_PROTOCOL = 73; // Computer Protocol Heart

	// Beat

	public static final byte WSN_PROTOCOL = 74; // Wang Span Network

	public static final byte PVP_PROTOCOL = 75; // Packet Video Protocol

	public static final byte BR_SAT_MON_PROTOCOL = 76; // Backroom SATNET

	// Monitoring

	public static final byte SUN_ND_PROTOCOL = 77; // SUN ND PROTOCOL_Temporary

	public static final byte WB_MON_PROTOCOL = 78; // WIDEBAND Monitoring

	public static final byte WB_EXPAK_PROTOCOL = 79; // WIDEBAND EXPAK

	public static final byte ISO_IP_PROTOCOL = 80; // International

	// Organisation for
	// Standardization Internet
	// Protocol

	public static final byte VMTP_PROTOCOL = 81; // VMTP

	public static final byte SECURE_VMTP_PROTOCOL = 82; // SECURE_VMTP

	public static final byte VINES_PROTOCOL = 83; // VINES

	public static final byte TTP_PROTOCOL = 84; // TTP

	public static final byte NSFNET_IGP_PROTOCOL = 85; // NSFNET_IGP

	public static final byte DGP_PROTOCOL = 86; // Dissimilar Gateway Protocol

	public static final byte TCF_PROTOCOL = 87; // TCF

	public static final byte EIGRP_PROTOCOL = 88; // EIGRP

	public static final byte OSPF_PROTOCOL = 89; // Open Shortest Path First

	// RFC 1583

	public static final byte Sprite_RPC_PROTOCOL = 90; // Sprite RPC Protocol

	public static final byte LARP_PROTOCOL = 91; // Locus Address Resolution

	// Protocol

	public static final byte MTP_PROTOCOL = 92; // Multicast Transport Protocol

	public static final byte AX_25_PROTOCOL = 93; // AX.25

	public static final byte IPIP_PROTOCOL = 94; // IP_within_IP

	// Encapsulation Protocol

	public static final byte MICP_PROTOCOL = 95; // Mobile Internetworking

	// Control Protocol

	public static final byte SCC_SP_PROTOCOL = 96; // Semaphore Communications

	// Sec. Pro

	public static final byte ETHERIP_PROTOCOL = 97; // Ethernet_within_IP

	// Encapsulation RFC 3378

	public static final byte ENCAP_PROTOCOL = 98; // Encapsulation Header RFC

	// 1241

	public static final byte PES_PROTOCOL = 99; // Any private encryption scheme

	public static final byte GMTP_PROTOCOL = 100; // GMTP

	public static final byte IFMP_PROTOCOL = 101; // Ipsilon Flow Management

	// Protocol

	public static final byte PNNI_PROTOCOL = 102; // PNNI over IP

	public static final byte PIM_PROTOCOL = 103; // Protocol Independent

	// Multicast

	public static final byte ARIS_PROTOCOL = 104; // ARIS

	public static final byte SCPS_PROTOCOL = 105; // SCPS

	public static final byte QNX_PROTOCOL = 106; // QNX

	public static final byte AN_PROTOCOL = 107; // Active Networks

	public static final byte IPComp_PROTOCOL = 108; // IP Payload Compression

	// Protocol RFC 2393

	public static final byte SNP_PROTOCOL = 109; // Sitara Networks Protocol

	public static final byte Compaq_Peer_PROTOCOL = 110;// Compaq Peer Protocol

	public static final byte IPX_IN_IP_PROTOCOL = 111; // IPX in IP

	public static final byte VRRP_PROTOCOL = 112; // Virtual Router Redundancy

	// Protocol, Common Address
	// Redundancy Protocol(not
	// IANA assigned) VRRP:RFC
	// 3768

	public static final byte PGM_PROTOCOL = 113; // PGM Reliable Transport

	// Protocol

	public static final byte Any_PROTOCOL = 114; // 0_hop protocol

	public static final byte L2TP_PROTOCOL = 115; // Layer Two Tunneling

	// Protocol

	public static final byte DDX_PROTOCOL = 116; // D_II Data Exchange (DDX)

	public static final byte IATP_PROTOCOL = 117; // Interactive Agent

	// Transfer Protocol

	public static final byte STP_PROTOCOL = 118; // Schedule Transfer

	// Protocol

	public static final byte SRP_PROTOCOL = 119; // SpectraLink Radio

	// Protocol

	public static final byte UTI_PROTOCOL = 120; // UTI

	public static final byte SMP_PROTOCOL = 121; // Simple Message Protocol

	public static final byte SM_PROTOCOL = 122; // SM

	public static final byte PTP_PROTOCOL = 123; // Performance Transparency

	// Protocol

	public static final byte ISIS_PROTOCOL = 124; // over IPv4

	public static final byte FIRE_PROTOCOL = 125;

	public static final byte CRTP_PROTOCOL = 126; // Combat Radio Transport

	// Protocol

	public static final byte CRUDP_PROTOCOL = 127; // Combat Radio User

	// Datagram

	public static final byte SSCOPMCE_PROTOCOL = (byte) 128;

	public static final byte IPLT_PROTOCOL = (byte) 129;

	public static final byte SPS_PROTOCOL = (byte) 130; // Secure Packet Shield

	public static final byte PIPE_PROTOCOL = (byte) 131; // Private IP

	// Encapsulation
	// within IP

	public static final byte SCTP_PROTOCOL = (byte) 132; // Stream Control

	// Transmission
	// Protocol

	public static final byte FC_PROTOCOL = (byte) 133; // Fibre Channel

	public static final byte RSVP_E2E_IGNORE_PROTOCOL = (byte) 134; // RSVP_E2E_IGNORE

	// RFC 3175

	public static final byte MH_PROTOCOL = (byte) 135; // Mobility Header RFC

	// 3775

	public static final byte UDPLITE_PROTOCOL = (byte) 136; // UDPLite RFC 3828

	public static final byte MPLS_IN_IP_PROTOCOL = (byte) 137; // MPLS_in_IP
	
	public static final byte RUDP_PROTOCOL = (byte) 167; // MPLS_in_IP
	

	public static final byte IPv4_VERSION = 4;

	public static final byte IPv6_VERSION = 6;

	public static final byte TOS_DEFAULT = 0x00;

	public static final byte TOS_ASSURED_FORWARDING_11 = (byte) (0x0A << 2);

	public static final byte TOS_ASSURED_FORWARDING_12 = (byte) (0x0C << 2);

	public static final byte TOS_ASSURED_FORWARDING_13 = (byte) (0x0E << 2);

	public static final byte TOS_ASSURED_FORWARDING_21 = (byte) (0x12 << 2);

	public static final byte TOS_ASSURED_FORWARDING_22 = (byte) (0x14 << 2);

	public static final byte TOS_ASSURED_FORWARDING_23 = (byte) (0x16 << 2);

	public static final byte TOS_ASSURED_FORWARDING_31 = (byte) (0x1A << 2);

	public static final byte TOS_ASSURED_FORWARDING_32 = (byte) (0x1C << 2);

	public static final byte TOS_ASSURED_FORWARDING_33 = (byte) (0x1E << 2);

	public static final byte TOS_ASSURED_FORWARDING_41 = (byte) (0x22 << 2);

	public static final byte TOS_ASSURED_FORWARDING_42 = (byte) (0x24 << 2);

	public static final byte TOS_ASSURED_FORWARDING_43 = (byte) (0x26 << 2);

	public static final byte TOS_EXPEDITED_FORWARDING = (byte) 0x2E;

	public static final short NO_OFFSET = 0x0;

	public static final byte DEFAULT_TTL = (byte) 128;
	
	
	final static int ihlOffset = 0;
	final static int versionOffset = 0;
	final static int tosOffset = 1;
	final static int totalLenOffset = 2;
	final static int identificationOffset = 4;
	final static int flagsOffset = 6;
	final static int ttlOffset = 8;
	final static int protocolOffset = 9;
	final static int SourceOffset = 12;
	final static int destinationOffset = 16;
	final static int offsetOffset = 6;
	final static int checksumOffset = 10;
	

	private byte version;

	private byte ihl;

	private byte tos;

	private short length;

	private short identification;

	private byte flags;

	private short offset;

	private byte ttl;

	private byte protocol;

	private short checksum = 0;

	private String sourceAddress;

	private String destinationAddress;

	private byte[] options;

	// private byte[] header;

	private byte[] payload;

	private EthernetPacket containedIn;

	private long timeout = 0;

	private Log log;
	
	private boolean chkSumValid;

	private IPPacket(EthernetPacket packet) throws PacketParsingException {
		log = LogFactory.getLog(this.getClass());
	}

	private IPPacket(byte version, byte tos, short identification, byte flags, short offset, byte ttl, byte protocol, String sourceAddress,
			String destinationAddress, byte[] payload) {
		this.version = version;
		this.tos = tos;
		this.identification = identification;
		this.flags = flags;
		this.offset = offset;
		this.ttl = ttl;
		this.protocol = protocol;
		this.sourceAddress = sourceAddress;
		this.destinationAddress = destinationAddress;
		this.payload = payload;
		chkSumValid = true;
	}

	private IPPacket(byte[] header, byte[] payload) throws PacketParsingException {

		if(header.length < destinationOffset+4)
			throw new PacketParsingException("Header is too short!");

	
		ihl = (byte) (header[ihlOffset] & 0x0F);
		version =  (byte) (header[versionOffset] >> 4);
		tos = header[tosOffset];
		identification = header[identificationOffset];
		length = NetUtils.bytesToShort(header, totalLenOffset);
		flags = (byte)(header[flagsOffset]>>5);
		ttl = header[ttlOffset];
		protocol = header[protocolOffset];
		offset = (short)((header[offsetOffset] & 0x0F) << 8 | header[offsetOffset+1]);
		checksum = (short)(header[checksumOffset]<<8 | header[checksumOffset+1]);
		
		StringBuilder source = new StringBuilder();
		StringBuilder dest = new StringBuilder();
		
		
		source.append((int)header[SourceOffset+0] & 0xFF); source.append('.');
		source.append((int)header[SourceOffset+1]& 0xFF); source.append('.');
		source.append((int)header[SourceOffset+2]& 0xFF); source.append('.');
		source.append((int)header[SourceOffset+3]& 0xFF);
		
		dest.append((int)header[destinationOffset+0] & 0xFF); dest.append('.');
		dest.append((int)header[destinationOffset+1]& 0xFF); dest.append('.');
		dest.append((int)header[destinationOffset+2]& 0xFF); dest.append('.');
		dest.append((int)header[destinationOffset+3]& 0xFF);
		
		sourceAddress = source.toString();
		destinationAddress = dest.toString();
		
		short sum = NetUtils.calcIPChecksum(header, 0, 20);
		
		if(sum == 0)
			chkSumValid = true;
		else
			chkSumValid = false;
	
		this.payload = payload;
	}

	public static IPPacket createIPPacket(byte[] header, byte[] payload) throws PacketParsingException {
		return new IPPacket(header, payload);
	}

	
	public static IPPacket createIPPacket(EthernetPacket packet) throws PacketParsingException {
		int headerLen;
		int payloadLen;
		
		headerLen = (packet.getPayload()[0] & 0x0f)*4;
		payloadLen = NetUtils.toInt((short)(packet.getPayload()[2]<<8 | packet.getPayload()[3])) - headerLen;
		
		byte[] header = new byte[headerLen];
		byte[] payload = new byte[payloadLen];
		System.arraycopy(packet.getPayload(), 0, header, 0, headerLen);
		System.arraycopy(packet.getPayload(), headerLen, payload, 0, payloadLen);
		
		return createIPPacket(header, payload);
	}

	public static IPPacket createIPPacket(byte version, byte tos, short identification, byte flags, short offset, byte ttl, byte protocol,
			String sourceAddress, String destinationAddress, byte[] payload) {
		return new IPPacket(version, tos, identification, flags, offset, ttl, protocol, sourceAddress, destinationAddress, payload);
	}

	public static IPPacket createDefaultIPPacket(byte protocol, short identification, String sourceAddress, String destinationAddress,
			byte[] payload) {
		return new IPPacket((byte)4, (byte)0, identification, (byte)0x02, (short)0, IPPacket.DEFAULT_TTL, protocol, sourceAddress, destinationAddress, payload);
	}

	public byte[] getPayload() {
		return payload;
	}

	public String getSourceAddress() {
		return sourceAddress;
	}

	public String getDestinationAddress() {
		return destinationAddress;
	}

	public byte getProtocol() {
		return protocol;
	}

	public short getChecksum() {
		return checksum;
	}

	public byte getFlags() {
		return flags;
	}

	public short getIdentification() {
		return identification;
	}

	public byte getIhl() {
		return ihl;
	}

	public short getLength() {
		return length;
	}

	public short getOffset() {
		return offset;
	}

	public byte[] getOptions() {
		return options;
	}

	public byte getTos() {
		return tos;
	}

	public byte getTtl() {
		return ttl;
	}

	public byte getVersion() {
		return version;
	}

	public EthernetPacket getContainedIn() {
		return containedIn;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public byte[] getHeader() {
		byte[] pkg = new byte[20];
		
		pkg[versionOffset] = (byte)(version << 4);
		pkg[ihlOffset] |= (byte)(20/4);
		pkg[tosOffset] = tos;
		NetUtils.insertData(pkg, NetUtils.shortToBytes((short)(pkg.length + payload.length)), totalLenOffset);		
		NetUtils.insertData(pkg, NetUtils.shortToBytes(identification), identificationOffset);
		pkg[flagsOffset] = (byte)((flags << 5));
		pkg[offsetOffset] = (byte)(pkg[offsetOffset] | offset >> 8);
		pkg[offsetOffset + 1] = (byte)(offset & 0xff);
		
		pkg[ttlOffset] = ttl;
		pkg[protocolOffset] = protocol;
		
		
		NetUtils.insertData(pkg, NetUtils.addressToBytes(sourceAddress, "."), SourceOffset);
		NetUtils.insertData(pkg, NetUtils.addressToBytes(destinationAddress, "."), destinationOffset);
		
		
		short sum = NetUtils.calcIPChecksum(pkg, 0, 20);

		NetUtils.insertData(pkg, NetUtils.shortToBytes(sum), checksumOffset);
		
		return pkg;
	}

	public byte[] getPacket() {
		byte[] header = getHeader();
		byte[] pkg = new byte[header.length + payload.length];

		
		System.arraycopy(header, 0, pkg, 0, header.length);
		System.arraycopy(payload, 0, pkg, header.length, payload.length);
		
		
		
		/*System.out.println("sending following package:");
		EthernetPacket ether = EthernetPacket.createEthernetPacket((short)0, "08:00:27:27:6F:34", "08:00:27:27:6F:34", pkg);
		try {
			System.out.println(createIPPacket(ether).getInfo());
		} catch (PacketParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		return pkg;		
	}

	public boolean isValid() {
		return chkSumValid;
	}


	public String getInfo() {
		StringBuffer info = new StringBuffer();
		info.append("IP packet info:\n");
		info.append("__________________________________________________________\n");
		info.append("IP version             : " + version + "\n");
		info.append("IHL                    : " + ihl + "\n");
		info.append("Type of service        : " + NetUtils.toInt(tos) + "\n");
		info.append("Total length           : " + NetUtils.toInt(length) + "\n");
		info.append("Identification         : " + NetUtils.toInt(identification) + "\n");
		info.append("Flags                  : " + flags + "\n");
		switch (flags) {
		case RESERVED_BIT:
			info.append("    1..    RESERVED_BIT: Set\n");
			info.append("    .0.    DONT_FRAGMENT_BIT: Not set\n");
			info.append("    ..0    MORE_FRAGMENTS_BIT: Not set\n");
			break;
		case DONT_FRAGMENT_BIT:
			info.append("    0..    RESERVED_BIT: Not set\n");
			info.append("    .1.    DONT_FRAGMENT_BIT: Set\n");
			info.append("    ..0    MORE_FRAGMENTS_BIT: Not set\n");
			break;
		case MORE_FRAGMENTS_BIT:
			info.append("    0..    RESERVED_BIT: Not set\n");
			info.append("    .0.    DONT_FRAGMENT_BIT: Not set\n");
			info.append("    ..1    MORE_FRAGMENTS_BIT: Set\n");
			break;
		default:
			info.append("    0..    RESERVED_BIT: Not set\n");
			info.append("    .0.    DONT_FRAGMENT_BIT: Not set\n");
			info.append("    ..0    MORE_FRAGMENTS_BIT: Not set\n");
			break;
		}
		info.append("Offset                 : " + NetUtils.toInt(offset) + "\n");
		info.append("Time to live (TTL)     : " + NetUtils.toInt(ttl) + "\n");
		info.append("Protocol               : " + protocol + "\n");
		info.append("Checksum               : " + NetUtils.toInt(checksum) + "\n");
		info.append("IP source address      : " + sourceAddress + "\n");
		info.append("IP destination address : " + destinationAddress + "\n");
		if (ihl > 20) {
			info.append("Options                : " + options + "\n");
		}
		info.append("__________________________________________________________\n");

		return info.toString();
	}
}
