package com.rsshandler.servlets;

import com.rsshandler.Gui;
import com.rsshandler.interfaces.RssHandlerFeedPlugin;
import com.rsshandler.RssHandlerPluginService;
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
import java.util.Iterator;

public class RssServlet extends HttpServlet {
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
		int format  = Integer.parseInt(request.getParameter("format"));
		
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

		RssHandlerPluginService rhs = RssHandlerPluginService.getInstance();
		RssHandlerFeedPlugin owner = null;
		
		Iterator<RssHandlerFeedPlugin> iterator = rhs.getPlugins();

        while(iterator.hasNext())
        {
            RssHandlerFeedPlugin plugin = iterator.next();
            logger.info("Interrogating plugin: " + plugin.getName());
			
			if (plugin.isHandler(request) == true) {
				owner = plugin;
				break;
			}
        }		

		if (owner == null) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().write("Failed to find plugin");
			return;
		}	
	
		String parameters = String.format("alt=rss&v=2&max-results=%s&orderby=%s", size, orderby);
		String rssurl = owner.getRssUrl(request);
		
		if (rssurl.indexOf("?") > -1) {
			rssurl = rssurl + "&" + parameters;
		} else {
			rssurl = rssurl + "?" + parameters;
		}
		
		URL url = new URL(rssurl);

		logger.info(String.format("RSS URL: %s", url));
		URLConnection connection = url.openConnection();
		logger.info(String.format("Request properties: %s", connection.getRequestProperties()));
		String str = Utils.readString(connection.getInputStream());
		logger.info(String.format("Headers: %s", connection.getHeaderFields()));

		str = owner.transformContent(str, request);

		response.setContentType("application/rss+xml; charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write(str);
	}

  /*protected String getPostFetchFixups(String s, String overridetitle, HttpServletRequest request) {
    logger.info("Running default post fetch fixups");
	String ret = s;
	
	if (overridetitle != null && s != null) {
		ret = ret.replaceAll("</url><title>.*?</title>","</url><title>" + overridetitle + "</title>");
		ret = ret.replaceAll("</lastBuildDate><title>.*?</title>","</lastBuildDate><title>" + overridetitle + "</title>");
 	}
	
	return ret;
  }
  */


}