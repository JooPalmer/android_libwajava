package com.smorra.libwajava.callbacks;

import java.util.Date;

public interface WAReceiptCallback
{
	public void onReceipt(String from, String messageId, Date time);
}
