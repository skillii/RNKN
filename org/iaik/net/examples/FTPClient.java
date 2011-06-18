package org.iaik.net.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.iaik.net.Network;
import org.iaik.net.RUDP.RUDPClientConnection;
import org.iaik.net.RUDP.RUDPServerConnection;
import org.iaik.net.RUDP.RUDPConnection;
import org.iaik.net.examples.FTPClientCallback.ClientCallbackState;
import org.iaik.net.exceptions.NetworkException;
import org.iaik.net.exceptions.RUDPException;
import org.iaik.net.interfaces.RUDPClientCallback;
import org.iaik.net.interfaces.RUDPServerCallback;
import org.iaik.net.utils.NetUtils;
import org.iaik.net.utils.PingSender;

public class FTPClient {
	
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
		RUDPClientConnection myConnection = null;
		FTPClientCallback myCallback = null;
		Condition transferCondition;
		Lock transferConditionLock;
		
		transferConditionLock = new ReentrantLock();
		transferCondition = transferConditionLock.newCondition();

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
						System.out.println("Please enter your command:\n");
						inputLine = input.readLine();
						st = new StringTokenizer(inputLine);
						
						command = st.nextToken();
						
						if(command.equals("list"))  // list files on server
						{
						  FTPCmdListFiles cmd = new FTPCmdListFiles("");	
						  
						  
						  ((FTPClientCallback)myCallback).setCallbackState(ClientCallbackState.AwaitingFileList);
						  myConnection.sendData(cmd.getCommand());
						  
						  transferConditionLock.lock();
						  transferCondition.await();
						  transferConditionLock.unlock();
						  
						  if(myCallback.getCallbackState() == ClientCallbackState.Error)
						  {
						    System.out.println(myCallback.getErrorMessage());	  
						  }
						  else if(myCallback.getCallbackState() == ClientCallbackState.FileListComplete)
						  {
							System.out.println("Loading file list completed\n");
							System.out.println("===========================\n");
							System.out.println(myCallback.getReceivedData().toString());
						  }
						}
						else if(command.equals("get"))
						{
						  if(st.countTokens() != 1)
						  {
							  System.out.println("oops, you gave me too much or too less arguments! usage: ...");
						  }
						  else
						  {
						    FTPCmdGetFile cmd = new FTPCmdGetFile(st.nextToken());
						    
						    ((FTPClientCallback)myCallback).setCallbackState(ClientCallbackState.AwaitingFile);
						    myConnection.sendData(cmd.getCommand());
						    
						    transferConditionLock.lock();
							transferCondition.await();
							transferConditionLock.unlock();
						  }
						}
						else if(command.equals("put"))
						{
						  if(st.countTokens() != 1)
						    System.out.println("oops, you gave me too much or too less arguments! usage: ...");
						  else
						  {
						    String fileName = st.nextToken();
						    
						    File file = new File(fileName);
						    
						    FTPCmdPutFile cmd1 = new FTPCmdPutFile(fileName);
						    FTPCmdFileData cmd2 = new FTPCmdFileData(file);
						    
						    
						    ((FTPClientCallback)myCallback).setCallbackState(ClientCallbackState.PuttingFile);
						    myConnection.sendData(cmd1.getCommand());
						    myConnection.sendData(cmd2.getCommand());
						  }
						}
						
						else if(command.equals("connect"))
						{
							if(st.countTokens() != 2)
							  System.out.println("oops, you gave me too much or too less arguments! usage: ...");
							else
							{
							  String destAddress = st.nextToken();
							  int remotePort = Integer.parseInt(st.nextToken());
							
							  if(NetUtils.isValidIP(destAddress))
							  {
							    
								myCallback = new FTPClientCallback(transferCondition, transferConditionLock);  
							    myConnection = new RUDPClientConnection(34000,destAddress,remotePort, myCallback);
							    myCallback.setConnection(myConnection);
							    
							  
							    try
							    {
							      myConnection.connect();
							    }
							    catch(RUDPException ex)
							    {
							      System.out.println("Failed to connect " + ex.getMessage());
							    }
							  }
							  else
							  {
							    System.out.println("IP Address not valid");
							  }
	                        }
						}
						else if(command.equals("exit") || command.equals("quit"))
						{
							myConnection.disconnect();
							System.out.println("so long!");
							keepRunning = false;
						}
						else
						{
							System.out.println("unknown command");
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
