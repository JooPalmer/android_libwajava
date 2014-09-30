package com.smorra.libwajava;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class WAElement
{
	public byte[] text;
	public byte[] name;
	public ArrayList<WAElement> children = new ArrayList<WAElement>();
	public ArrayList<WAAttribute> attributes = new ArrayList<WAAttribute>();

	public WAElement(byte[] name)
	{
		this.name = name;
	}

	public byte[] serialize()
	{
		byte[] ret = new byte[] { '<' };
		ret = WAUtil.concat(ret, name);
		for (WAAttribute attr : attributes)
		{
			ret = WAUtil.concat(ret, new byte[] { ' ' });
			ret = WAUtil.concat(ret, attr.name);
			ret = WAUtil.concat(ret, new byte[] { '=', '\'' });
			ret = WAUtil.concat(ret, WAUtil.xmlEncode(attr.value));
			ret = WAUtil.concat(ret, new byte[] { '\'' });
		}
		if (text == null && children.size() == 0)
		{
			ret = WAUtil.concat(ret, new byte[] { '/', '>' });
			return ret;
		}
		ret = WAUtil.concat(ret, new byte[] { '>' });
		if (text != null)
			ret = WAUtil.concat(ret, WAUtil.xmlEncode(text));
		else if (children.size() != 0)
			for (WAElement child : children)
				ret = WAUtil.concat(ret, child.serialize());
		ret = WAUtil.concat(ret, new byte[] { '<', '/' });
		ret = WAUtil.concat(ret, name);
		ret = WAUtil.concat(ret, new byte[] { '>' });
		return ret;
	}

	public static WAElement fromString(String xml) throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(xml));
		Document doc = db.parse(is);
		return fromDomElement((Element) doc.getFirstChild());
	}

	public static WAElement fromDomElement(Element e)
	{
		WAElement ret = new WAElement(e.getNodeName().getBytes());
		for (int i = 0; i < e.getAttributes().getLength(); i++)
		{
			Node attr = e.getAttributes().item(i);
			ret.attributes.add(new WAAttribute(attr.getNodeName().getBytes(), attr.getNodeValue().getBytes()));
		}
		Node child = e.getFirstChild();
		if (child == null)
			return ret;
		if (child.getNodeType() == Node.TEXT_NODE)
			ret.text = child.getTextContent().getBytes();
		else if (child.getNodeType() == Node.ELEMENT_NODE)
		{
			NodeList children = e.getChildNodes();
			for (int i = 0; i < children.getLength(); i++)
				ret.children.add(fromDomElement((Element) children.item(i)));
		}
		return ret;
	}

	public WAAttribute getAttributeByName(byte[] name)
	{
		for (WAAttribute attr : attributes)
		{
			if (Arrays.equals(attr.name, name))
				return attr;
		}
		return null;
	}

	public ArrayList<WAElement> getChildrenByName(byte[] name)
	{
		ArrayList<WAElement> ret = new ArrayList<WAElement>();
		for (WAElement e : children)
		{
			if (Arrays.equals(e.name, name))
				ret.add(e);
		}
		return ret;
	}

}
