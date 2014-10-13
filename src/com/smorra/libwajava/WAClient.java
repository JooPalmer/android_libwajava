package com.smorra.libwajava;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.smorra.libwajava.WAMessage.Type;
import com.smorra.libwajava.callbacks.WAAddParticipantsCallback;
import com.smorra.libwajava.callbacks.WAConnectCallback;
import com.smorra.libwajava.callbacks.WACreateGroupCallback;
import com.smorra.libwajava.callbacks.WADisconnectCallback;
import com.smorra.libwajava.callbacks.WAFillGroupCallback;
import com.smorra.libwajava.callbacks.WAGroupCallback;
import com.smorra.libwajava.callbacks.WAIqCallback;
import com.smorra.libwajava.callbacks.WALastSeenCallback;
import com.smorra.libwajava.callbacks.WAMessageCallback;
import com.smorra.libwajava.callbacks.WAProfilePictureCallback;
import com.smorra.libwajava.callbacks.WARawCallback;
import com.smorra.libwajava.callbacks.WAReceiptCallback;
import com.smorra.libwajava.callbacks.WASetSubjectCallback;
import com.smorra.libwajava.callbacks.WAStatusCallback;
import com.smorra.libwajava.callbacks.WAConnectCallback.Reason;
import com.smorra.libwajava.callbacks.WASyncContactsCallback;

import android.util.Base64;
import android.util.Pair;

public class WAClient implements WARawCallback
{
	WAClientRaw client;
	String phoneNumber;
	String password;
	String displayName;
	WADisconnectCallback disconnectCB;
	WAMessageCallback messageCB;
	WAReceiptCallback receiptCB;
	WAConnectCallback connectCallback;
	HashMap<String, Object> cbs = new HashMap<String, Object>();
	boolean autoReceipt = true;
	BigInteger messageIdGenerator = BigInteger.ZERO;

	public String generateId(String prefix)
	{
		for (int i = 0;; i++)
			if (!cbs.containsKey(prefix + i))
				return prefix + i;
	}

