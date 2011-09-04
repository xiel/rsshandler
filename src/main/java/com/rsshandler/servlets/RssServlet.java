package com.rsshandler.servlets;

import com.rsshandler.Gui;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO add easy error report, configurable from GUI
public abstract class RssServlet extends HttpServlet {
	Logger logger = Logger.getLogger(this.getClass().getName());
	
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
  	CookieManager cookieManager = new CookieManager();
  	CookieHandler.setDefault(cookieManager);
  	cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_NONE);
  	List<String> headers = Collections.list(request.getHeaderNames());
  	for (String name : headers) {
  		logger.info(String.format("Header: %s, %s", name, request.getHeader(name)));
  	}
  	String host = request.getParameter("host");
    String port = request.getParameter("port");
    int format = Integer.parseInt(request.getParameter("format"));
    int fallback = 1; 
    if (request.getParameter("fb") != null) {
        fallback = Integer.parseInt(request.getParameter("fb"));
    }
    String size = request.getParameter("size");
    if (size == null) {
      size = "25";
    }
    String orderby = request.getParameter("orderby");
    if (orderby == null) {
      orderby = "published";
    }
    String parameters = String.format("?alt=rss&v=2&max-results=%s&orderby=%s", size, orderby);
    if (this.isTimeApplicable(request)) {
      String period = request.getParameter("period");
      if (period == null) {
        period = "all_time";
      }
      parameters += "&time="+period;
    }
    URL url = new URL(getRssUrl(request)+parameters);
    logger.info(String.format("RSS URL: %s", url));
    URLConnection connection = url.openConnection();
    logger.info(String.format("Request properties: %s", connection.getRequestProperties()));
    String str = Utils.readString(connection.getInputStream());
    logger.info(String.format("Headers: %s", connection.getHeaderFields()));
    str = replaceEnclosures(str, format, host, port, "true".equals(request.getParameter("removeDescription")), "true".equals(request.getParameter("removeTitle")), fallback);
//    logger.info(str);

    String newTitle = request.getParameter("title");

	String poststr = getPostFetchFixups(str, newTitle, request);
    response.setContentType("application/rss+xml; charset=UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().write(poststr);
  }

  protected boolean isTimeApplicable(HttpServletRequest request) {
    return false;
  }

  protected String getPostFetchFixups(String s, String overridetitle, HttpServletRequest request) {
    logger.info("Running default post fetch fixups");
	String ret = s;
	
	if (overridetitle != null && s != null) {
		ret = ret.replaceAll("</url><title>.*?</title>","</url><title>" + overridetitle + "</title>");
		ret = ret.replaceAll("</lastBuildDate><title>.*?</title>","</lastBuildDate><title>" + overridetitle + "</title>");
 	}
	
	return ret;
  }
  
  protected abstract String getRssUrl(HttpServletRequest request);

//TODO try to implement normal XML parsing and element insertion, this way it should be more stable
  String replaceEnclosures(String str, int format, String host, String port, boolean removeDescription, boolean removeTitle, int fallback) {
    Pattern pattern = Pattern.compile("<link>http\\://www.youtube.com/watch\\?v=(.+?)</link><author>");
    Matcher matcher = pattern.matcher(str);
    String formattype = "video/mp4";
    if (format == Gui.FLV) {
      formattype = "video/x-flv";
    }
    while (matcher.find()) {
      String link = matcher.group(1);
      logger.info(String.format("Video id: %s", link));
      String oldenclosure = String.format("%s</link><author>", link);
      String newenclosure = String.format("%s</link><enclosure url=\"http://"+host+":"+port+"/video.mp4?format=%s&amp;id=%s&amp;fb=%s\" length=\"35\" type=\"%s\"/><author>", link, format, link, fallback, formattype);
      str = str.replace(oldenclosure, newenclosure);
    }
    if (removeTitle) {
      Pattern desc = Pattern.compile("<title>.*?</title>", Pattern.DOTALL);
      Matcher descmatcher = desc.matcher(str);
      str = descmatcher.replaceAll("<title></title>");
    }
    if (removeDescription) {
      Pattern desc = Pattern.compile("<description>.*?</description>", Pattern.DOTALL);
      Matcher descmatcher = desc.matcher(str);
      str = descmatcher.replaceAll("<description></description>");
      Pattern mediadesc = Pattern.compile("<media\\:description type\\='plain'>.*?</media\\:description>", Pattern.DOTALL);
      Matcher mediadescmatcher = mediadesc.matcher(str);
      str = mediadescmatcher.replaceAll("<media:description type='plain'></media:description>");
    }
    Pattern mediagroup = Pattern.compile("<media:group>.*?</media:group>", Pattern.DOTALL);
    Matcher mediagroupmatcher = mediagroup.matcher(str);
    str = mediagroupmatcher.replaceAll("");
    str = str.replaceAll("<category.*?>.*?</category>", "");
    return str;
  }

}