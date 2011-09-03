package com.rsshandler.plugins.youtube;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;
import com.rsshandler.interfaces.RssHandlerFeedPlugin;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class YoutubeBaseImpl implements RssHandlerFeedPlugin  {
	Logger logger = Logger.getLogger(this.getClass().getName());
	
	// this is for authenticated requests such as private playlists like "Watch Later"
	//private String user = null;
	//private String pass = null;
	
	public void init() {
		logger.info(this.getName() + "[" + this.getVersion() + "] plugin initializing");
	}
		
	public String getVersion() {
		return "0.1";
	}
	
	public String getInfo() {
		return "Author: Jerome Kuptz";
	}
		
	public boolean isHandler(HttpServletRequest request) {
		return false;
	}	
	
	public void unload() {
		logger.info(this.getName() + "[" + this.getVersion() + "] plugin unloading");
	}	

	protected String getTimeApplicableParams(HttpServletRequest request) {
		String parameters = null;
		
		String type = request.getParameter("id");
		if (type.equals("top_rated") 
			|| type.equals("top_favorites")
			|| type.equals("most_viewed") 
			|| type.equals("most_popular") 
			|| type.equals("most_discussed") 
			|| type.equals("most_responded")) {

			String period = request.getParameter("period");
			if (period == null) {
				period = "all_time";
			}
			parameters += "&time="+period;
		}
		
		return parameters;
	}	
	
	protected String replaceEnclosures(String str, HttpServletRequest request) {
		String host = request.getParameter("host");
		String port = request.getParameter("port");
		int format = Integer.parseInt(request.getParameter("format"));
		int fallback = 1; 

		if (request.getParameter("fb") != null) {
			fallback = Integer.parseInt(request.getParameter("fb"));
		}

		boolean removeDescription = "true".equals(request.getParameter("removeDescription"));
		boolean removeTitle = "true".equals(request.getParameter("removeTitle"));		
		
		Pattern pattern = Pattern.compile("<link>http\\://www.youtube.com/watch\\?v=(.+?)</link><author>");
		Matcher matcher = pattern.matcher(str);
		String formattype = "video/mp4";
		
		if (format == 35) {
			formattype = "video/x-flv";
		}
		
		while (matcher.find()) {
			String link = matcher.group(1);
			logger.info(String.format("Video id: %s", link));
			String oldenclosure = String.format("%s</link><author>", link);
			String newenclosure = String.format("%s</link><enclosure url=\"http://"+host+":"+port+
					"/video.mp4?format=%s&amp;id=%s&amp;fb=%s\" length=\"35\" type=\"%s\"/><author>", 
					link, format, link, fallback, formattype);
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