package com.smorra.libwajava.callbacks;

public interface WALastSeenCallback
{
	public void onLastSeenSuccess(int seconds);

	public void onLastSeenError();
}
