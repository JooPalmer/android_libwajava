package com.smorra.libwajava;

import java.util.ArrayList;
import java.util.Date;

public class WAGroup
{
	public String id;
	public String owner;
	public String subject;
	public Date creationDate;

	public ArrayList<String> participants = new ArrayList<String>();
}
