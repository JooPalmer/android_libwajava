package com.smorra.libwajava;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class WATreeWriter
{
	public static byte[] getXml(WAElement firstChild)
	{
		int len = 1;
		len += firstChild.attributes.size() * 2;
		if (firstChild.text != null || firstChild.children.size() > 0)
			len += 1;
		byte[] ret = getListStart(len);
		ret = WAUtil.concat(ret, getBytes(firstChild.name));
		ret = WAUtil.concat(ret, getAttributes(firstChild.attributes));

		if (firstChild.text != null)
			ret = WAUtil.concat(ret, getBytes(firstChild.text));

		else if (firstChild.children.size() > 0)
		{
			ret = WAUtil.concat(ret, getListStart(firstChild.children.size()));
			for (WAElement element : firstChild.children)
				ret = WAUtil.concat(ret, getXml(element));
		}

		return ret;
	}

	static byte[] getBytes(byte[] bytes)
	{
		byte[] ret;
		if (bytes.length >= 0x100)
		{
			ret = new byte[] { (byte) 0xFD };
			ret = WAUtil.concat(ret, getInt24(bytes.length));
		}
		else
		{
			ret = new byte[] { (byte) 0xFC };
			ret = WAUtil.concat(ret, new byte[] { (byte) bytes.length });
		}
		ret = WAUtil.concat(ret, bytes);

		return ret;
	}

	public static byte[] getInt24(int integer)
	{
		return new byte[] { (byte) ((integer & 0xFF0000) >> 16), (byte) ((integer & 0xFF00) >> 8), (byte) (integer & 0xFF) };
	}

	static byte[] getAttributes(ArrayList<WAAttribute> array)
	{
		byte[] ret = new byte[0];

		for (WAAttribute attr : array)
		{
			ret = WAUtil.concat(ret, getBytes(attr.name));
			ret = WAUtil.concat(ret, getBytes(attr.value));
		}
		return ret;

	}

	static byte[] getListStart(int len)
	{
		if (len == 0)
		{
			return new byte[] { 0 };
		}
		else if (len < 256)
		{
			return new byte[] { (byte) 0xF8, (byte) len };
		}
		else
		{
			ByteBuffer b = ByteBuffer.allocate(3);
			b.put((byte) 0xF9);
			b.putShort((short) len);
			return b.array();
		}
	}

}
