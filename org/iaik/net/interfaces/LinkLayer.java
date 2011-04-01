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
package org.iaik.net.interfaces;

import java.util.Properties;

import org.iaik.net.datatypes.interfaces.ARPTable;
import org.iaik.net.exceptions.NetworkException;
import org.iaik.net.utils.NetworkBuffer;

/**
 * This class provides the abstraction for a network interface which receives
 * packets from the network. This class is the interface for all possible
 * linklayer implementations.
 * 
 * @author <a href="mailto:stefan.kraxberger@iaik.tugraz.at">Stefan
 *         Kraxberger</a>
 * @date Dec 6, 2006
 * @version $Rev: 930 $ $Date: 2007/02/14 16:09:19 $
 */
public interface LinkLayer {
	public void init() throws NetworkException;

	public void start();

	public void terminate();

	public void checkTimeouts();

	public PhysicalSender getSender();

	public void setMACAddress(String address);

	public void setIPAddress(String address);

	public String getMACAddress();

	public String getIPAddress();

	public void setProperties(Properties properties);

	public Properties getProperties();

	public void setFilter(String filter);

	public NetworkBuffer getReceiveBuffer();

	public NetworkBuffer getSendBuffer();

	public ARPTable getARPTable();

	public String getInfo();
}
