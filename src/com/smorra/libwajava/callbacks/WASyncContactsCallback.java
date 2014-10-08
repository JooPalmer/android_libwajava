package com.smorra.libwajava.callbacks;

import java.util.ArrayList;

import com.smorra.libwajava.WAElement;


public interface WASyncContactsCallback
{
	public void onSyncContacts(ArrayList<WAElement> existing, ArrayList<WAElement> nonexisting, ArrayList<WAElement> invalid);
	
}
