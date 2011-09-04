package com.rsshandler.servlets;

import javax.servlet.http.HttpServletRequest;

public class StandardServlet extends RssServlet {

	@Override
  protected String getRssUrl(HttpServletRequest request) {
	  String type = request.getParameter("id");
	  String country = request.getParameter("country");
	  String path = "http://gdata.youtube.com/feeds/api/standardfeeds/";
	  if (country != null) {
	    path += country+"/";
	  }
	  return path+type;
	}

  @Override
  protected boolean isTimeApplicable(HttpServletRequest request) {
    String type = request.getParameter("id");
    if (type.equals("top_rated") || type.equals("top_favorites") || type.equals("most_viewed") || type.equals("most_popular") || type.equals("most_discussed") || type.equals("most_responded")) {
      return true;
    }
    return false;
  }
	
}