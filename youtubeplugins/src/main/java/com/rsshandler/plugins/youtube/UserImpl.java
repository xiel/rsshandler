package com.rsshandler.plugins.youtube;

import com.google.gdata.client.youtube.*;
import com.google.gdata.data.media.mediarss.*;
import com.google.gdata.data.youtube.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.io.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.util.logging.Logger;

public class UserImpl extends YoutubeBaseImpl { 
	Logger logger = Logger.getLogger(this.getClass().getName());
	
	public String getName() {
		return "YouTube-User";
	}
	
	public boolean isHandler(HttpServletRequest request) {
		String feedtype = request.getParameter("feedtype");
		String pth = request.getServletPath();
		
		if ((feedtype != null && feedtype.equals("user")) || pth.equals("/user.rss")) {
			return true;
		} else {
			return false;
		}
	}	
	
	public String transformContent(String content, HttpServletRequest request) {
		String cnt = content;

		cnt = replaceEnclosures(cnt, request);
		cnt = this.getFixups(cnt, request);
		return cnt;
	}
	
    public String getRssUrl(HttpServletRequest request) {
		String tp = getTimeApplicableParams(request);
	
		if (tp != null) {
			return String.format("http://gdata.youtube.com/feeds/base/users/%s/uploads?%s", request.getParameter("id"), tp);	
		} else {
			return String.format("http://gdata.youtube.com/feeds/base/users/%s/uploads", request.getParameter("id"));	
		}
	}
	
	private String getFixups(String transformed, HttpServletRequest request) {
		logger.info("Running UserServlet getFixups");

		String ret = transformed;

		try {
			URL url = new URL(String.format("http://gdata.youtube.com/feeds/api/users/%s", request.getParameter("id")));

			logger.info(String.format("Userservlet fix ups URL: %s", url));
			YouTubeService service = new YouTubeService(null);
			UserProfileEntry profileEntry = service.getEntry(url, UserProfileEntry.class);

			if (profileEntry == null) {
				return ret;
			}

			String t = profileEntry.getUsername();

			String newTitle = request.getParameter("title");
			if (newTitle != null) {
				t = newTitle;
			}

			logger.info("Username: " + t);

			MediaThumbnail logo = profileEntry.getThumbnail();
			String lurl = null;

			if (logo != null) {
				lurl = logo.getUrl();
				logger.info("logo url:" + lurl);
			}

			InputSource is = new InputSource(new StringReader(transformed));
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
		
		logger.info("Completed UserServlet getFixups");
		return ret;	
	}
}