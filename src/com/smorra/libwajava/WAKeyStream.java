package com.smorra.libwajava;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class WAKeyStream
{
	private byte[] macKey;
	private WARC4 rc4;
	private int seq = 0;

	public WAKeyStream(byte[] key, byte[] macKey) throws UnsupportedEncodingException
	{
		this.macKey = macKey;
		rc4 = new WARC4(key);
	}

	public static byte[][] generateKeys(byte[] password, byte[] nonce) throws InvalidKeySpecException, NoSuchAlgorithmException
	{
		byte[][] ret = new byte[4][];
		byte[] nnonce = new byte[nonce.length + 1];
		System.arraycopy(nonce, 0, nnonce, 0, nonce.length);

		for (int i = 0; i < 4; i++)
		{
			nnonce[nnonce.length - 1] = (byte) (i + 1);
			ret[i] = WAUtil.wa_pbkdf2(password, nnonce);
		}
		return ret;
	}

	public byte[] encodeMessage(byte[] data, int macOffset, int offset, int length) throws InvalidKeyException, NoSuchAlgorithmException
	{
		byte[] encoded = rc4.cipher(data, offset, length);
		byte[] mac = computeMac(encoded, offset, length);
		byte[] ret;
		if (macOffset == data.length)
			ret = new byte[encoded.length + 4];
		else
			ret = new byte[encoded.length];
		System.arraycopy(encoded, 0, ret, 0, macOffset);
		System.arraycopy(mac, 0, ret, macOffset, 4);
		if (macOffset != data.length)
			System.arraycopy(encoded, macOffset + 4, ret, macOffset + 4, encoded.length - macOffset - 4);

		return ret;
	}

	public byte[] computeMac(byte[] encoded, int offset, int length) throws NoSuchAlgorithmException, InvalidKeyException
	{
		byte[] todo = new byte[length];
		System.arraycopy(encoded, offset, todo, 0, length);

		SecretKeySpec signingKey = new SecretKeySpec(macKey, "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signingKey);
		mac.update(todo);
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(seq);
		mac.update(bb.array());
		seq++;
		return mac.doFinal();
	}

	public byte[] decodeMessage(byte[] buffer, int macOffset, int offset, int length) throws InvalidKeyException, NoSuchAlgorithmException
	{
		computeMac(buffer, offset, length);
		return rc4.cipher(buffer, offset, length);
	}
}
