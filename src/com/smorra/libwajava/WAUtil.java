package com.smorra.libwajava;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;

public class WAUtil
{
	public static byte[] generateRequestToken(byte[] countryCode, byte[] phoneNumber) throws NoSuchAlgorithmException
	{

		String signature = "MIIDMjCCAvCgAwIBAgIETCU2pDALBgcqhkjOOAQDBQAwfDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFDASBgNVBAcTC1NhbnRhIENsYXJhMRYwFAYDVQQKEw1XaGF0c0FwcCBJbmMuMRQwEgYDVQQLEwtFbmdpbmVlcmluZzEUMBIGA1UEAxMLQnJpYW4gQWN0b24wHhcNMTAwNjI1MjMwNzE2WhcNNDQwMjE1MjMwNzE2WjB8MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEUMBIGA1UEBxMLU2FudGEgQ2xhcmExFjAUBgNVBAoTDVdoYXRzQXBwIEluYy4xFDASBgNVBAsTC0VuZ2luZWVyaW5nMRQwEgYDVQQDEwtCcmlhbiBBY3RvbjCCAbgwggEsBgcqhkjOOAQBMIIBHwKBgQD9f1OBHXUSKVLfSpwu7OTn9hG3UjzvRADDHj+AtlEmaUVdQCJR+1k9jVj6v8X1ujD2y5tVbNeBO4AdNG/yZmC3a5lQpaSfn+gEexAiwk+7qdf+t8Yb+DtX58aophUPBPuD9tPFHsMCNVQTWhaRMvZ1864rYdcq7/IiAxmd0UgBxwIVAJdgUI8VIwvMspK5gqLrhAvwWBz1AoGBAPfhoIXWmz3ey7yrXDa4V7l5lK+7+jrqgvlXTAs9B4JnUVlXjrrUWU/mcQcQgYC0SRZxI+hMKBYTt88JMozIpuE8FnqLVHyNKOCjrh4rs6Z1kW6jfwv6ITVi8ftiegEkO8yk8b6oUZCJqIPf4VrlnwaSi2ZegHtVJWQBTDv+z0kqA4GFAAKBgQDRGYtLgWh7zyRtQainJfCpiaUbzjJuhMgo4fVWZIvXHaSHBU1t5w//S0lDK2hiqkj8KpMWGywVov9eZxZy37V26dEqr/c2m5qZ0E+ynSu7sqUD7kGx/zeIcGT0H+KAVgkGNQCo5Uc0koLRWYHNtYoIvt5R3X6YZylbPftF/8ayWTALBgcqhkjOOAQDBQADLwAwLAIUAKYCp0d6z4QQdyN74JDfQ2WCyi8CFDUM4CaNB+ceVXdKtOrNTQcc0e+t";
		String classesMd5 = "P3b9TfNFCkkzPoVzZnm+BA=="; // 2.11.395 [*]

		byte[] key2 = Base64.decode("/UIGKU1FVQa+ATM2A0za7G2KI9S/CwPYjgAbc67v7ep42eO/WeTLx1lb1cHwxpsEgF4+PmYpLd2YpGUdX/A2JQitsHzDwgcdBpUf7psX1BU=", Base64.DEFAULT);

		byte[] data = concat(concat(Base64.decode(signature, Base64.DEFAULT), Base64.decode(classesMd5, Base64.DEFAULT)), phoneNumber);

		byte[] opad = new byte[64];
		byte[] ipad = new byte[64];
		for (int i = 0; i < opad.length; i++)
		{
			opad[i] = 0x5C;
			ipad[i] = 0x36;
		}

		for (int i = 0; i < 64; i++)
		{
			opad[i] = (byte) ((opad[i] & 0xFF) ^ (key2[i] & 0xFF));
			ipad[i] = (byte) ((ipad[i] & 0xFF) ^ (key2[i] & 0xFF));
		}

		data = concat(ipad, data);

		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(data);
		byte[] firstHash = md.digest();

		data = concat(opad, firstHash);
		md = MessageDigest.getInstance("SHA-1");
		md.update(data);
		byte[] secondHash = md.digest();

		return Base64.encode(secondHash, Base64.DEFAULT | Base64.NO_WRAP);
	}

