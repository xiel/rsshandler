package com.rsshandler.plugins.youtube;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

public class StandardImpl extends YoutubeBaseImpl  {
	Logger logger = Logger.getLogger(this.getClass().getName());
	
	public String getName() {
		return "YouTube-Standard";
	}
	
	public boolean isHandler(HttpServletRequest request) {
		String feedtype = request.getParameter("feedtype");
		String pth = request.getServletPath();
		
		if ((feedtype != null && feedtype.equals("standard")) || pth.equals("/standard")) {
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
		String type = request.getParameter("id");
		String country = request.getParameter("country");
		String path = "http://gdata.youtube.com/feeds/api/standardfeeds/";
		if (country != null) {
			path += country+"/";
		}
		String tp = getTimeApplicableParams(request);
	
		if (tp != null) {			
			return path+type+"?"+tp;
		} else {
			return path+type;	
		}
	}
	
}