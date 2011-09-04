package com.rsshandler.servlets;

import javax.servlet.http.HttpServletRequest;

public class PlaylistServlet extends RssServlet {

	@Override
  protected String getRssUrl(HttpServletRequest request) {
	  return String.format("http://gdata.youtube.com/feeds/api/playlists/%s", request.getParameter("id"));
  }
}