package com.smorra.libwajava;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.json.JSONObject;

import com.smorra.asyncsocket.SSLClient;
import com.smorra.asyncsocket.SSLClientCallback;

public class WACodeRequest implements SSLClientCallback
{
	SSLClient client;
	WACodeRequestCallback callback;
	String phoneNumber;
	String method;
	String buffer = "";

	public WACodeRequest(String method, String phoneNumber, WACodeRequestCallback callback) throws KeyManagementException, NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException
	{
		this.method = method;
		this.phoneNumber = phoneNumber;
		client = new SSLClient("v.whatsapp.net", 443, this);
		this.callback = callback;
	}

	@Override
	public void onConnectFailed(SSLClient sslClient)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnect(SSLClient sslClient)
	{
		try
		{
			byte[] cc = WAUtil.getCountryCode(phoneNumber.getBytes());
			byte[] phoneRest = new byte[phoneNumber.getBytes().length - cc.length];
			System.arraycopy(phoneNumber.getBytes(), cc.length, phoneRest, 0, phoneRest.length);

			String str = "GET /v2/code?method=" + method;
			str += "&in=" + new String(phoneRest, "UTF-8");
			str += "&cc=" + new String(cc, "UTF-8");
			str += "&id=%00%00%00%00%00%00%00%00%00%00%00%00%00%00%00%00";
			str += "&token=" + URLEncoder.encode(new String(WAUtil.generateRequestToken(cc, phoneRest), "UTF-8"), "UTF-8");
			str += " HTTP/1.1\n";
			str += "User-Agent: WhatsApp/2.11.395 Android/4.3 Device/GalaxyS3\n\n";

			System.out.println("SENDING: " + str);
			client.write(str.getBytes());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onDisconnected(SSLClient sslClient)
	{
		try
		{
			System.out.println("BUFFER IS " + buffer);
			int idx = buffer.indexOf("\r\n\r\n");
			String json = buffer.substring(idx + 4);
			JSONObject pages = new JSONObject(json);
			callback.onFinish(pages);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onRead(SSLClient sslClient, byte[] readBytes)
	{
		try
		{
			System.out.println("READ:" + new String(readBytes, "UTF-8"));
			buffer += new String(readBytes, "UTF-8");
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onWritten(SSLClient sslClient)
	{
		// TODO Auto-generated method stub

	}
}
