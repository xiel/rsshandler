package com.rsshandler.interfaces;

import javax.servlet.http.HttpServletRequest;

public interface RssHandlerFeedPlugin {
    public void init();
	public String getVersion();
	public String getInfo();
	public String getName();
	public boolean isHandler(HttpServletRequest request);
	public String transformContent(String content, HttpServletRequest request);
    public String getRssUrl(HttpServletRequest request);
    public void unload();
}