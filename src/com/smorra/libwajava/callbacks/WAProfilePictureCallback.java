package com.smorra.libwajava.callbacks;

public interface WAProfilePictureCallback
{
	public void onProfilePictureSuccess(byte[] image);
	public void onProfilePictureError();
}
