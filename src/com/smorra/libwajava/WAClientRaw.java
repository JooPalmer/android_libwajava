package com.smorra.libwajava;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.smorra.asyncsocket.TcpClient;
import com.smorra.asyncsocket.TcpClientCallback;

public class WAClientRaw implements TcpClientCallback
{
	TcpClient tcpClient;
	WACallbackRaw callback;
	private static final String WHATSAPP_DEVICE = "Android";
	private static final String WHATSAPP_VER = "2.11.378";
	private static final String WHATSAPP_HOST = "c.whatsapp.net";
	private static final String WHATSAPP_SERVER = "s.whatsapp.net";
	private static final int PORT = 443;
	byte[] buffer = new byte[0];

	private WAKeyStream inputKeystream;
	private WAKeyStream outputKeystream;
	private boolean encrypt = false;

	public WAClientRaw(WACallbackRaw callback) throws IOException
	{
		tcpClient = new TcpClient(this, WHATSAPP_HOST, PORT);
		this.callback = callback;
	}

	public void sendResponse(byte[] password, byte[] challenge, byte[] username) throws UnsupportedEncodingException, IOException, InterruptedException, SAXException, ParserConfigurationException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException
	{
		byte[][] keys = WAKeyStream.generateKeys(password, challenge);
		inputKeystream = new WAKeyStream(keys[2], keys[3]);
		outputKeystream = new WAKeyStream(keys[0], keys[1]);
		byte[] data = WAUtil.concat(new byte[] { 0, 0, 0, 0 }, WAUtil.concat(username, challenge));
		byte[] responseChallenge = outputKeystream.encodeMessage(data, 0, 4, data.length - 4);

		WAElement element = new WAElement("response".getBytes());
		element.attributes.add(new WAAttribute("xmlns".getBytes(), "urn:ietf:params:xml:ns:xmpp-sasl".getBytes()));
		element.text = responseChallenge.clone();
		write(element);
		encrypt = true;
	}

	public void sendHeader() throws IOException, InterruptedException
	{
		ByteBuffer data = ByteBuffer.allocate(4);
		data.put(new byte[] { 'W', 'A' });
		data.put((byte) 1);
		data.put((byte) 4);
		tcpClient.write(data.array());

		byte[] cmd = getListStart(5);
		cmd = WAUtil.concat(cmd, new byte[] { 1 });
		ArrayList<WAAttribute> attributes = new ArrayList<WAAttribute>();
		attributes.add(new WAAttribute("to".getBytes(), WHATSAPP_SERVER.getBytes()));
		attributes.add(new WAAttribute("resource".getBytes(), (WHATSAPP_DEVICE + '-' + WHATSAPP_VER + '-' + PORT).getBytes()));
		cmd = WAUtil.concat(cmd, getAttributes(attributes));
		cmd = WAUtil.concat(getInt24(cmd.length), cmd);
		tcpClient.write(cmd);

	}

	public void write(WAElement element) throws IOException, InterruptedException, SAXException, ParserConfigurationException, InvalidKeyException, NoSuchAlgorithmException
	{
		System.out.println("Write: " + new String(element.serialize(), "UTF-8"));
		byte[] cmd = getXml(element);

		if (!encrypt)
		{
			cmd = WAUtil.concat(getInt24(cmd.length), cmd);
			tcpClient.write(cmd);
		}
		else
			tcpClient.write(encode(cmd));
	}

	private byte[] encode(byte[] cmd) throws InvalidKeyException, NoSuchAlgorithmException
	{

		byte[] data = outputKeystream.encodeMessage(cmd, cmd.length, 0, cmd.length);
		int len = data.length | (8 << 20);
		byte[] integer = getInt24(len);
		byte[] ret = new byte[3 + data.length];
		System.arraycopy(integer, 0, ret, 0, 3);
		System.arraycopy(data, 0, ret, 3, data.length);
		return ret;
	}

	private byte[] getXml(WAElement firstChild)
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

	private int getListSize(ByteBuffer buf, int token)
	{
		if (token == 0xF8)
			return buf.get() & 0xFF;
		else if (token == 0xF9)
			return buf.getShort() & 0xFFFF;
		return 0;
	}

	private ArrayList<WAElement> readList(ByteBuffer buf, int token)
	{
		int size = getListSize(buf, token);
		ArrayList<WAElement> ret = new ArrayList<WAElement>();
		for (int i = 0; i < size; i++)
		{
			ret.add(nextTreeInternal(buf));
		}
		return ret;
	}

