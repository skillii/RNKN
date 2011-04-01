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
package org.iaik.net;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.exceptions.NetworkException;
import org.iaik.net.exceptions.PacketParsingException;
import org.iaik.net.packets.IPPacket;
import org.iaik.net.utils.Bitmap;
import org.iaik.net.utils.NetUtils;
import org.iaik.net.utils.NumFunctions;

public class Payload {

	/** MTU Size */
	public final static short PACKET_MTU_SIZE = 1500;

	/** Maximum Data length in bytes (max IP Packet size excl. Header) */
	public final static int PAYLOAD_MAX_DATA_SIZE = PACKET_MTU_SIZE - 20;

	/** Maximum Data length in words */
	public final static int MAX_PAYLOAD_SIZE = 65536;

	/** Maximum IP Header length */
	public final static int MAXIPH = 60;

	public final static int STATUS_MORE_FRAGMENTS = 1;

	public final static int STATUS_COMPLETE = 0;

	public final static int STATUS_INIT = 0;

	private int status = STATUS_INIT;

	private Log log;

	private byte[] payload;

	private int length = 0;

	private long timeout = 0;

	private short offset = 0;

	private byte[] ipHeader;

	private Bitmap reassembleBitmap;

	private Payload(byte[] ipHeader) {
		log = LogFactory.getLog(this.getClass());
		reassembleBitmap = new Bitmap(NumFunctions.divRoundUp(PAYLOAD_MAX_DATA_SIZE, 8));
		payload = new byte[MAX_PAYLOAD_SIZE];
		status = STATUS_INIT;
		this.ipHeader = ipHeader;
		// TODO: Currently only set but not responded to an timeout. Maybe we
		// can
		// send back an destination unreachable or a timeout ICMP or whatever.
		// Must check the RFC.
		timeout = System.currentTimeMillis();
	}

	public static Payload createPayload(IPPacket packet) {
		return new Payload(packet.getHeader());
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * Fills the payload buffer starting at offset with a given area of a byte
	 * array
	 * 
	 * @param offset
	 *            The offset within the payload.
	 * @param buffer
	 *            An byte array containing the data.
	 * @param count
	 *            The amount of bytes which should be copied.
	 * 
	 * @throws NetworkException
	 *             Thrown if provided data is too large to fit into the payload
	 *             object.
	 */
	public void fillData(short offset, byte[] buffer, short count) throws NetworkException {
		this.offset &= 0xFFFF;
		count &= 0xFFFF;

		if (length < offset + count)
			this.length = offset + count;

		if (length > PAYLOAD_MAX_DATA_SIZE)
			throw new NetworkException("Packet size bigger than the specified size of: " + PAYLOAD_MAX_DATA_SIZE);

		NetUtils.insertData(payload, buffer, offset, count);

		reassembleBitmap.setBits(offset, count / 8);

		if (reassembleBitmap.allSet()) {
			status = STATUS_COMPLETE;
			log.info("Payload has been reassembled successfully!");
		}
	}

	public short getIdentification() {
		return NetUtils.bytesToShort(ipHeader, 4);
	}

	public String getSourceAddress() {
		return NetUtils.toIPv4Address(ipHeader, 12);

	}

	public IPPacket getIPPacket() throws PacketParsingException {
		return IPPacket.createIPPacket(ipHeader, payload);
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public short getOffset() {
		return offset;
	}

	public void setOffset(short offset) {
		this.offset = offset;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
}
