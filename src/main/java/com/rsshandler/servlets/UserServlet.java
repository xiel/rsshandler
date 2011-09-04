package com.rsshandler.servlets;

import com.google.gdata.client.*;
import com.google.gdata.client.youtube.*;
import com.google.gdata.data.*;
import com.google.gdata.data.geo.impl.*;
import com.google.gdata.data.media.*;
import com.google.gdata.data.media.mediarss.*;
import com.google.gdata.data.youtube.*;
import com.google.gdata.data.extensions.*;
import com.google.gdata.util.*;

import javax.servlet.http.HttpServletRequest;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import java.io.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;


import java.util.logging.Logger;

public class UserServlet extends RssServlet { 
    @Override
  protected String getPostFetchFixups(String str, String overridetitle, HttpServletRequest request) {
    logger.info("Running UserServlet post fetch fixups");
	
	String ret = str;
	
	try {
      URL url = new URL(String.format("http://gdata.youtube.com/feeds/api/users/%s", request.getParameter("id")));
      logger.info(String.format("Userservlet fix ups URL: %s", url));
	  YouTubeService service = new YouTubeService(null);
      UserProfileEntry profileEntry = service.getEntry(url, UserProfileEntry.class);
	  
	  if (profileEntry == null) {
	    return ret;
	  }
	  
	  String t = profileEntry.getUsername();
	  
	  if (overridetitle != null) {
		t = overridetitle;
	  }
	  
      logger.info("Username: " + t);
      
	  MediaThumbnail logo = profileEntry.getThumbnail();
	  String lurl = null;
	
	  if (logo != null) {
	    lurl = logo.getUrl();
	    logger.info("logo url:" + lurl);
	  }
	
      InputSource is = new InputSource(new StringReader(str));
	  Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
	  
	  // replace both title nodes for the channels
      if (t != null) {
	    XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList)xpath.evaluate("/rss/channel/title", doc, XPathConstants.NODESET);

        Node n = nodes.item(0);
	
	    if (n != null) {
	      n.setTextContent(t);
	    }
		
		nodes = (NodeList)xpath.evaluate("/rss/channel/image/title", doc, XPathConstants.NODESET);
        n = nodes.item(0);
	
	    if (n != null) {
	      n.setTextContent(t);
	    }
	  }
	  
      // replace the default youtube logo with the user specific one
      if (lurl != null) {
	    XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList)xpath.evaluate("/rss/channel/image/url", doc, XPathConstants.NODESET);

        Node n = nodes.item(0);
	
	    if (n != null) {
	      n.setTextContent(lurl);
	    }
      }

	  TransformerFactory trans = TransformerFactory.newInstance();
	  Transformer transformer = trans.newTransformer();
	  StringWriter writer = new StringWriter();
	  Result result = new StreamResult(writer);
	  javax.xml.transform.Source source = new javax.xml.transform.dom.DOMSource(doc);
	  transformer.transform(source, result);
	  writer.close();
	  ret = writer.toString();	  
    }
	catch (Exception ex) {
	  logger.info(ex.toString());
	}
	
	logger.info("Completed UserServlet post fetch fixups");
	return ret;
  }

	@Override
  protected String getRssUrl(HttpServletRequest request) {
	  return String.format("http://gdata.youtube.com/feeds/base/users/%s/uploads", request.getParameter("id"));
  }

}