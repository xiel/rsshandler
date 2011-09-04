package com.rsshandler.servlets;

import javax.servlet.http.HttpServletRequest;

public class SubscriptionsServlet extends RssServlet {

	@Override
  protected String getRssUrl(HttpServletRequest request) {
		return String.format("http://gdata.youtube.com/feeds/base/users/%s/newsubscriptionvideos", request.getParameter("id"));
  }
}