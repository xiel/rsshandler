package com.rsshandler;

import java.util.Arrays;

import com.rsshandler.servlets.VideoServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import com.rsshandler.servlets.RssServlet;
import java.util.logging.Logger;
import com.rsshandler.RssHandlerPluginService;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.net.URL;

public class PodcastServerImpl implements PodcastServer {
	private Server server;
	private int port;
	private boolean proxyMode = false;
	private RssHandlerPluginService rhs = null;
	private  Logger logger = Logger.getLogger("PodcastServerImpl");

	public PodcastServerImpl() {

	}

	public boolean stop() {
		try {
			server.stop();
			server.destroy();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean start() {
		try {
			server = new Server(getPort());
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath("/");
			server.setHandler(context);
			
			String executionPath = System.getProperty("user.dir");
			ClassLoader cl = getClassloader(new File(executionPath + "\\plugins"));
			rhs = RssHandlerPluginService.getInstance();
			rhs.initPlugins(cl);

			// backwards compatible with rsshandler
			context.addServlet(new ServletHolder(new RssServlet()), "/user.rss");
			context.addServlet(new ServletHolder(new RssServlet()), "/playlist");
			context.addServlet(new ServletHolder(new RssServlet()), "/favorite");
			context.addServlet(new ServletHolder(new RssServlet()), "/subscriptions");
			context.addServlet(new ServletHolder(new RssServlet()), "/standard");			
			
			// new preferred URLs 
			context.addServlet(new ServletHolder(new RssServlet()), "/request.rss");
			context.addServlet(new ServletHolder(new VideoServlet(proxyMode)), "/video.mp4");

			logger.info("Starting jetty");
			this.server.start();
			logger.info("joining jetty to thread");
			this.server.join();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private ClassLoader getClassloader(File directory) throws IOException
	{
		URL[] uempty = new URL[0];
		logger.info("adding " + directory);
		ClassLoader classLoader = null;
		if (directory.exists())
		{
			File[] files = directory.listFiles();
			ArrayList<URL> flist = new ArrayList<URL>();
			if (files != null) {
				for(File f:files) {
					URL u = f.toURI().toURL();
					flist.add(u);
				}

				
				URL[] uls = flist.toArray(uempty);
				classLoader = new URLClassLoader(uls);

			}
		} else {
			logger.warning("The directory \"" + directory + "\" does not exist!");
		}
		return classLoader;
	}	
	
	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	/*public static void main(String args[]) throws IOException {
		int port = 8083;
		boolean proxyMode = false;

		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
		
		if (args.length > 1) {
			proxyMode = "true".equals(args[1]);
		}
		
		String executionPath = System.getProperty("user.dir");
		addDirToClasspath(new File(executionPath + "\\plugins"));
		
		PodcastServerImpl server = new PodcastServerImpl();
		server.setPort(port);
		server.setProxyMode(proxyMode);
		server.start();
	}*/

	public void setProxyMode(boolean proxyMode) {
		this.proxyMode = proxyMode;
	}

	public boolean isProxyMode() {
		return proxyMode;
	}
}
