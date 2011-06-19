package org.iaik.net.bpmodules;

import java.util.Random;

import org.iaik.net.interfaces.BPModule;
import org.iaik.net.packets.Packet;

/**
 * simulates a packet loss on the network.
 * 
 * Parameters:
 *  -  The packet loss can be specified as a module-parameter.
 *  
 * Example-Configuration:
 *  <entry key="BPModule00">org.iaik.net.bpmodules.PacketLoss</entry>
 *  <entry key="BPModule00param">0.3</entry>	
 * 
 * @author tebner
 *
 */
public class PacketLoss implements BPModule {
	BPModule nextModule;
	double loss;
	
	static final double defaultLoss = 0.1; 
	
	@Override
	public void passThrough(Packet packet) {
		
		if(loss == 0)
		{
			nextModule.passThrough(packet);
			return;
		}
		
		Random rand = new Random();
		
		boolean drop = (rand.nextFloat() / loss) <= 1;
		
		
		if(!drop)
			nextModule.passThrough(packet);
	}

	@Override
	public void setNextModule(BPModule next) {
		nextModule = next;
	}

	@Override
	public void setParameter(String parameter) {
		if(parameter == null)
		{
			loss = defaultLoss;
			return;
		}
		
		try
		{
			loss = Double.parseDouble(parameter);
		}
		catch(NumberFormatException ex)
		{
			loss = defaultLoss;
		}
		
		if(loss < 0 || loss > 1)
			loss = defaultLoss;
	}

}
