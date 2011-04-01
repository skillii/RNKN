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

package org.iaik.net.factories;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.iaik.net.exceptions.NetworkException;
import org.iaik.net.interfaces.InternetLayer;
import org.iaik.net.interfaces.TransportLayer;
import org.iaik.net.layers.DefaultInternetLayer;

/**
 * 
 * @author <a href="mailto:stefan.kraxberger@iaik.tugraz.at">Stefan
 *         Kraxberger</a>
 * @date Jan 11, 2007
 * @version $Rev: 930 $ $Date: 2007/02/13 16:50:30 $
 */
public class InternetLayerFactory {

	/** The properties for the factory class. */
	private static Properties properties = null;

	/**
	 * The transport layer which has been created by a call to the
	 * {@link #createInstance(Properties, DefaultInternetLayer)} method.
	 */
	private static InternetLayer internetlayer = null;

	/** The logger for this class */
	private static Log staticLog = LogFactory.getLog(InternetLayerFactory.class);

	/**
	 * Initializes the {@link InternetLayerFactory} with the necessary
	 * parameters. Since this is a static class all the provided parameters are
	 * stored also as static parameters.
	 * 
	 * @param properties
	 *            The properties for the {@link InternetLayerFactory}.
	 * @param internetlayer
	 *            The {@link DefaultInternetLayer} which is beneath the
	 *            {@link TransportLayer}.
	 * @throws NetworkException
	 *             Thrown if the {@link InternetLayerFactory} couldn't be
	 *             initialized.
	 */
	public static void init(Properties properties) throws NetworkException {
		if (properties instanceof Properties)
			InternetLayerFactory.properties = properties;
		else
			throw new NetworkException("Properties must be specified for the initialization of the network layer factory!");

		staticLog.debug("NetworkLayerFactory initialized successfully");
	}

	/**
	 * Creates a new {@link InternetLayer} class from the specified parameters
	 * {@link Properties}. This method always creates a new network layer and
	 * sets it as the current instance and overwrites the old one. To obtain the
	 * currently used instance the method {@link #getInstance()} must be called.
	 * 
	 * @param properties
	 *            The properties for the new {@link InternetLayer}.
	 * @return The newly created {@link InternetLayer}.
	 * @throws NetworkException
	 *             Thrown if an error occured during the creation.
	 */
	public static InternetLayer createInstance(Properties properties) throws NetworkException {

		InternetLayerFactory.properties = properties;

		try {
			InternetLayer internetLayer = (InternetLayer) Class.forName(properties.getProperty("internetlayer")).newInstance();

			InternetLayerFactory.internetlayer = internetLayer;

			internetLayer.setProperties(properties);
			internetLayer.init();

			staticLog.debug("Network layer created successfully!");

			return internetLayer;

		} catch (Exception e) {
			throw new NetworkException("NetworkLayer couldn't be instantiated!\nReason: " + e.getMessage());
		}

	}

	/**
	 * Creates a new {@link InternetLayer} class from the parameters
	 * {@link Properties} which must have been specified through a succeeding
	 * call to {@link #init(Properties}. This method always creates a new
	 * network layer and sets it as the current instance and overwrites the old
	 * one. To obtain the currently used instance the method
	 * {@link #getInstance()} must be called.
	 * 
	 * @return The newly created {@link InternetLayer}.
	 * @throws NetworkException
	 *             Thrown if an error occured during the creation.
	 */
	public static InternetLayer createInstance() throws NetworkException {
		if (properties instanceof Properties) {
			return createInstance(properties);
		} else
			throw new NetworkException("Couldn't create a netowrk layer instance because the factory wasn't initialized correctly!");
	}

	/**
	 * Returns the currently used {@link InternetLayer} instance. If no instance
	 * has been created so far <code>null</code> is returned.
	 * 
	 * @return The currently used {@link InternetLayer} instance.
	 */
	public static InternetLayer getInstance() {
		if (internetlayer instanceof InternetLayer) {
			return internetlayer;
		} else
			return null;
	}
}