	public static byte[] getCountryCode(byte[] phoneNumber) throws UnsupportedEncodingException
	{
		int[] ccs = new int[] { 1, 20, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 27, 28, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 30, 31, 32, 33, 34, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 36, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 3883, 389, 39, 3906, 40, 41, 420, 421, 422, 423, 424, 425, 426, 427, 428, 429, 43, 44, 45, 46, 47, 48, 49, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 51, 52, 53, 54, 55, 56, 57, 58, 590, 591, 592, 593, 594, 595, 596, 597, 598, 599, 60, 61, 62, 63, 64, 65, 66, 670, 671, 672, 673, 674, 675, 676, 677, 678, 679, 680, 681, 682, 683, 684, 685, 686, 687, 688, 689, 690, 691, 692, 693, 694, 695, 696, 697, 698, 699, 7, 800, 801, 802, 803, 804, 805, 806, 807, 808, 809, 81, 82, 83, 84, 850, 851, 852, 853, 854, 855, 856, 857, 858, 859, 86, 870, 871, 875, 876, 877, 878, 879, 880, 881, 882, 883, 884, 885, 886, 887, 888, 889, 89, 90, 91, 92, 93, 94, 95, 960, 961, 962, 963, 964, 965, 966, 967, 968, 969, 970, 971, 972, 973, 974, 975, 976, 977, 978, 979, 98, 990, 991, 992, 993, 994, 995, 996, 997, 998, 999 };
		int cc = -1;
		for (int i = 0; i < ccs.length; i++)
		{
			String c = String.valueOf(ccs[i]);
			if (new String(phoneNumber, "UTF-8").startsWith(c) && ccs[i] > cc)
				cc = ccs[i];
		}
		return String.valueOf(cc).getBytes();

	}

	public static byte[] xmlEncode(byte[] str)
	{

		byte[] ret = new byte[0];

		for (int i = 0; i < str.length; i++)
		{

			if ((str[i] & 0xFF) >= 128 || (str[i] & 0xFF) < 32 || (str[i] & 0xFF) == '\'' || (str[i] & 0xFF) == '<' || (str[i] & 0xFF) == '>' || (str[i] & 0xFF) == '"')
			{
				String entity = "&#" + (str[i] & 0xFF) + ";";
				ret = concat(ret, entity.getBytes());
			}
			else
				ret = concat(ret, new byte[] { str[i] });
		}

		return ret;
	}

	public static String xmlEncode(String str)
	{
		String ret = "";
		for (int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			if (c >= 128 || c < 32 || c == '\'' || c == '"' || c == '<' || c == '>')
			{
				ret += "&#" + (int) c + ";";
			}
			else
				ret += c;
		}
		return ret;
	}

	public static byte[] concat(byte[] b1, byte[] b2)
	{
		byte[] ret = new byte[b1.length + b2.length];
		System.arraycopy(b1, 0, ret, 0, b1.length);
		System.arraycopy(b2, 0, ret, b1.length, b2.length);
		return ret;
	}

	private static byte[] hmacSha1(byte[] value, byte[] key)
	{
		try
		{
			SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(signingKey);
			return mac.doFinal(value);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public static byte[] wa_pbkdf2(byte[] password, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException
	{
		/*
		 * 2 iterations, 20 keylength
		 */

		byte[] last = new byte[salt.length + 4];
		System.arraycopy(salt, 0, last, 0, salt.length);
		last[last.length - 1] = 1;
		byte[] xorsum = hmacSha1(last, password);
		last = xorsum.clone();

		for (int i = 1; i < 2; i++)
		{
			last = hmacSha1(last, password);

			for (int j = 0; j < xorsum.length; j++)
				xorsum[j] = (byte) ((xorsum[j] & 0xFF) ^ (last[j] & 0xFF));
		}

		byte[] ret = new byte[20];
		System.arraycopy(xorsum, 0, ret, 0, 20);
		return ret;
	}
}
