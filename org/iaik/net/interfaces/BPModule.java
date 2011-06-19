package org.iaik.net.interfaces;

import org.iaik.net.packets.Packet;


public interface BPModule {
	void passThrough(Packet packet);
	void setNextModule(BPModule next);
	void setParameter(String parameter);
}
