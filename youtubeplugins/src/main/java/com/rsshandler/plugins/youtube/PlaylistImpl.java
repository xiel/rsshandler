package com.rsshandler.plugins.youtube;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

public class PlaylistImpl extends YoutubeBaseImpl  {
	Logger logger = Logger.getLogger(this.getClass().getName());

	public String getName() {
		return "YouTube-Playlist";
	}
	
	public boolean isHandler(HttpServletRequest request) {
		String feedtype = request.getParameter("feedtype");
		String pth = request.getServletPath();
		
		if ((feedtype != null && feedtype.equals("playlist")) || pth.equals("/playlist")) {
			return true;
		} else {
			return false;
		}

	}	
	
	public String transformContent(String content, HttpServletRequest request) {
		String cnt = content;
		cnt = replaceEnclosures(cnt, request);
		return cnt;
	}
	
	public String getRssUrl(HttpServletRequest request) {
		String tp = getTimeApplicableParams(request);
	
		if (tp != null) {	
			return String.format("http://gdata.youtube.com/feeds/api/playlists/%s?%s", request.getParameter("id"),tp);
		} else {
			return String.format("http://gdata.youtube.com/feeds/api/playlists/%s", request.getParameter("id"));
		}
	}
}