package com.smorra.libwajava.callbacks;


public interface WAConnectCallback
{
	public enum Reason
	{
		SOCKET_ERROR, NOT_AUTHORIZED
	};
	public void onConnectSuccess();
	public void onConnectFailure(Reason reason);
}
