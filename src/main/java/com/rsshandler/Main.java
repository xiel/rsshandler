package com.rsshandler;

import javax.swing.UIManager;

public class Main {

  public static void main(String[] args) throws Exception {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      // ignore and move on
      e.printStackTrace();
    }
    PodcastServer server = new PodcastServerImpl();
    Gui gui = new Gui();
    gui.setServer(server);
    gui.createGui();
    gui.startServer();
  }
}
