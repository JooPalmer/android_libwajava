package com.smorra.libwajava;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.smorra.libwajava.WAConnectCallback.Reason;
import com.smorra.libwajava.WAMessage.Type;

import android.util.Base64;

public class WAClient implements WACallbackRaw
{
	WAClientRaw client;
	String phoneNumber;
	String password;
	String displayName;
	WADisconnectCallback disconnectCB;
	WAMessageCallback messageCB;
	WAReceiptCallback receiptCB;

	Queue<Object> callbacks = new LinkedList<Object>();

	public WAClient(String phoneNumber, String password, String displayName)
	{
		this.phoneNumber = phoneNumber;
		this.password = password;
		this.displayName = displayName;
	}

	public void setDisconnectCallback(WADisconnectCallback callback)
	{
		disconnectCB = callback;
	}

	public void setMessageCallback(WAMessageCallback callback)
	{
		messageCB = callback;
	}

	public void setReceiptCallback(WAReceiptCallback callback)
	{
		receiptCB = callback;
	}

	public void connect(WAConnectCallback callback) throws IOException
	{
		callbacks.add(callback);
		client = new WAClientRaw(this);

	}

	@Override
	public void onConnect()
	{
		try
		{
			client.sendHeader();
			client.write(WAElement.fromString("<stream:features/>"));
			client.write(WAElement.fromString("<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='WAUTH-2' user='" + phoneNumber + "'/>"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void getGroups(WAGroupCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		callbacks.add(callback);
		client.write(WAElement.fromString("<iq id='getgroups-participating' type='get' xmlns='w:g' to='g.us'><list type='participating'></list></iq>"));
	}

	public void fillGroup(WAGroup group, WAFillGroupCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		callbacks.add(group);
		callbacks.add(callback);
		String str = "<iq id='getgroupparticipants' type='get' xmlns='w:g' to='" + group.id + "@g.us'><list></list></iq>";
		client.write(WAElement.fromString(str));
	}

	public void sendActive() throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		client.write(WAElement.fromString("<presence type='active'></presence>"));
	}

	public void getLastSeen(String phoneNumber, WALastSeenCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		callbacks.add(callback);
		client.write(WAElement.fromString("<iq to='" + phoneNumber + "@s.whatsapp.net' type='get' id='lastseen' xmlns='jabber:iq:last'><query/></iq>"));

	}

	public void getStatus(String phoneNumber, WAStatusCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		callbacks.add(callback);
		String str = "<iq to='s.whatsapp.net' type='get' xmlns='status' id='getstatus'>";
		str += "<status><user jid='" + phoneNumber + "@s.whatsapp.net'/></status>";
		str += "</iq>";
		client.write(WAElement.fromString(str));
	}

	public void sendTyping(String phoneNumber) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		client.write(WAElement.fromString("<chatstate to='" + phoneNumber + "@s.whatsapp.net'><composing/></chatstate>"));
	}

	public void sendMessage(String to, String body) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		String str = "<message to='" + WAUtil.xmlEncode(to) + "@s.whatsapp.net' type='text' id='message-1410987641-1' t='1410987641'>";
		str += "<x xmlns='jabber:x:event'><server/></x>";
		str += "<notify xmlns='urn:xmpp:whatsapp' name='" + WAUtil.xmlEncode(displayName) + "'/>";
		str += "<request xmlns='urn:xmpp:receipts'/>";
		str += "<body>" + WAUtil.xmlEncode(body) + "</body>";
		str += "</message>";
		client.write(WAElement.fromString(str));
	}

	@Override
	public void onRead(WAElement element)
	{
		try
		{
			String name = new String(element.name, "UTF-8");
			String xmlns = null;
			WAAttribute xmlnsAttr = element.getAttributeByName("xmlns".getBytes());
			if (xmlnsAttr != null)
				xmlns = new String(xmlnsAttr.value, "UTF-8");
			if (name.equals("challenge"))
			{
				client.sendResponse(Base64.decode(password, Base64.DEFAULT), element.text, phoneNumber.getBytes());
				// client.write(WAElement.fromString("<presence name='Stefan Smorra'></presence>"));
			}
			else if (name.equals("receipt"))
			{
				String from = new String(element.getAttributeByName("from".getBytes()).value, "UTF-8");
				from = from.substring(0, from.indexOf('@'));
				String id = new String(element.getAttributeByName("id".getBytes()).value, "UTF-8");
				String t = new String(element.getAttributeByName("t".getBytes()).value, "UTF-8");
				Date d = new Date(Long.parseLong(t) * 1000);
				if (receiptCB != null)
					receiptCB.onReceipt(from, id, d);

			}
			else if (name.equals("success"))
			{
				String str = "<presence name='" + WAUtil.xmlEncode(displayName) + "'></presence>";
				client.write(WAElement.fromString(str));
				WAConnectCallback cc = (WAConnectCallback) callbacks.poll();
				cc.onConnect();

			}
			else if (name.equals("failure"))
			{
				WAElement child = element.children.get(0);
				String childName = new String(child.name, "UTF-8");
				if (childName.equals("not-authorized"))
				{
					WAConnectCallback cc = (WAConnectCallback) callbacks.poll();
					cc.onConnectFailure(Reason.NOT_AUTHORIZED);
					close();
				}
			}
			else if (name.equals("message"))
			{
				String id = new String(element.getAttributeByName("id".getBytes()).value, "UTF-8");
				String from = new String(element.getAttributeByName("from".getBytes()).value, "UTF-8");
				from = from.substring(0, from.indexOf('@'));

				client.write(WAElement.fromString("<receipt to='" + from + "@s.whatsapp.net' id='" + id + "'/>"));

				String type = new String(element.getAttributeByName("type".getBytes()).value, "UTF-8");
				if (type.equals("text"))
				{
					WAMessage para = new WAMessage();
					para.body = element.getChildrenByName("body".getBytes()).get(0).text;
					para.from = from;
					para.type = Type.TYPE_TEXT;

					if (messageCB != null)
						messageCB.onMessage(para);
				}
			}
			else if (name.equals("iq"))
			{
				String id = new String(element.getAttributeByName("id".getBytes()).value, "UTF-8");
				System.out.println("IQ WITH ID " + id);
				if (id.equals("getstatus"))
				{
					WAStatusCallback callback = (WAStatusCallback) callbacks.poll();
					WAElement statusElement = element.getChildrenByName("status".getBytes()).get(0);
					WAElement userElement = statusElement.getChildrenByName("user".getBytes()).get(0);
					Date d = new Date(Integer.valueOf(new String(userElement.getAttributeByName("t".getBytes()).value, "UTF-8")) * 1000L);
					callback.onStatus(d, userElement.text);
				}
				else if (id.equals("lastseen"))
				{
					WALastSeenCallback callback = (WALastSeenCallback) callbacks.poll();
					WAElement queryElement = element.getChildrenByName("query".getBytes()).get(0);
					callback.onLastSeen(Integer.parseInt(new String(queryElement.getAttributeByName("seconds".getBytes()).value, "UTF-8")));
				}
				else if (id.equals("getgroups-participating"))
				{
					ArrayList<WAGroup> para = new ArrayList<WAGroup>();

					for (WAElement group : element.children)
					{
						WAGroup g = new WAGroup();
						g.id = new String(group.getAttributeByName("id".getBytes()).value, "UTF-8");
						g.owner = new String(group.getAttributeByName("owner".getBytes()).value, "UTF-8");
						g.owner = g.owner.substring(0, g.owner.indexOf('@'));
						g.subject = new String(group.getAttributeByName("subject".getBytes()).value, "UTF-8");
						g.creationDate = new Date(Long.parseLong(new String(group.getAttributeByName("creation".getBytes()).value, "UTF-8")) * 1000);
						para.add(g);
					}
					WAGroupCallback gc = (WAGroupCallback) callbacks.poll();
					if (gc != null)
						gc.onGetGroups(para);

				}
				else if (id.equals("getgroupparticipants"))
				{
					WAGroup group = (WAGroup) callbacks.poll();
					WAFillGroupCallback cb = (WAFillGroupCallback) callbacks.poll();
					for (WAElement participant : element.children)
					{
						group.participants.add(new String(participant.getAttributeByName("jid".getBytes()).name, "UTF-8"));
					}
					if (cb != null)
						cb.onFillGroup(group);
				}
				else if (xmlns != null && xmlns.equals("urn:xmpp:ping"))
				{
					client.write(WAElement.fromString("<iq to='s.whatsapp.net' id='" + id + "' type='result'/>"));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onConnectFailure()
	{
		WAConnectCallback cc = (WAConnectCallback) callbacks.poll();
		if (cc != null)
			cc.onConnectFailure(Reason.SOCKET_ERROR);
	}

	@Override
	public void onDisconnected()
	{
		if (disconnectCB != null)
			disconnectCB.onDisconnected();
	}

	public void close() throws IOException
	{
		client.close();
	}

}
