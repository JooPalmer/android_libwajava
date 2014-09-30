package com.smorra.libwajava;

import java.io.UnsupportedEncodingException;


public class WARC4
{
	private byte[] s = new byte[256];
	private byte i = 0;
	private byte j = 0;

	public WARC4(final byte[] key) throws UnsupportedEncodingException
	{
		for (int i = 0; i < 256; i++)
			s[i] = (byte) i;
		for (int h = 0; h < 256; h++)
		{
			i = (byte) h;
			byte k = key[(i & 0xFF) % key.length];
			j = (byte) ((((j & 0xFF) + (k & 0xFF) + (s[i & 0xFF] & 0xFF))) & 0xFF);

			swap(i & 0xFF, j & 0xFF);
		}
		i = 0;
		j = 0;

		byte[] enc = new byte[768];
		for (int i = 0; i < enc.length; i++)
			enc[i] = (byte) String.valueOf(i).charAt(0);
		cipher(enc, 0, enc.length);
	}

	public byte[] cipher(byte[] enc, int offset, int length)
	{
		byte[] out = enc.clone();
		for (int n = length; n > 0; n--)
		{
			i = (byte) ((i & 0xFF) + 1);
			j = (byte) ((j & 0xFF) + s[i & 0xFF]);
			swap(i & 0xFF, j & 0xFF);
			byte d = enc[offset];
			out[offset] = (byte) (d ^ s[(s[i & 0xFF] + s[j & 0xFF]) & 0xFF]);
			offset++;
		}
		return out;
	}

	private void swap(int i, int j)
	{
		byte c = s[i];
		s[i] = s[j];
		s[j] = c;
	}

}