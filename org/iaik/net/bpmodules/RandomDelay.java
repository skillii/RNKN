package org.iaik.net.bpmodules;

import java.sql.Time;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.iaik.net.interfaces.BPModule;
import org.iaik.net.packets.Packet;


/**
 * simulates a a random delay on the network. So packages may arrive in a different order.
 * For each package a random delay will be calculated!
 * 
 * Parameters:
 *  - the minimum delay on the network
 *  - the maximum delay on the network
 *  
 * Example-Configuration:
 *  <entry key="BPModule01">org.iaik.net.bpmodules.RandomDelay</entry>
 *  <entry key="BPModule01param">50,1000</entry>	
 * 
 * @author tebner
 *
 */
public class RandomDelay implements BPModule {
	private BPModule nextModule;
	private int minDelay, maxDelay;
	
	private final int defaultMinDelay = 40;
	private final int defaulMaxDelay = 100;
	
	private final int timerInterval = 20;
	
	private Random rand = new Random();
	
	private Vector<PacketTimePair> packages = new Vector<PacketTimePair>();
	
	private Timer timer = null;

	@Override
	public void passThrough(Packet packet) {
		int delay = rand.nextInt(maxDelay-minDelay) + minDelay;
		
		PacketTimePair pair = new PacketTimePair();
		
		pair.sendTime = System.currentTimeMillis() + delay;
		pair.packet = packet;
		
		synchronized(packages)
		{
			packages.add(pair);
		}
		
		
		if(timer == null)
		{
			//start timer:
			timer = new Timer();
			timer.scheduleAtFixedRate(new SendTimer(), timerInterval, timerInterval);
		}
	}

	@Override
	public void setNextModule(BPModule next) {
		nextModule = next;
	}

	@Override
	public void setParameter(String parameter) {
		if(parameter == null)
		{
			minDelay = defaultMinDelay;
			maxDelay = defaulMaxDelay;
			return;
		}
		
		try
		{
			StringTokenizer tok = new StringTokenizer(parameter, ",");
			if(tok.countTokens() != 2)
			{
				minDelay = defaultMinDelay;
				maxDelay = defaulMaxDelay;
				return;
			}
			
			minDelay = Integer.parseInt(tok.nextToken());
			maxDelay = Integer.parseInt(tok.nextToken());
		}
		catch(NumberFormatException ex)
		{
			minDelay = defaultMinDelay;
			maxDelay = defaulMaxDelay;
		}
	}
	
	private class PacketTimePair
	{
		public Packet packet;
		public long sendTime;
	}
	
	private class SendTimer extends TimerTask
	{

		@Override
		public void run() {
			synchronized (packages)
			{
				for(int i = 0; i < packages.size(); i++)
				{
					if(packages.get(i).sendTime<= System.currentTimeMillis())
					{
						nextModule.passThrough(packages.get(i).packet);
						packages.remove(i);
						i--;
						continue;
					}
				}
			}
		}
		
	}
}
