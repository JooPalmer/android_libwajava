package com.smorra.libwajava.callbacks;

import com.smorra.libwajava.WAElement;

public interface WARawCallback
{

	public void onConnect();

	public void onRead(WAElement node);

	public void onConnectFailure();

	public void onDisconnected();

}
