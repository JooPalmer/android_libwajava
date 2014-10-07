package com.smorra.libwajava;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class WATreeReader
{

	private static byte[] getToken(ByteBuffer bb, int token)
	{
		if (token < WATokenMap.primary.length)
			return WATokenMap.primary[token].getBytes();
		if (token >= 236)
		{
			token = bb.get() & 0xFF;
			return WATokenMap.secondary[token].getBytes();
		}
		token = bb.get() & 0xFF;
		return WATokenMap.primary[token].getBytes();
	}

	private static byte[] fillArray(ByteBuffer buf, int size)
	{
		byte[] ret = new byte[size];
		System.arraycopy(buf.array(), buf.position(), ret, 0, size);
		buf.position(buf.position() + size);
		return ret;
	}

	private static int readInt24(ByteBuffer buf)
	{
		byte b1 = buf.get();
		byte b2 = buf.get();
		byte b3 = buf.get();
		return ((b1 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b3 & 0xFF);
	}

	private static byte[] readString(ByteBuffer buf, int token)
	{
		if (token > 2 && token < 0xf5)
		{
			return getToken(buf, token);
		}
		else if (token == 0xFC)
		{
			int size = buf.get() & 0xFF;
			return fillArray(buf, size);
		}
		else if (token == 0xFD)
		{
			int size = readInt24(buf);
			return fillArray(buf, size);
		}
		else if (token == 0xFA)
		{
			byte[] user = readString(buf, buf.get() & 0xFF);
			byte[] server = readString(buf, buf.get() & 0xFF);
			if (user.length > 0 && server.length > 0)
				return WAUtil.concat(WAUtil.concat(user, new byte[] { '@' }), server);
			else if (server.length > 0)
				return server;

		}
		System.out.println("UNRESOLVED TOKEN: " + token);
		return new byte[] {};

	}

	private static ArrayList<WAAttribute> readAttributes(ByteBuffer buf, int size)
	{
		int n = (size - 2 + size % 2) / 2;

		ArrayList<WAAttribute> ret = new ArrayList<WAAttribute>();
		for (int i = 0; i < n; i++)
		{
			int token = buf.get() & 0xFF;
			byte[] key = readString(buf, token);
			token = buf.get() & 0xFF;
			byte[] value = readString(buf, token);

			ret.add(new WAAttribute(key, value));
		}
		return ret;
	}

	public static WAElement nextTreeInternal(ByteBuffer buf)
	{
		int token = buf.get() & 0xFF;
		int size = 0;
		if (token == 0xF8)
			size = buf.get() & 0xFF;
		else if (token == 0xF9)
			size = buf.getShort() & 0xFFFF;

		token = buf.get() & 0xFF;
		if (token == 1)
		{
			ArrayList<WAAttribute> attributes = readAttributes(buf, size);
			WAElement ret = new WAElement("start".getBytes());
			ret.attributes = attributes;
			return ret;
		}
		else if (token == 2)
			return null;

		byte[] tag = readString(buf, token);
		ArrayList<WAAttribute> attributes = readAttributes(buf, size);
		if (size % 2 == 1)
		{
			WAElement ret = new WAElement(tag);
			ret.attributes = attributes;
			return ret;
		}
		token = buf.get() & 0xFF;
		if (isListTag(token))
		{
			WAElement ret = new WAElement(tag);
			ret.attributes = attributes;
			ret.children = readList(buf, token);
			return ret;
		}

		WAElement ret = new WAElement(tag);
		ret.attributes = attributes;
		ret.text = readString(buf, token);
		return ret;
	}

	private static ArrayList<WAElement> readList(ByteBuffer buf, int token)
	{
		int size = getListSize(buf, token);
		ArrayList<WAElement> ret = new ArrayList<WAElement>();
		for (int i = 0; i < size; i++)
		{
			ret.add(nextTreeInternal(buf));
		}
		return ret;
	}

	private static boolean isListTag(int token)
	{
		return ((token == 248) || (token == 0) || (token == 249));
	}

	private static int getListSize(ByteBuffer buf, int token)
	{
		if (token == 0xF8)
			return buf.get() & 0xFF;
		else if (token == 0xF9)
			return buf.getShort() & 0xFFFF;
		return 0;
	}

}
