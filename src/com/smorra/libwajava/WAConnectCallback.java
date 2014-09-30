package com.smorra.libwajava;


public interface WAConnectCallback
{
	public enum Reason
	{
		SOCKET_ERROR, NOT_AUTHORIZED
	};
	public void onConnect();
	public void onConnectFailure(Reason reason);
}
