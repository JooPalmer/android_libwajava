package com.smorra.libwajava;

public interface WACallbackRaw
{
	public void onConnect();
	public void onRead(WAElement node);
	public void onConnectFailure();
	public void onDisconnected();
}
