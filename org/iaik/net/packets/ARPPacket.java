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
public class ARPPacket implements Packet {

	public static final short ARP_REQUEST = 1;

	public static final short ARP_REPLY = 2;

	public static final short HTYPE_ETHERNET = 0x0001;

	public static final short PTYPE_IP = 0x0800;

	public static final byte HLEN = 6;

	public static final byte PLEN = 4;

	private short htype;

	private short ptype;

	private byte hlen;

	private byte plen;

	private short oper;

	private String sha;

	private String spa;

	private String tha;

	private String tpa;

	/*
	 * Don't know if this should be used because this would prevent garbage
	 * collection but it may also be interesting to get information about the
	 * lower layer packet in which this one was contained.
	 */
	private EthernetPacket containedIn;

	private long timeout = 0;

	private Log log;

	public ARPPacket(EthernetPacket packet) throws PacketParsingException {
		log = LogFactory.getLog(this.getClass());
	}

	public static ARPPacket createARPPacket(short oper, String sha, String spa, String tha, String tpa) {
		return null;
	}

	public static ARPPacket createARPPacket(EthernetPacket packet) throws PacketParsingException {
		return new ARPPacket(packet);
	}

	public byte getHardwareLength() {
		return hlen;
	}

	public void setHardwareLength(byte hlen) {
		this.hlen = hlen;
	}

	public short getHardwareType() {
		return htype;
	}

	public void setHardwareType(short htype) {
		this.htype = htype;
	}

	public short getOperationCode() {
		return oper;
	}

	public void setOperationCode(short oper) {
		this.oper = oper;
	}

	public byte getProtocolLength() {
		return plen;
	}

	public void setProtocolLength(byte plen) {
		this.plen = plen;
	}

	public short getProtocolType() {
		return ptype;
	}

	public void setProtocolType(short ptype) {
		this.ptype = ptype;
	}

	public String getSenderHardwareAddress() {
		return sha;
	}

	public void setSenderHardwareAddress(String sha) {
		this.sha = sha;
	}

	public String getSenderProtocolAddress() {
		return spa;
	}

	public void setSenderProtocolAddress(String spa) {
		this.spa = spa;
	}

	public String getTargetHardwareAddress() {
		return tha;
	}

	public void setTargetHardwareAddress(String tha) {
		this.tha = tha;
	}

	public String getTargetProtocolAddress() {
		return tpa;
	}

	public void setTargetProtocolAddress(String tpa) {
		this.tpa = tpa;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public byte[] getPacket() {
		return null;
	}

	public EthernetPacket getContainedIn() {
		return containedIn;
	}

	public String getInfo() {
		StringBuffer info = new StringBuffer();
		info.append("ARP packet info:\n");
		info.append("__________________________________________________________\n");
		info.append("Hardware type          : " + NetUtils.toHexString(htype) + "\n");
		info.append("Protocol type          : " + NetUtils.toHexString(ptype) + "\n");
		info.append("Hardware length        : " + NetUtils.toHexString(hlen) + "\n");
		info.append("Protocol length        : " + NetUtils.toHexString(plen) + "\n");
		info.append("Operation code         : " + NetUtils.toHexString(oper) + "\n");
		info.append("Sender hardware address: " + sha + "\n");
		info.append("Sender protocol address: " + spa + "\n");
		info.append("Target hardware address: " + tha + "\n");
		info.append("Target protocol address: " + tpa + "\n");
		info.append("__________________________________________________________\n");
		return info.toString();
	}

}
