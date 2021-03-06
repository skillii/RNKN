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
package org.iaik.net.examples;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.StringTokenizer;

import org.iaik.net.Network;
import org.iaik.net.exceptions.NetworkException;
import org.iaik.net.factories.InternetLayerFactory;
import org.iaik.net.layers.DefaultInternetLayer;
import org.iaik.net.layers.JPcapPhysicalSender;
import org.iaik.net.packets.ICMPPacket;
import org.iaik.net.packets.IPPacket;
import org.iaik.net.utils.NetUtils;
import org.iaik.net.utils.PingSender;

/**
 * Tests the network capturing capabilities using the basic templates of his
 * package. Therefore the MAC and IP addresses are read from an configuration
 * file. These parameters are then used to initialize and configure the virtual
 * network interface.
 * 
 * @author <a href="mailto:stefan.kraxberger@iaik.tugraz.at">Stefan
 *         Kraxberger</a>
 * @date Dec 6, 2006
 * @version $Rev: 930 $ $Date: 2007/02/12 17:56:29 $
 */
public class TestNetwork {
	private static String name = "TestNetwork";

	/**
	 * Prints the usage.
	 */
	private static void usage() {
		System.out.println("usage: TestNetwork -config <file> \n\n" + "	-config file  File to read the configuration from.\n");
	}

	/**
	 * 
	 * @param argv
	 */
	public static void main(String argv[]) {

		File config = null;

		/**
		 * Parses the provided command line parameters.
		 */
		if (argv.length > 0) {

			if (argv[0].equalsIgnoreCase("-config")) {
				config = new File(argv[1]);
				if (config.exists() == false) {
					System.err.println(name + ": Configuration file " + config.getAbsolutePath() + " does not exist");
					return;
				}
			} else {
				usage();
				return;
			}
		} else {
			usage();
			return;
		}

		if (!(config instanceof File)) {
			System.err.println(name + ": Configuration file couldn't be opened!");
			return;
		}

		try {
			/**
			 * Parses the provided configuration file and creates a
			 * <code>org.w3c.dom.Document</code> from the parsed file. This
			 * document is then provided as parameter to the constructor of this
			 * class.
			 */
			if (config instanceof File) {
				Properties configs = new Properties();
				configs.loadFromXML(new FileInputStream(config));

				System.setProperty("log4j.configuration", configs.getProperty("log-config"));
				try {
					Network.start(configs, configs.getProperty("ip-address"), configs.getProperty("mac-address"), configs
							.getProperty("gateway"), configs.getProperty("netmask"), configs.getProperty("capture-filter"));
				} catch (NetworkException ne) {
					System.err.println("Some error occured during the starting of the network: \n" + ne.getMessage());
					System.exit(-1);
				}
				try {
					boolean keepRunning = true;
					StringTokenizer st;
					BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
					String inputLine;
					String command;
					String destinationAddress;
					
					while(keepRunning)
					{
						
						inputLine = input.readLine();
						st = new StringTokenizer(inputLine);
						
						command = st.nextToken();
						
						if(command.equals("ping"))  // executes a ping request
						{
							if(st.countTokens() != 1)
							{
								System.out.println("oops, you gave me too much or too less arguments! usage: ping <ip-address>");
							}
							else
							{
								destinationAddress = st.nextToken();
								
								if(NetUtils.isValidIP(destinationAddress))  // check if IP is valid
								{
									//do the ping!
									PingSender.getInstance().sendPing(destinationAddress);
//									ICMPPacket icmprequest = ICMPPacket.createICMPPacket(ICMPPacket.ECHO_REQUEST, (byte)0, (short)0, (short)0, new byte[] {42, 42, 42, 42, 42, 42, 42, 42});
//									IPPacket iprequest = IPPacket.createDefaultIPPacket(IPPacket.ICMP_PROTOCOL, (short)0, Network.ip, destinationAddress, icmprequest.getPacket());
//									InternetLayerFactory.getInstance().send(iprequest);
//									System.out.println("#### Sent ICMP Echo Request Packet to " + destinationAddress);
								}
								else
								{
									System.out.println("hey, the IP you gave me is not valid! shame on you!");
								}
							}
						}
						else if(command.equals("exit") || command.equals("quit"))
						{
							System.out.println("so long!");
							keepRunning = false;
						}
						else
						{
							System.out.println("unknown command, use 'ping <addr>' or 'exit' or 'quit'");
						}
						
					}
					Network.stop();
					System.exit(0);
				} catch (Exception ioe) {
					Network.stop();
					System.err.println("Some error occured during stopping the network: \n" + ioe.getMessage());
					System.exit(-1);

				}
			}
		} catch (IOException e) {
			System.err.println(name + ": Error parsing config\n" + e.getMessage());
			return;
		}
	}
}