	private byte[] getListStart(int len)
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

	private byte[] getBytes(byte[] bytes)
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

	private byte[] getInt24(int integer)
	{
		return new byte[] { (byte) ((integer & 0xFF0000) >> 16), (byte) ((integer & 0xFF00) >> 8), (byte) (integer & 0xFF) };
	}

	private byte[] getAttributes(ArrayList<WAAttribute> array)
	{
		byte[] ret = new byte[0];

		for (WAAttribute attr : array)
		{
			ret = WAUtil.concat(ret, getBytes(attr.name));
			ret = WAUtil.concat(ret, getBytes(attr.value));
		}
		return ret;

	}

	@Override
	public void onConnectFailed(TcpClient tcpClient)
	{
		callback.onConnectFailure();
		System.out.println("CANT CONNECT");
	}

	@Override
	public void onConnect(TcpClient tcpClient)
	{
		callback.onConnect();
	}

	@Override
	public void onDisconnected(TcpClient tcpClient)
	{
		System.out.println("WA DISCONNECT");
		callback.onDisconnected();
	}

	public void close() throws IOException
	{
		tcpClient.close();
	}

	@Override
	public void onRead(TcpClient tcpClient, byte[] readBytes)
	{
		try
		{
			buffer = WAUtil.concat(buffer, readBytes);

			while (true)
			{
				if (buffer.length < 3)
					break;

				byte[] headerSizePart1 = new byte[2];

				System.arraycopy(buffer, 1, headerSizePart1, 0, 2);

				int stanzaLen = ByteBuffer.wrap(headerSizePart1).getShort() & 0xFFFF + ((buffer[0] & 0x0F) << 16);
				if (buffer.length < stanzaLen + 3)
					break;
				byte[] data = new byte[stanzaLen];
				System.arraycopy(buffer, 3, data, 0, data.length);
				handleStanza(WAUtil.concat(new byte[] { buffer[0] }, WAUtil.concat(headerSizePart1, data)));
				byte[] new_buffer = new byte[buffer.length - 3 - data.length];
				System.arraycopy(buffer, 3 + stanzaLen, new_buffer, 0, new_buffer.length);
				buffer = new_buffer;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void handleStanza(byte[] d) throws ParserConfigurationException, SAXException, IOException, InvalidKeyException, NoSuchAlgorithmException
	{
		ByteBuffer buf = ByteBuffer.wrap(d);
		int flags = buf.get() >> 4;
		int stanzaSize = buf.getShort() & 0xFFFF; // skip size
		if ((flags & 8) != 0)
		{
			byte[] new_d = new byte[d.length - 3];
			System.arraycopy(d, 3, new_d, 0, new_d.length);

			int realsize = stanzaSize - 4;
			byte[] decrypted = inputKeystream.decodeMessage(new_d, realsize, 0, realsize);

			WAElement read = nextTreeInternal(ByteBuffer.wrap(decrypted));
			if (read != null)
			{
				System.out.println("Read: " + new String(read.serialize()));

				callback.onRead(read);
			}
			// System.out.println("ENCRYPTED STANZA: \"" + new String(decrypted,
			// "UTF-8") + "\"");
		}
		else
		{
			WAElement read = nextTreeInternal(buf);
			System.out.println("Read: " + new String(read.serialize()));
			callback.onRead(read);
		}
	}

	private WAElement nextTreeInternal(ByteBuffer buf)
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

	private boolean isListTag(int token)
	{
		return ((token == 248) || (token == 0) || (token == 249));
	}

	private ArrayList<WAAttribute> readAttributes(ByteBuffer buf, int size)
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

	public byte[] getToken(ByteBuffer bb, int token)
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

	private byte[] readString(ByteBuffer buf, int token)
	{
		if (token > 4 && token < 0xf5)
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
		else if (token == 0xFE)
		{
			token = buf.get() & 0xFF;
			return getToken(buf, token + 0xf5);
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
		return new byte[] {};

	}

	private int readInt24(ByteBuffer buf)
	{
		byte b1 = buf.get();
		byte b2 = buf.get();
		byte b3 = buf.get();
		return (b1 << 16) | (b2 << 8) | b3;
	}

	private byte[] fillArray(ByteBuffer buf, int size)
	{
		byte[] ret = new byte[size];
		System.arraycopy(buf.array(), buf.position(), ret, 0, size);
		buf.position(buf.position() + size);
		return ret;
	}

	@Override
	public void onWritten(TcpClient tcpClient)
	{
		// TODO Auto-generated method stub

	}
}
