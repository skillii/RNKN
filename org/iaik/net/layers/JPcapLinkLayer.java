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

import java.util.Properties;

import jpcap.JpcapCaptor;
import jpcap.JpcapSender;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.datatypes.interfaces.ARPTable;
import org.iaik.net.exceptions.NetworkException;
import org.iaik.net.factories.InternetLayerFactory;
import org.iaik.net.interfaces.InternetLayer;
import org.iaik.net.interfaces.LinkLayer;
import org.iaik.net.interfaces.PhysicalSender;
import org.iaik.net.utils.NetworkBuffer;

/**
 * 
 * @author <a href="mailto:stefan.kraxberger@iaik.tugraz.at">Stefan Kraxberger</a>
 * @date Jan 11, 2007
 * @version $Rev: 930 $ $Date: 2007/02/15 15:49:05 $
 */
public class JPcapLinkLayer implements LinkLayer {

	private jpcap.NetworkInterface realInterface;

	private InternetLayer internetLayer;

	private JPcapReceiver receiver;

	private JpcapSender sender;

	private PhysicalSender physicalSender;

	private String bpfFilter;

	private Properties properties;

	private boolean running = false;

	/**
	 * The MAC address which should be used for this network interface
	 */
	private String mac_address; // own ethernet address

	/**
	 * The IP address which should be simulated on this network interface
	 */
	private String ip_address;

	private Log log;

	public JPcapLinkLayer() throws NetworkException {
		log = LogFactory.getLog(this.getClass());
	}

	public void init() throws NetworkException {
		if (realInterface == null) {
			throw new NetworkException("No network interface to capture from.");
		} else {
			internetLayer = InternetLayerFactory.createInstance(properties);
		}
	}

	public void setProperties(Properties properties) {
		int iface = Integer.parseInt(properties.getProperty("interface"));

		this.properties = properties;

		jpcap.NetworkInterface[] devices = JpcapCaptor.getDeviceList();

		if (iface < devices.length) {
			realInterface = devices[iface];
			String show = properties.getProperty("show-interfaces");
			if (show instanceof String && show.equalsIgnoreCase("true")) {
				for (int i = 0; i < devices.length; i++)
					log.info(i + ":" + devices[i].name + "("
							+ devices[i].description + ")");
			}
		} else {
			if (devices.length == 0) {
				System.out
						.println("No usable devices found. Maybe you don't have sufficient rights.");
			} else {
				for (int i = 0; i < devices.length; i++)
					log.info(i + ":" + devices[i].name + "("
							+ devices[i].description + ")");
			}
		}

	}

	public Properties getProperties() {
		return properties;
	}

	public void setMACAddress(String address) {
		this.mac_address = address;
	}

	public void setIPAddress(String address) {
		this.ip_address = address;
	}

	public String getMACAddress() {
		return mac_address;
	}

	public String getIPAddress() {
		return ip_address;
	}

	public void setFilter(String filter) {
		this.bpfFilter = filter;
	}

	/**
	 * Starts the thread for recieving packets over this network interface. This
	 * network interface is initialized and the thread which loops for packets
	 * is started. Also the packet sending interface is initialized.
	 */
	public void start() {
		try {
			if (realInterface != null) {
				JpcapCaptor captor = JpcapCaptor.openDevice(realInterface,
						2000, true, 20);

				/*
				 * Filters out all the traffic which is not intended for our
				 * simulated network interfac except ARP and DHCP traffic.
				 */
				if (bpfFilter instanceof String)
					captor.setFilter(bpfFilter, false);

				receiver = new JPcapReceiver(this, captor);
				sender = JpcapSender.openDevice(realInterface);

				// sender = new JPcapSender(this, jpcapsender);

				receiver.setName("JPCAP receiver");
				receiver.start();
				log.debug("Capturing thread started!");
				/*
				 * sender.setName("JPCAP sender"); sender.start();
				 * log.debug("Sending thread started!");
				 */
				((Thread) internetLayer).setName("InternetLayer");
				((Thread) internetLayer).start();
				log.debug("Network layer thread started!");
				running = true;
			}

		} catch (Exception e) {
			log.debug("Exception network interface execution! Reason: "
					+ e.getMessage());
		}
	}

	public void checkTimeouts() {
		internetLayer.checkTimeouts();
	}

	public void terminate() {
		running = false;

		if (sender instanceof JpcapSender) {
			sender.close();
			sender = null;
		}

		if (receiver instanceof JPcapReceiver) {
			receiver.terminate();
			receiver = null;
		}

		if (internetLayer instanceof DefaultInternetLayer) {
			internetLayer.terminate();
			internetLayer = null;
		}
	}

	public boolean isRunning() {
		return running;
	}

	public String getInfo() {
		StringBuffer info = new StringBuffer();
		info.append("Link layer info:\n");
		info
				.append("__________________________________________________________\n");
		info.append("LinkLayer Type    : JPCAP link layer\n");
		info.append("IP Address        : " + ip_address + "\n");
		info.append("MAC Address       : " + mac_address + "\n");
		info.append("Network Interface : " + realInterface.description + "\n");
		info.append("BPF Filter Mask   : " + bpfFilter + "\n");
		info
				.append("__________________________________________________________\n");

		return info.toString();
	}

	public NetworkBuffer getReceiveBuffer() {
		return internetLayer.getReceiveBuffer();
	}

	public NetworkBuffer getSendBuffer() {
		return internetLayer.getSendBuffer();
	}

	public PhysicalSender getSender() {
		if (physicalSender instanceof PhysicalSender)
			return physicalSender;
		else {
			physicalSender = new JPcapPhysicalSender(this, sender);
			return physicalSender;
		}

	}

	public ARPTable getARPTable() {
		return internetLayer.getARPTable();
	}

}
