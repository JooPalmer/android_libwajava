package com.smorra.libwajava;

public class WAMessage
{
	public enum Type
	{
		TYPE_TEXT;
	}
	
	public Type type;
	public String from;
	public String to;
	public String notify;
	
	public byte[] body;
}
