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

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.StackParameters;
import org.iaik.net.packets.Packet;

/**
 * 
 * @author <a href="mailto:stefan.kraxberger@iaik.tugraz.at">Stefan
 *         Kraxberger</a>
 * @date Jan 11, 2007
 * @version $Rev: 930 $ $Date: 2007/02/20 15:09:50 $
 */
public class NetworkBuffer {

	private int bufferSize = StackParameters.MAX_BUFFER_SIZE;

	private Log log;

	private LinkedList<Packet> buffer;

	private Object bufferMonitor = new Object();

	public NetworkBuffer() {

		buffer = new LinkedList<Packet>();

		log = LogFactory.getLog(this.getClass());
	}

	public NetworkBuffer(int bufferSize) {
		this();
		this.bufferSize = bufferSize;
		// buffer.ensureCapacity(bufferSize);
	}

	public void add(Packet p) {

		synchronized (bufferMonitor) {
			buffer.add(p);
			// bufferMonitor.notify();
		}
	}

	public Packet get(int index) {
		if (index < buffer.size())
			return buffer.get(index);
		else
			return null;
	}

	public void remove(Packet p) {
		synchronized (bufferMonitor) {
			if (!buffer.isEmpty())
				buffer.remove(p);
			log.info("Packet " + p.getInfo() + " removed from the buffer!");
		}
	}

	public int getSize() {
		return buffer.size();
	}

	public Packet getNextPacket() {
		synchronized (bufferMonitor) {
			// try {
			/*
			 * if (buffer.isEmpty()) bufferMonitor.wait(); return (Packet)
			 * buffer.remove();
			 */

			if (buffer.isEmpty())
				return null;
			else
				return (Packet) buffer.remove();

			/*
			 * } catch (InterruptedException ie) {
			 * log.error("Waiting for next packet interrupted!\nReason: " +
			 * ie.getMessage()); return null; }
			 */

		}
	}

	public long getBufferSize() {
		return bufferSize;
	}

}
