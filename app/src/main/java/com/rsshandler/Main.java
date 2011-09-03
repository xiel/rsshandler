package com.rsshandler;

import javax.swing.UIManager;

import java.util.logging.Logger;


public class Main {
	private static Logger logger = Logger.getLogger("Main");

	public static void main(String[] args) throws Exception {
		PodcastServerImpl server = new PodcastServerImpl();

		for(String s: args) {
			logger.info("command line arg: " + s);
		}
		
		if (args[0].equals( "console")) {
			int port = 8083;
			boolean proxyMode = false;

			if (args.length > 1) {
				port = Integer.parseInt(args[1]);
			}

			if (args.length > 2) {
				proxyMode = "true".equals(args[2]);
			}

			server.setPort(port);
			server.setProxyMode(proxyMode);
			server.start();
		} else {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch (Exception e) {
				// ignore and move on
				e.printStackTrace();
			}
			Gui gui = new Gui();
			gui.setServer((PodcastServer) server);
			gui.createGui();
			gui.startServer();
		}
	}

}