	public void setAutoReceipt(boolean autoReceipt)
	{
		this.autoReceipt = autoReceipt;
	}

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
		connectCallback = callback;
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
		String id = generateId("getgroups-");
		cbs.put(id, callback);
		client.write(WAElement.fromString("<iq id='" + id + "' type='get' xmlns='w:g' to='g.us'><list type='participating'></list></iq>"));
	}
	
	
	/**
	 * Requests a contacts sync by sending the contacts' <b>numbers</b> to the WhatsApp server, which responds with the numbers 
	 * it knows, e.g. that have a WhatsApp account.
	 * @param phoneNumber The user's phonenumber in international format <b>excluding</b> '+'. There exists a version of this method 
	 * that uses the phonenumber supplied to WAClient in its construct.
	 * @param numbers The contacts' phonenumbers to be checked. The numbers need to be in international format <b>including</b> '+'. 
	 * If the number does not start with a '+', this method will simply add a '+' to the number. However, this is very primitive and should not be relied upon. 
	 * On Android, internationally formatted numbers can be retrieved from the <i>ContactsContract.Data</i> table. Look for values in the column <i>ContactsContract.Data.DATA4</i>
	 * WHERE <i>ContactsContract.Data.MIMETYPE = Phone.CONTENT_ITEM_TYPE</i> (<i>"mimetype" = "vnd.android.cursor.item/phone_v2"</i>). 
	 * @param mode Either "full" or "delta".
	 * @param context Either "registration" or "background";
	 * @param index Default = 0.
	 * @param last Default = true.
	 * @param callback
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public void syncContacts(String phoneNumber, ArrayList<String> numbers, String mode, String context, int index, boolean last, WASyncContactsCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException{
		String id = generateId("sendsync_");
		String cmd = "<iq id='" + id + "' type='get' xmlns='urn:xmpp:whatsapp:sync' to='" + phoneNumber + "@s.whatsapp.net'>";
		cmd += "<sync mode='" + mode + "' context='" + context + "' sid='" + (((System.currentTimeMillis()/1000)+11644477200L)*10000000) + "' index='" + index + "' last='" + String.valueOf(last) + "'>";
		for (String number : numbers)
		{
			if(!number.startsWith("+")){
				number = "+" + number;
			}
			//cmd += "<user='" + WAUtil.xmlEncode(contact) + "'/>";
			//cmd += "<user '" + number + "'/>";
			cmd += "<user>" + number + "</user>";
		}
		cmd += "</sync></iq>";
		cbs.put(id, callback);
		client.write(WAElement.fromString(cmd));
	}
	public void syncContacts(ArrayList<String> contacts, String mode, String context, int index, boolean last, WASyncContactsCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException{
		syncContacts(phoneNumber,contacts,mode,context,index,last,callback);
	}	
	public void syncContacts(ArrayList<String> contacts, WASyncContactsCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException{
		syncContacts(phoneNumber, contacts, "full", "registration", 0, true, callback);
	}	
	public void syncContacts(String phoneNumber, ArrayList<String> contacts, WASyncContactsCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException{
		syncContacts(phoneNumber, contacts, "full", "registration", 0, true, callback);
	}
	

	public void fillGroup(WAGroup group, WAFillGroupCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		String id = generateId("fillgroup-");
		cbs.put(id, Pair.create(group, callback));
		String str = "<iq id='" + id + "' type='get' xmlns='w:g' to='" + WAUtil.xmlEncode(group.id) + "@g.us'><list/></iq>";
		client.write(WAElement.fromString(str));
	}

	public void setStatus(String status, WAIqCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		String id = generateId("setstatus-");
		cbs.put(id, callback);
		client.write(WAElement.fromString("<iq to='s.whatsapp.net' type='set' id='" + id + "' xmlns='status'><status>" + WAUtil.xmlEncode(status) + "</status></iq>"));
	}

	public void addParticipants(String groupId, String[] participants, WAAddParticipantsCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		String id = generateId("addparticipants-");
		String cmd = "<iq id='" + id + "' type='set' xmlns='w:g' to='" + WAUtil.xmlEncode(groupId) + "@g.us'>";
		cmd += "<add>";
		for (String participant : participants)
		{
			cmd += "<participant jid='" + WAUtil.xmlEncode(participant) + "@s.whatsapp.net'/>";
		}
		cmd += "</add></iq>";
		cbs.put(id, Pair.create(callback, participants));
		client.write(WAElement.fromString(cmd));
	}

	public void test() throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		String cmd = "<message to='491604523088@s.whatsapp.net' type='media' id='message-1412430970-3' t='1412430970'>";
		cmd += "<x xmlns='jabber:x:event'>";
		cmd += "<server></server>";
		cmd += "</x>";
		cmd += "<notify xmlns='urn:xmpp:whatsapp' name='Stefan Smorra'></notify>";
		cmd += "<request xmlns='urn:xmpp:receipts'></request>";
		cmd += "<media xmlns='urn:xmpp:whatsapp:mms' type='image' url='https://mmi208.whatsapp.net/d/Ags8Skz7gXmSFU0p-Pg-RmugAB-qBhUjQNJbB2CamOsN.jpg' file='3de71cfa038ff51b5058cfd5447fd0f2.jpeg' size='10803' hash='HPeySsiC0lmgPxhfJwZ0ZQEXlojZ8dcFJt795oMHKU4='> 12024 byte data</media>";
		cmd += "</message>";
		client.write(WAElement.fromString(cmd));

	}

	public void createGroup(String subject, WACreateGroupCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		String id = generateId("creategroup-");
		String cmd = "<iq id='" + id + "' type='set' xmlns='w:g' to='g.us'>";
		cmd += "<group action='create' subject='" + WAUtil.xmlEncode(subject) + "'></group>";
		cmd += "</iq>";
		client.write(WAElement.fromString(cmd));
		cbs.put(id, callback);
	}

	public void getProfilePicture(String phoneNumber, boolean large, WAProfilePictureCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		String id = generateId("getprofilepicture-");
		String cmd = "<iq id='" + id + "' type='get' xmlns='w:profile:picture' to='" + phoneNumber + "'@s.whatsapp.net'>";
		cmd += "<picture type='" + (large ? "image" : "preview") + "'/>";
		cmd += "</iq>";
		client.write(WAElement.fromString(cmd));
		cbs.put(id, callback);
	}

	public void sendActive() throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		client.write(WAElement.fromString("<presence type='active'/>"));
	}

	public void subscribePresence(String phoneNumber) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		client.write(WAElement.fromString("<presence type='subscribe' to='" + WAUtil.xmlEncode(phoneNumber) + "@s.whatsapp.net'/>"));
	}

	public void getLastSeen(String phoneNumber, WALastSeenCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		String id = generateId("getlastseen-");
		cbs.put(id, callback);
		client.write(WAElement.fromString("<iq to='" + WAUtil.xmlEncode(phoneNumber) + "@s.whatsapp.net' type='get' id='" + id + "' xmlns='jabber:iq:last'><query/></iq>"));
	}

	public void setSubject(String gid, String subject, WASetSubjectCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		String id = generateId("setsubject-");
		String cmd = "<iq id='" + id + "' type='set' to='" + WAUtil.xmlEncode(gid) + "@g.us' xmlns='w:g'>";
		cmd += "<subject value='" + WAUtil.xmlEncode(subject) + "'/>";
		cmd += "</iq>";
		cbs.put(id, callback);
		client.write(WAElement.fromString(cmd));
	}

	public void getStatus(String phoneNumber, WAStatusCallback callback) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		String id = generateId("getstatus-");
		cbs.put(id, callback);
		String str = "<iq to='s.whatsapp.net' type='get' xmlns='status' id='" + id + "'>";
		str += "<status><user jid='" + WAUtil.xmlEncode(phoneNumber) + "@s.whatsapp.net'/></status>";
		str += "</iq>";
		client.write(WAElement.fromString(str));
	}

	public void sendTyping(String phoneNumber) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		client.write(WAElement.fromString("<chatstate to='" + WAUtil.xmlEncode(phoneNumber) + "@s.whatsapp.net'><composing/></chatstate>"));
	}
	
	public void sendTypingPaused(String phoneNumber) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException{
		client.write(WAElement.fromString("<chatstate to='" + WAUtil.xmlEncode(phoneNumber) + "@s.whatsapp.net'><paused/></chatstate>"));
	}

	public String sendMessage(String to, String body) throws InvalidKeyException, NoSuchAlgorithmException, IOException, InterruptedException, SAXException, ParserConfigurationException
	{
		long unixTime = System.currentTimeMillis() / 1000L;
		String id = "message-" + unixTime + "-" + messageIdGenerator;
		String str = "<message to='" + WAUtil.xmlEncode(to) + "@s.whatsapp.net' type='text' id='" + id + "' t='" + unixTime + "'>";
		str += "<x xmlns='jabber:x:event'><server/></x>";
		str += "<notify xmlns='urn:xmpp:whatsapp' name='" + WAUtil.xmlEncode(displayName) + "'/>";
		str += "<request xmlns='urn:xmpp:receipts'/>";
		str += "<body>" + WAUtil.xmlEncode(body) + "</body>";
		str += "</message>";
		client.write(WAElement.fromString(str));
		messageIdGenerator = messageIdGenerator.add(BigInteger.ONE);
		return id;

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
				client.sendResponse(Base64.decode(password, Base64.DEFAULT), element.text, phoneNumber.getBytes());
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
				String str = "<presence name='" + WAUtil.xmlEncode(displayName) + "'/>";
				client.write(WAElement.fromString(str));
				connectCallback.onConnectSuccess();

			}
			else if (name.equals("failure"))
			{
				WAElement child = element.children.get(0);
				String childName = new String(child.name, "UTF-8");
				if (childName.equals("not-authorized"))
				{
					connectCallback.onConnectFailure(Reason.NOT_AUTHORIZED);
					close();
				}
			}
			else if (name.equals("message"))
			{
				String id = new String(element.getAttributeByName("id".getBytes()).value, "UTF-8");
				String from = new String(element.getAttributeByName("from".getBytes()).value, "UTF-8");
				from = from.substring(0, from.indexOf('@'));
				if (autoReceipt)
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
				String type = new String(element.getAttributeByName("type".getBytes()).value, "UTF-8");
				System.out.println("IQ WITH ID " + id);

				if (id.startsWith("getstatus-"))
				{
					WAStatusCallback callback = (WAStatusCallback) cbs.remove(id);
					WAElement statusElement = element.getChildrenByName("status".getBytes()).get(0);
					WAElement userElement = statusElement.getChildrenByName("user".getBytes()).get(0);
					Date d = new Date(Integer.valueOf(new String(userElement.getAttributeByName("t".getBytes()).value, "UTF-8")) * 1000L);
					callback.onStatus(d, new String(userElement.text, "UTF-8"));
				}
				else if (id.startsWith("getprofilepicture-"))
				{
					WAProfilePictureCallback callback = (WAProfilePictureCallback) cbs.remove(id);
					if (type.equals("error"))
					{
						callback.onProfilePictureError();
					}
					else
					{
						WAElement pictureElement = element.getChildrenByName("picture".getBytes()).get(0);
						callback.onProfilePictureSuccess(pictureElement.text);
					}
				}
				else if (id.startsWith("setsubject-"))
				{
					WASetSubjectCallback callback = (WASetSubjectCallback) cbs.remove(id);
					if (callback != null)
						callback.onSetSubjectSuccess();
				}
				else if (id.startsWith("addparticipants-"))
				{
					Pair<?, ?> pair = (Pair<?, ?>) cbs.remove(id);
					WAAddParticipantsCallback callback = (WAAddParticipantsCallback) pair.first;
					String[] participants = (String[]) pair.second;
					if (type.equals("error"))
					{
						if (callback != null)
							callback.onAddParticipantsError();
					}
					else
					{
						ArrayList<WAElement> adds = element.getChildrenByName("add".getBytes());
						boolean[] result = new boolean[participants.length];
						int pos = 0;
						for (String participant : participants)
						{
							boolean found = false;
							for (WAElement add : adds)
							{
								String phoneNumber = new String(add.getAttributeByName("participant".getBytes()).value, "UTF-8");
								phoneNumber = phoneNumber.substring(0, phoneNumber.indexOf('@'));
								String type2 = new String(add.getAttributeByName("type".getBytes()).value, "UTF-8");
								if (phoneNumber.equals(participant))
								{
									found = true;
									result[pos] = type2.equals("success");
									pos++;
									break;
								}
							}
							if (!found)
							{
								result[pos] = false;
								pos++;
							}
						}
						if (callback != null)
							callback.onAddParticipantsSuccess(result);
					}
				}
				else if (id.startsWith("creategroup-"))
				{
					WACreateGroupCallback callback = (WACreateGroupCallback) cbs.remove(id);
					WAElement groupElement = element.getChildrenByName("group".getBytes()).get(0);
					if (callback != null)
						callback.onCreateGroupSuccess(new String(groupElement.getAttributeByName("id".getBytes()).value, "UTF-8"));
				}
				else if (id.startsWith("setstatus-"))
				{
					WAIqCallback callback = (WAIqCallback) cbs.remove(id);
					if (callback != null)
						callback.onSuccess();
				}
				else if (id.startsWith("getlastseen-"))
				{
					WALastSeenCallback callback = (WALastSeenCallback) cbs.remove(id);
					if (callback != null)
					{
						if (type.equals("error"))
						{
							callback.onLastSeenError();
						}
						else
						{
							WAElement queryElement = element.getChildrenByName("query".getBytes()).get(0);
							callback.onLastSeenSuccess(Integer.parseInt(new String(queryElement.getAttributeByName("seconds".getBytes()).value, "UTF-8")));
						}
					}
				}
				else if (id.startsWith("getgroups-"))
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
					WAGroupCallback gc = (WAGroupCallback) cbs.remove(id);
					if (gc != null)
						gc.onGetGroups(para);

				}
				else if (id.startsWith("fillgroup-"))
				{
					Pair<?, ?> pair = (Pair<?, ?>) cbs.remove(id);
					WAGroup group = (WAGroup) pair.first;
					WAFillGroupCallback cb = (WAFillGroupCallback) pair.second;

					for (WAElement participant : element.children)
					{
						group.participants.add(new String(participant.getAttributeByName("jid".getBytes()).name, "UTF-8"));
					}
					if (cb != null)
						cb.onFillGroup(group);
				}
				else if (id.startsWith("sendsync_")){
					//TODO
					WAElement syncNode = element.getChildrenByName("sync".getBytes()).get(0);				
					ArrayList<WAElement> existing = new ArrayList<WAElement>();
					ArrayList<WAElement> nonexisting = new ArrayList<WAElement>();
					ArrayList<WAElement> invalid = new ArrayList<WAElement>();
					
					ArrayList<WAElement> childIn = syncNode.getChildrenByName("in".getBytes());
					if(childIn.size()>0){
						// Some contacts have WhatsApp.
						WAElement existingNode = childIn.get(0);
						existing = existingNode.getChildrenByName("user".getBytes());
					}
					ArrayList<WAElement> childOut = syncNode.getChildrenByName("out".getBytes());
					if(childOut.size()>0){
						// Some contacts have no WhatsApp
						WAElement nonexistingNode = childOut.get(0);
						nonexisting = nonexistingNode.getChildrenByName("user".getBytes());
					}
					ArrayList<WAElement> childInvalid = syncNode.getChildrenByName("invalid".getBytes());
					if(childInvalid.size()>0){
						// Some numbers were invalid
						WAElement invalidNode = childInvalid.get(0);
						invalid = invalidNode.getChildrenByName("user".getBytes());
					}
					
					WASyncContactsCallback scc = (WASyncContactsCallback) cbs.remove(id);					
					scc.onSyncContacts(existing,nonexisting,invalid);
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
		if (connectCallback != null)
			connectCallback.onConnectFailure(Reason.SOCKET_ERROR);
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
