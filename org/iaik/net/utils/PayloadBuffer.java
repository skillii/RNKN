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
package org.iaik.net.utils;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.Payload;
import org.iaik.net.StackParameters;
import org.iaik.net.packets.IPPacket;

public class PayloadBuffer {

	private int bufferSize = StackParameters.MAX_BUFFER_SIZE;

	private Log log;

	private ArrayList<Payload> buffer;

	private Object tableMonitor = new Object();

	public PayloadBuffer() {

		buffer = new ArrayList<Payload>(bufferSize);

		log = LogFactory.getLog(this.getClass());

		buffer.ensureCapacity(bufferSize);

		log.debug("ARPTable initialized successfully!");
	}

	public PayloadBuffer(int bufferSize) {
		buffer = new ArrayList<Payload>(bufferSize);

		log = LogFactory.getLog(this.getClass());

		this.bufferSize = bufferSize;

		buffer.ensureCapacity(bufferSize);

		log.debug("ARPTable initialized successfully!");
	}

	public void add(Payload entry) {

		synchronized (tableMonitor) {
			int index = indexOf(entry);

			if (index < 0) {
				buffer.add(entry);
			} else {
				buffer.remove(index);
				buffer.add(index, entry);
			}
			tableMonitor.notify();
		}
	}

	public void remove(Payload p) {
		synchronized (tableMonitor) {
			int index = indexOf(p);

			if (index >= 0)
				buffer.remove(index);
		}
	}

	public boolean contains(Payload p) {
		synchronized (tableMonitor) {

			for (int index = 0; index < buffer.size(); index++) {
				Payload temp = (Payload) buffer.get(index);
				if (temp.getIdentification() == p.getIdentification())
					return true;
			}
			return false;
		}
	}

	public int indexOf(Payload packet) {
		synchronized (tableMonitor) {

			for (int index = 0; index < buffer.size(); index++) {
				Payload temp = (Payload) buffer.get(index);
				if (temp.getIdentification() == packet.getIdentification()
						&& temp.getSourceAddress().equalsIgnoreCase(packet.getSourceAddress()))
					return index;
			}
			return -1;
		}
	}

	public int indexOf(IPPacket packet) {
		synchronized (tableMonitor) {

			for (int index = 0; index < buffer.size(); index++) {
				Payload temp = (Payload) buffer.get(index);
				if (temp.getIdentification() == packet.getIdentification()
						&& temp.getSourceAddress().equalsIgnoreCase(packet.getSourceAddress()))
					return index;
			}
			return -1;
		}
	}

	public Payload findPayload(IPPacket packet) {
		if (packet instanceof IPPacket)
			return (Payload) buffer.get(indexOf(packet));
		else
			return null;
	}

	public long getTableSize() {
		return bufferSize;
	}

}
