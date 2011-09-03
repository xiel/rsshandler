package com.rsshandler;

public interface PodcastServer {

  boolean stop();

  boolean start();

	int getPort();

	void setPort(int parseInt);

	boolean isProxyMode();

	void setProxyMode(boolean selected);
}
