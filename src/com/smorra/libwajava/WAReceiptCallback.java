package com.smorra.libwajava;

import java.util.Date;

public interface WAReceiptCallback
{
	public void onReceipt(String from, String messageId, Date time);
}
