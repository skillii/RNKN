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
 * @version $Rev: 930 $ $Date: 2007/02/12 17:56:28 $
 */
public class ICMPPacket implements Packet {
	/*
	 * Possible values for the type field and their meaning.
	 */
	public static final byte DEFAULT_CODE = 0;

	public static final short DEFAULT_IDENTIFIER = (short) 0x5D3B;

	public static final byte ECHO_REPLY = 0;

	public static final byte DESTINATION_UNREACHABLE = 3;

	public static final byte SOURCE_QUENCH = 4;

	public static final byte REDIRECT_MESSAGE = 5;

	public static final byte ALTERNATE_HOST_ADDRESS = 6;

	public static final byte ECHO_REQUEST = 8;

	public static final byte ROUTER_ADVERSTISEMENT = 9;

	public static final byte ROUTER_SOLICITATION = 10;

	public static final byte TIME_EXCEEDED = 11;

	public static final byte PARAMETER_PROBLEM = 12;

	public static final byte TIMESTAMP = 13;

	public static final byte TIMESTAMP_REPLY = 14;

	public static final byte INFORMATION_REQUEST = 15;

	public static final byte INFORMATION_REPLY = 16;

	public static final byte ADDRESS_MASK_REQUEST = 17;

	public static final byte ADDRESS_MASK_REPLY = 18;

	public static final byte TRACE_ROUTE = 30;

	public static final byte DATAGRAM_CONVERSION_ERROR = 31;

	public static final byte MOBILE_HOST_REDIRECT = 32;

	public static final byte IPv6_WHERE_ARE_YOU = 33;

	public static final byte IPv6_HERE_I_AM = 34;

	public static final byte MOBILE_REGISTRATION_REQUEST = 35;

	public static final byte MOBILE_REGISTRATION_REPLY = 36;

	public static final byte DOMAIN_NAME_REPLY = 38;

	public static final byte SKIP_ALGORITHM_DISCOVERY_PROTOCOL = 39;

	public static final byte PHOTURIS = 40; // Also for security failures

	public static final byte SEAMOBY = 41; // ICMP for experimental mobility

	// protocols

	public static final byte ICMP_HEADER_LENGTH = 8;

	public static final byte[] DEFAULT_PAYLOAD = "abcdefghijklmnopqrstuvwabcdefghi".getBytes();

	private byte type;

	private byte code;

	private short checksum = 0;

	private short identifier;

	private short sequenceNumber;

	private long timeout = 0;

	private Log log;

	private ICMPPacket(byte[] packet) throws PacketParsingException {
		log = LogFactory.getLog(this.getClass());
	}

	private ICMPPacket(byte type, byte code, short identifier, short sequenceNumber, byte[] payload) {
	}

	public static ICMPPacket createICMPPacket(IPPacket packet) throws PacketParsingException {
		return null;
	}

	public static ICMPPacket createICMPPacket(byte type, byte code, short identifier, short sequenceNumber, byte[] payload) {
		return null;
	}

	public static ICMPPacket createICMPPacket(byte type, short sequenceNumber) {
		return null;
	}

	public short getChecksum() {
		return checksum;
	}

	public byte getCode() {
		return code;
	}

	public short getIdentifier() {
		return identifier;
	}

	public short getSequenceNumber() {
		return sequenceNumber;
	}

	public byte getType() {
		return type;
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

	public String getInfo() {
		StringBuffer info = new StringBuffer();
		info.append("ICMP packet info:\n");
		info.append("__________________________________________________________\n");
		info.append("Type                  : " + NetUtils.toInt(type) + "\n");
		info.append("Code                  : " + NetUtils.toInt(code) + "\n");
		info.append("Identifier            : " + NetUtils.toInt(identifier) + "\n");
		info.append("Checksum              : " + NetUtils.toInt(checksum) + "\n");
		info.append("Sequence number       : " + NetUtils.toInt(sequenceNumber) + "\n");
		info.append("__________________________________________________________\n");

		return info.toString();
	}

}
