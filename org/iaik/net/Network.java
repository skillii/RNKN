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

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.exceptions.NetworkException;
import org.iaik.net.factories.LinkLayerFactory;
import org.iaik.net.interfaces.LinkLayer;

/**
 * 
 * @author <a href="mailto:stefan.kraxberger@iaik.tugraz.at">Stefan
 *         Kraxberger</a>
 * @date Jan 11, 2007
 * @version $Rev: 930 $ $Date: 2007/02/13 16:07:41 $
 */
public class Network {

	public static String ip;

	public static String mac;

	public static String gateway;

	public static String netmask;

	private static LinkLayer linklayer;

	private static boolean isInitialized = false;

	private static Log log = LogFactory.getLog(Network.class);

	public static void start(Properties properties, String ipAddr, String macAddr,String gate, String mask, String captureFilter) throws NetworkException {

		ip = ipAddr;
		mac = macAddr;
		netmask = mask;
		gateway = gate;

		linklayer = LinkLayerFactory.createInstance(properties);

		if (linklayer instanceof LinkLayer) {
			linklayer.setMACAddress(macAddr);
			linklayer.setIPAddress(ipAddr);

			if (captureFilter instanceof String)
				linklayer.setFilter(captureFilter);

			linklayer.start();

			isInitialized = true;

			log.debug("Java Network Stack version 0.1.0 has been created and initialized successfully!");
		} else {
			isInitialized = false;
			log.debug("Network has not been created and initialized successfully!");
			throw new RuntimeException("Virtual interface couldn't be instantiated!");
		}
	}

	public static boolean isInitialized() {
		return isInitialized;
	}

	public static void stop() {
		if (isInitialized) {
			linklayer.terminate();
			linklayer = null;
		}
	}

	public LinkLayer getVirtualInterface() {
		return linklayer;
	}
}
