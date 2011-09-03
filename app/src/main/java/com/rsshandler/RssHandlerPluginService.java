package com.rsshandler;

import com.rsshandler.interfaces.RssHandlerFeedPlugin;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Logger;


public class RssHandlerPluginService 
{
	private static volatile RssHandlerPluginService pluginService;
	private ServiceLoader<RssHandlerFeedPlugin> serviceLoader;
	private Logger logger = Logger.getLogger(getClass().getName());
	
	private RssHandlerPluginService()
	{

	}
		
	public static RssHandlerPluginService getInstance()
	{
		if(pluginService == null)
		{
			pluginService = new RssHandlerPluginService();
		}
		return pluginService;
	}

	public Iterator<RssHandlerFeedPlugin> getPlugins()
	{
		return serviceLoader.iterator();
	}
	
	public void initPlugins(ClassLoader cl)
	{
		serviceLoader = ServiceLoader.load(RssHandlerFeedPlugin.class, cl);
		Iterator<RssHandlerFeedPlugin> iterator = getPlugins();
		
		if(!iterator.hasNext())
		{
			logger.info("No plugins were found!");
		}
		
		while(iterator.hasNext())
		{
			RssHandlerFeedPlugin plugin = iterator.next();
			logger.info("Initializing the plugin " + plugin.getName());
			plugin.init();
		}
	}
}