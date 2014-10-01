package com.smorra.libwajava;

import java.util.Date;

public interface WAStatusCallback
{
	public void onStatus(Date set, byte[] status);
}
