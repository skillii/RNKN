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

/**
 * 
 * @author <a href="mailto:stefan.kraxberger@iaik.tugraz.at">Stefan
 *         Kraxberger</a>
 * @date Jan 11, 2007
 * @version $Rev: 930 $ $Date: 2007/02/15 08:41:53 $
 */
public class StackParameters {
	/** Initial IP address. Set to "0.0.0.0" for DHCP */
	public final static String INIT_IP_ADDR = "0.0.0.0";

	/** Initial MAC for the virtual interface driver */
	public final static String INIT_MAC = "0A:02:03:04:05:06";

	/** Time between the iterations of packet processing and sending. */
	public final static int POOLING_WAIT_TIME = 20;

	/** Maximum size of network buffers used in this implementation */
	public final static int MAX_BUFFER_SIZE = 5000;

	/** Maximum amount of ports available by this network stack */
	public static final int MAX_AVAILABLE_PORTS = 65535;

	public static final short PSEUDO_IP_HEADER_SIZE = 12;

	public final static String BROADCAST_MAC_ADDRESS = "FF:FF:FF:FF:FF:FF";

	/** MTU Size */
	public final static short PACKET_MTU_SIZE = 1500;

	/** Maximum Data length in bytes (max IP Packet size excl. Header) */
	public final static int PAYLOAD_MAX_DATA_SIZE = PACKET_MTU_SIZE - 20;

	// minus 20 bytes IP header

	/** Size of the <code>Payload</code> pool */
	public final static byte PAYLOAD_POOL_SIZE = 8;

	/** Initial MSS assumed until remote hosts sends a different one */
	public final static int TCP_INITIAL_SND_MAX_SEGMENT_SIZE = 536;

	// Default by RFC-793 Section 3.1

	/** MSS we send with our initial SYN Packet */
	public final static int TCP_RCV_MAX_SEGMENT_SIZE = PACKET_MTU_SIZE - 40;

	// RFC 879

	/**
	 * Initial window size. The size marks the amount of TCP payload bytes the
	 * stack is willing to buffer until the next acknowlege
	 */
	public final static int TCP_INITIAL_WINDOW_SIZE = 2 * TCP_RCV_MAX_SEGMENT_SIZE;

	/** The count of TCP connections the stack can handle at one time */
	public final static byte TCP_CONNECTION_POOL_SIZE = 32;

	/**
	 * Size of the send buffer in bytes used for the application layer. NOTE:
	 * also the data for retransmission will be stored in this buffer so better
	 * chosse it bigger.
	 */
	public final static int TCP_CONNECTION_SND_BUFFER_SIZE = 5000;

	/**
	 * Size of the receive buffer in bytes used for the application layer
	 */
	public final static int TCP_CONNECTION_RCV_BUFFER_SIZE = 3000;

	public final static int TCP_CONNECTION_TIMEOUT = 100000;

	/** The timeout in milli seconds to wait for an acknowlwge */
	public final static int TCP_RETRANSMISSION_TIMEOUT = 1000;

	/** time for that the stack waits until it closes the connection */
	public final static int TCP_MAX_TRY_TO_RETRANSMIT_TIME = 60000;

	/**
	 * Since syn retransmitt should be done with a bigger timeout as a normal
	 * retransmitt. this is the multiplicator for the TCP_RETRANSMISSION_TIMEOUT
	 * to get the timeout for sny retransmitt.
	 */
	public static final int TCP_SYN_RETRANSMIT_TIMEOUT_MULTIPLYER = 3;

	/** How many times a SYN should be retransmitted */
	public final static int TCP_MAX_TIMES_SYN_RETRANSMIT = 3;

	/**
	 * milliseconds which have to pass until a connection in TIME WAIT state
	 * changes to CLOSED RFC says that is should be 2*max segment lifetime which
	 * is about 2 min... since connections are blocked for this time we are
	 * forced to choose a smaller value to get a fast stack.
	 */
	public static final int TCP_TIME_WAIT_TIME = 2000;

	/**
	 * The count of UDP connections the stack can handle at one time
	 */
	public final static byte UDP_CONNECTION_POOL_SIZE = 8;

	/**
	 * Size of the send buffer in words of 4 bytes used for the application
	 * layer
	 */
	public final static int UDP_CONNECTION_SND_BUFFER_SIZE = 1000;

	/**
	 * Size of the receive buffer in words of 4 bytes used for the application
	 * layer
	 */
	public final static int UDP_CONNECTION_RCV_BUFFER_SIZE = 1000;

	/** Size of a UDP packet */
	public final static int UDP_DATA_SIZE = PAYLOAD_MAX_DATA_SIZE - 8;

	// minus 8 bytes UDP header

	/** The timeout in milli seconds to wait for an ARP response */
	public final static int ARP_TIMEOUT = 20000;

	/** Timeout in milli seconds between two net.loop() calls */
	public final static int PACKET_SEND_TIMEOUT = 20000;

	/**
	 * Timeout in milli seconds until all fragments of a payload has to have
	 * arrived
	 */
	public static final int REASSEMBLE_TIMEOUT = 30000; // 30 sec

	/**
	 * Timeout in milli seconds to wait for a UDP datagram to arrive Function is
	 * disabled if 0
	 */
	public final static int UDP_RECV_TIMEOUT = 10000;
}
