package com.rsshandler;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.SystemTray;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.prefs.Preferences;
import java.net.URL;

public class Gui implements ClipboardOwner {
  private PodcastServer server;
  private JMenuItem startServer;
  private JMenuItem stopServer;
  private JFrame frame;
  private SystemTray sysTray;
  public static final int FLV = 35;
  private int port = -1;
  private String rsshostname = null;
  private boolean proxyMode = false;
  private static final String PROXY_MODE_PREF = "proxyMode";
  private static final String PORT_PREF = "port";
  private static final String RSS_SERVER_PREF = "rssserver";
  
  private TrayIcon trayIcon;
  public void setServer(PodcastServer server) {
    this.server = server;
  }

  public void restoreFromTray() {
	frame.setState( Frame.NORMAL );
    frame.setVisible(true);
	sysTray.remove(trayIcon);
  }
  
  public void sendToTray() {
    frame.setVisible(false);
    try {
      sysTray.add(trayIcon);
    } catch (AWTException e) {
      System.err.println("Failed to minimize to system tray");
	  e.printStackTrace();
    }	
  }
  
  public void createGui() {
    frame = new JFrame("RSS Handler");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setJMenuBar(createMenu());
    frame.getContentPane().add(createGeneratorPanel(), BorderLayout.CENTER);
    frame.pack();
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    proxyMode = prefs.getBoolean(PROXY_MODE_PREF, false);
    port = prefs.getInt(PORT_PREF, 8083);
	rsshostname = prefs.get(RSS_SERVER_PREF,"localhost");
	
	if (rsshostname.equals("") || rsshostname == null) {
		rsshostname = "localhost";
	}
	
    frame.setLocationByPlatform(true);
	frame.setVisible(true);    
	
    if (SystemTray.isSupported()) {
        WindowListener winStatelistener = new WindowAdapter() {
          public void windowIconified(WindowEvent w) {
			 System.out.println("Minimized window.  Sending to tray");
	         sendToTray();
          }
        };
		
        frame.addWindowListener(winStatelistener);
		
        sysTray = SystemTray.getSystemTray();
        String imgName = "images/tray.gif";
		URL imgURL = getClass().getResource(imgName);
		
		if (imgURL == null) {
			System.out.println(imgName + " not found");
		}
		
        Toolkit tk = Toolkit.getDefaultToolkit();
        Image image = null;
        try {
           image = tk.getImage(imgURL);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
		
        ActionListener exitListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Exiting...");
                System.exit(0);
            }
        };

		
		ActionListener restoreListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				restoreFromTray();
			}
		};
		
		ActionListener aboutListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		      String version = this.getClass().getPackage().getImplementationVersion();
		      JOptionPane.showMessageDialog(frame, "Version: " + version, "About", JOptionPane.INFORMATION_MESSAGE);
			}
		};

		MouseListener doubleClickListener = new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == 1 && e.getClickCount() == 2)
					restoreFromTray();
			}
		};

        PopupMenu popup = new PopupMenu();
        MenuItem restoreItem = new MenuItem("Restore");
        restoreItem.addActionListener(restoreListener);

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.addActionListener(aboutListener);
		

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(exitListener);

		popup.add(restoreItem);
		popup.add(aboutItem);
        popup.add(exitItem);

        trayIcon = new TrayIcon(image, "RSShandler", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(doubleClickListener);
	} else {
	    // Tray not supported
		System.out.println("System tray not a supported feature of this OS.  Disabling feature.");
	}
    startServer();
  }

  private JMenuBar createMenu() {
    JMenuBar greenMenuBar = new JMenuBar();
    greenMenuBar.setOpaque(true);
    JMenu file = new JMenu("File");
    JMenuItem exit = new JMenuItem("Exit");
    startServer = new JMenuItem("Start server");
    stopServer = new JMenuItem("Stop server");
    startServer.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        startServer();
      }
    });
    stopServer.setEnabled(false);
    stopServer.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        stopServer();
      }
    });

    JMenuItem settingsMenu = new JMenuItem("Settings...");
    settingsMenu.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        JDialog settings = new JDialog(frame, "Settings", true);
        settings.setContentPane(createStartStopPanel(settings, port, proxyMode));
        settings.pack();
        settings.setLocationRelativeTo(frame);
        settings.setVisible(true);
      }
    });

    JMenu help = new JMenu("Help");
    JMenuItem about = new JMenuItem("About");
    about.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent arg0) {
        String version = this.getClass().getPackage().getImplementationVersion();
        JOptionPane.showMessageDialog(frame, "Version: " + version, "About", JOptionPane.INFORMATION_MESSAGE);
      }
    });

    exit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        System.exit(0);
      }
    });

    file.add(settingsMenu);
    file.addSeparator();
    file.add(startServer);
    file.add(stopServer);
    file.addSeparator();
    file.add(exit);
    help.add(about);
    greenMenuBar.add(file);
    greenMenuBar.add(help);
    return greenMenuBar;
  }

  private JPanel createGeneratorPanel() {
    JPanel infoPanel = new JPanel(new MigLayout("", "[][grow][]", "[][][][][][][][grow]"));
    JLabel typeLabel = new JLabel("");
    JLabel formatLabel = new JLabel("Format");
    JLabel orderbyLabel = new JLabel("Order by");
    JLabel removeLabel = new JLabel("Remove feed content");
    final JLabel idLabel = new JLabel("ID");
    final JLabel standardFeedLabel = new JLabel("Feed type/country/period");
    standardFeedLabel.setVisible(false);
    JLabel sizeLabel = new JLabel("Size");
    JLabel resultLabel = new JLabel("Result podcast URL");

    final JComboBox standardFeeds = new JComboBox(new Object[] { new StandardFeed("recently_featured", "Recently featured"), new StandardFeed("most_viewed", "Most viewed"),
        new StandardFeed("most_popular", "Most popular"), new StandardFeed("most_recent", "Most recent"), new StandardFeed("most_discussed", "Most discussed"),
        new StandardFeed("most_linked", "Most linked"), new StandardFeed("most_responded", "Most responded"), new StandardFeed("top_rated", "Top rated"),
        new StandardFeed("top_favorites", "Top favorites"), });

    final JComboBox countries = new JComboBox(new Object[] { new Country("", "Worldwide"), new Country("AU", "Australia"), new Country("BR", "Brazil"), new Country("CA", "Canada")
    , new Country("CZ", "Czech Republic"), new Country("FR", "France"), new Country("DE", "Germany"), new Country("GB", "Great Britain")
    , new Country("NL", "Holland"), new Country("HK", "Hong Kong"), new Country("IN", "India"), new Country("IE", "Ireland")
    , new Country("IL", "Israel"), new Country("IT", "Italy"), new Country("JP", "Japan"), new Country("MX", "Mexico")
    , new Country("NZ", "New Zealand"), new Country("PL", "Poland"), new Country("RU", "Russia"), new Country("KR", "South Korea")
    , new Country("ES", "Spain"), new Country("SE", "Sweden"), new Country("TW", "Taiwan"), new Country("US", "United States")});

    final JComboBox periods = new JComboBox(new Object[] {new Period("all_time", "all time"), new Period("today", "today"), new Period("this_week", "this week"), new Period("this_month", "this month") });

    standardFeeds.setVisible(false);
    countries.setVisible(false);
    periods.setVisible(false);
    standardFeeds.setToolTipText("Standard feed type");
    countries.setToolTipText("Country for standard feed type, select Worldwide for all videos");
    periods.setToolTipText("Period for standart feed");
    ButtonGroup types = new ButtonGroup();
    final JRadioButton typeUser = new JRadioButton("User");
    final JRadioButton typePlaylist = new JRadioButton("Playlist");
    final JRadioButton typeFavorites = new JRadioButton("Favorites");
    final JRadioButton typeStandart = new JRadioButton("Standart");
    final JRadioButton typeSubscriptions = new JRadioButton("Subscriptions");
    types.add(typeUser);
    types.add(typePlaylist);
    types.add(typeFavorites);
    types.add(typeStandart);
    types.add(typeSubscriptions);
    typeUser.setSelected(true);
    ButtonGroup sizes = new ButtonGroup();
    final JRadioButton size25 = new JRadioButton("25 items");
    final JRadioButton size50 = new JRadioButton("50 items");
    sizes.add(size25);
    sizes.add(size50);
    size25.setSelected(true);

    ButtonGroup orderby = new ButtonGroup();
    final JRadioButton orderbyPublished = new JRadioButton("published");
    orderbyPublished.setToolTipText("Order by publishing date in reverse order");
    final JRadioButton orderbyPosition = new JRadioButton("position (for playlists)");
    orderbyPosition.setToolTipText("Order by item position in playlist, used in playlists");
    final JRadioButton orderbyRelevance = new JRadioButton("relevance");
    orderbyRelevance.setToolTipText("Order by relevance");
    final JRadioButton orderbyViewCount = new JRadioButton("view count");
    orderbyViewCount.setToolTipText("Order by view count");
    orderby.add(orderbyPublished);
    orderby.add(orderbyPosition);
    orderby.add(orderbyRelevance);
    orderby.add(orderbyViewCount);
    orderbyPublished.setSelected(true);
    
    final JCheckBox removeDescription = new JCheckBox("description");
    removeDescription.setToolTipText("Remove description from feed items");
    final JCheckBox removeTitle = new JCheckBox("title");
    removeTitle.setToolTipText("Remove title from feed items");
    removeDescription.setSelected(true);

    final JTextField id = new JTextField();
    id.setToolTipText("Name that identifies feed with given type: for user and favorites feeds - user name, for playlists - playlist id");
    final JTextArea result = new JTextArea();
    result.setToolTipText("Copy paste this text to your podcast player as podcast link");
    result.setLineWrap(true);
    typeStandart.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          standardFeedLabel.setVisible(true);
          standardFeeds.setVisible(true);
          countries.setVisible(true);
          periods.setVisible(true);
          idLabel.setVisible(false);
          id.setVisible(false);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
          standardFeedLabel.setVisible(false);
          standardFeeds.setVisible(false);
          countries.setVisible(false);
          periods.setVisible(false);
          idLabel.setVisible(true);
          id.setVisible(true);
        }
      }
    });

    /*
	 * https://secure.wikimedia.org/wikipedia/en/wiki/YouTube#Quality_and_codecs
 	 * 5 = 400x240 @ FLV; 34 = 640x360 @ FLV; 35 = 854x480 @FLV;
	 * 18 = 480x270 @ MP4; 22 = 1280x720 @ MP4; 37 = 1920x1080 @MP4
     * 
     */
    final JComboBox formats = new JComboBox(new Object[] { 
		//MP4
		new YoutubeFormat(37, "1920x1080 (MP4)"), 
		//MP4
		new YoutubeFormat(22, "1280x720 (MP4)"), 
		// MP4
		new YoutubeFormat(18, "480x360 (MP4)"),
		//FLV
		new YoutubeFormat(35, "854x480 (FLV)"), 
		//FLV
		new YoutubeFormat(34, "640x360 (FLV)"), 
		// FLV
		new YoutubeFormat(5, "400x240 (FLV)") });
	
    JButton copyButton = new JButton("Copy to buffer");
    copyButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        StringSelection stringSelection = new StringSelection(result.getText());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, Gui.this);
      }
    });
    JButton generateButton = new JButton("Generate podcast URL");
    generateButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String text = id.getText();
        if ((text.length() == 0) && !typeStandart.isSelected()) {
          JOptionPane.showMessageDialog(frame, "You must enter id", "Input error", JOptionPane.ERROR_MESSAGE);
          return;
        }
        int size = -1;
        if (size25.isSelected()) {
          size = 25;
        } else if (size50.isSelected()) {
          size = 50;
        } else {
          JOptionPane.showMessageDialog(frame, "Select feed size", "Input error", JOptionPane.ERROR_MESSAGE);
        }
        String orderby = null;
        if (orderbyPublished.isSelected()) {
          orderby = "published";
        } else if (orderbyPosition.isSelected()) {
          orderby = "position";
        } else if (orderbyRelevance.isSelected()) {
          orderby = "relevance";
        } else if (orderbyViewCount.isSelected()) {
          orderby = "viewCount";
        } else {
          JOptionPane.showMessageDialog(frame, "Select order by", "Input error", JOptionPane.ERROR_MESSAGE);
        }
        int format = ((YoutubeFormat) formats.getSelectedItem()).getId();
        if (typeUser.isSelected()) {
          result.setText(getUserPodcastUrl(text, format, size, orderby, removeDescription.isSelected(), removeTitle.isSelected()));
        } else if (typePlaylist.isSelected()) {
          result.setText(getPlaylistPodcastUrl(text, format, size, orderby, removeDescription.isSelected(), removeTitle.isSelected()));
        } else if (typeFavorites.isSelected()) {
          result.setText(getFavoritesPodcastUrl(text, format, size, orderby, removeDescription.isSelected(), removeTitle.isSelected()));
        } else if (typeSubscriptions.isSelected()) {
            result.setText(getSubscriptionsPodcastUrl(text, format, size, orderby, removeDescription.isSelected(), removeTitle.isSelected()));
        } else if (typeStandart.isSelected()) {
          StandardFeed feed = (StandardFeed) standardFeeds.getSelectedItem();
          Country country = (Country) countries.getSelectedItem();
          Period period = (Period) periods.getSelectedItem();
          result.setText(getStandardUrl(feed, format, size, orderby, removeDescription.isSelected(), removeTitle.isSelected(), country.getId(), period.getId()));
        } else {
          JOptionPane.showMessageDialog(frame, "Select feed type", "Input error", JOptionPane.ERROR_MESSAGE);
        }
      }
    });
    infoPanel.add(typeLabel, "");
    infoPanel.add(typeUser, "split 4");
    infoPanel.add(typePlaylist, "");
    infoPanel.add(typeFavorites, "");
    infoPanel.add(typeSubscriptions, "");
    infoPanel.add(typeStandart, "wrap");
    infoPanel.add(formatLabel, "");
    infoPanel.add(formats, "wrap");
    infoPanel.add(sizeLabel, "");
    infoPanel.add(size25, "split 2");
    infoPanel.add(size50, "wrap");
    infoPanel.add(orderbyLabel, "");
    infoPanel.add(orderbyPublished, "split 4");
    infoPanel.add(orderbyPosition, "");
    infoPanel.add(orderbyRelevance, "");
    infoPanel.add(orderbyViewCount, "wrap");
    infoPanel.add(removeLabel, "");
    infoPanel.add(removeDescription, "split 2");
    infoPanel.add(removeTitle, "wrap");
    infoPanel.add(idLabel, "split 2,hidemode 2");
    infoPanel.add(standardFeedLabel, "hidemode 2");
    infoPanel.add(id, "growx, width 30::,split 4,hidemode 2");
    infoPanel.add(standardFeeds, "growx, width 30::,hidemode 2");
    infoPanel.add(countries, "growx, width 30::,hidemode 2");
    infoPanel.add(periods, "growx, width 30::,hidemode 2");
    infoPanel.add(generateButton, "wrap");
    infoPanel.add(resultLabel, "top");
    infoPanel.add(result, "grow, width 30:300:, height 10:100:");
    infoPanel.add(copyButton, "top");
    return infoPanel;
  }

  private String getStandardUrl(StandardFeed feed, int format, int size, String orderby, boolean removeDescription, boolean removeTitle, String country, String period) {
    String url = getPodcastUrl("standard", feed.getId(), format, size, orderby, removeDescription, removeTitle);
    if (country.length() > 0) {
      url += "&country="+country;
    }
    url += "&period="+period;
    return url;
  }

  private String getPlaylistPodcastUrl(String text, int format, int size, String orderby, boolean removeDescription, boolean removeTitle) {
    return getPodcastUrl("playlist", text, format, size, orderby, removeDescription, removeTitle);
  }

  private String getFavoritesPodcastUrl(String text, int format, int size, String orderby, boolean removeDescription, boolean removeTitle) {
    return getPodcastUrl("favorite", text, format, size, orderby, removeDescription, removeTitle);
  }

  private String getSubscriptionsPodcastUrl(String text, int format, int size, String orderby, boolean removeDescription, boolean removeTitle) {
	 return getPodcastUrl("subscriptions", text, format, size, orderby, removeDescription, removeTitle);
 }

  private String getUserPodcastUrl(String text, int format, int size, String orderby, boolean removeDescription, boolean removeTitle) {
    return getPodcastUrl("user.rss", text, format, size, orderby, removeDescription, removeTitle);
  }

  private String getPodcastUrl(String type, String text, int format, int size, String orderby, boolean removeDescription, boolean removeTitle) {
    String hostname = getHostName();
    return "http://" + getHostName() + ":" + port + "/" + type + "?id=" + text + "&format=" + format + "&host=" + hostname + "&port=" + port + "&size=" + size + "&orderby=" + orderby + "&removeDescription=" + removeDescription + "&removeTitle=" + removeTitle;
  }

  private String getHostName() {
    //try {
	return rsshostname;
	  //return "localhost";
      //return InetAddress.getLocalHost().getHostName();
    //} catch (UnknownHostException e) {
    //  JOptionPane.showMessageDialog(frame, "Cann't detect IP address, please change it manually", "Error", JOptionPane.ERROR_MESSAGE);
    // return "<INSERT YOUR IP ADDRESS>";
    //}
  }

  private JPanel createStartStopPanel(final JDialog settings, int portValue, boolean proxyModeValue) {
 	final JTextField serverName = new JTextField(1024);
	serverName.setText("" + rsshostname);
	final JCheckBox proxyMode = new JCheckBox();
    proxyMode.setToolTipText("Check this to proxy all videos through program, otherwise users will be redirected to result video directly from YouTube");
    proxyMode.setSelected(proxyModeValue);

    final JTextField port = new JTextField(5);
    port.setToolTipText("Server port number for podcast server");
    port.setText("" + portValue);
    port.setInputVerifier(new InputVerifier() {
      public boolean verify(JComponent input) {
        String value = port.getText();
        int port = -1;
        try {
          port = Integer.parseInt(value);
        } catch (NumberFormatException e) {
        }
        if ((port > 0) && (port < 65535)) {
          return true;
        } else {
          JOptionPane.showMessageDialog(settings, "Port must be number between 0 and 65535", "Input error", JOptionPane.ERROR_MESSAGE);
          return false;
        }
      }

      @Override
      public boolean shouldYieldFocus(JComponent input) {
        return verify(input);
      }
    });
    JButton updatePortButton = new JButton("Use new settings");
    updatePortButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        saveSettings(serverName.getText(),Integer.parseInt(port.getText()), proxyMode.isSelected());
        stopServer();
        startServer();
        settings.dispose();
      }
    });
    JButton cancel = new JButton("Cancel");
    cancel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        settings.dispose();
      }
    });

    JPanel serverPanel = new JPanel(new MigLayout("", "[grow][grow]", "[][][grow]"));
	serverPanel.add(new JLabel("RSS Server Name"), "w 50%");
	serverPanel.add(serverName, "w 50%, wrap");
    serverPanel.add(new JLabel("Port"), "w 50%");
    serverPanel.add(port, "w 50%, wrap");
    serverPanel.add(new JLabel("Proxy mode"), "");
    serverPanel.add(proxyMode, "wrap");
    serverPanel.add(updatePortButton, "split 2, span 2, center");
    serverPanel.add(cancel, "");
    return serverPanel;
  }

  private void saveSettings(String server, int port, boolean isProxyMode) {
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
	prefs.put(RSS_SERVER_PREF, server);
    prefs.putBoolean(PROXY_MODE_PREF, isProxyMode);
    prefs.putInt(PORT_PREF, port);
    this.port = port;
    this.proxyMode = isProxyMode;
	this.rsshostname = server;
  }

  private void stopServer() {
    boolean result = server.stop();
    if (result) {
      stopServer.setEnabled(false);
      startServer.setEnabled(true);
    } else {
      JOptionPane.showMessageDialog(frame, "Cann't stop server, for error message - check logs", "Server error", JOptionPane.ERROR_MESSAGE);
    }
    updateStats();
  }

  private static void updateStats() {
  }

  public void startServer() {
    startServer.setEnabled(false);
    stopServer.setEnabled(true);
    new Thread(new Runnable() {
      public void run() {
        server.setPort(port);
        server.setProxyMode(proxyMode);
        boolean result = server.start();
        if (!result) {
          JOptionPane.showMessageDialog(frame, "Cann't start server, for error message - check logs", "Server error", JOptionPane.ERROR_MESSAGE);
          startServer.setEnabled(true);
          stopServer.setEnabled(false);
        }
      }
    }).start();
    updateStats();
  }

   public static void main(String[] args) {
    PodcastServer server = new PodcastServer() {
      @Override
      public int getPort() {
        return 0;
      }

      @Override
      public void setPort(int parseInt) {
      }

      @Override
      public boolean start() {
        return false;
      }

      @Override
      public boolean stop() {
        return false;
      }

      @Override
      public boolean isProxyMode() {
        return false;
      }

      @Override
      public void setProxyMode(boolean selected) {
      }
    };
    Gui gui = new Gui();
    gui.setServer(server);
    gui.createGui();
  }

  private class YoutubeFormat {
    private int id;
    private String name;

    public YoutubeFormat(int id, String name) {
      this.id = id;
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      YoutubeFormat that = (YoutubeFormat) o;
      if (id != that.id)
        return false;
      return true;
    }

    @Override
    public int hashCode() {
      return id;
    }

    public int getId() {
      return id;
    }
  }

  @Override
  public void lostOwnership(Clipboard arg0, Transferable arg1) {
  }

  private class StandardFeed {
    private String id;
    private String name;

    public StandardFeed(String id, String name) {
      this.id = id;
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      StandardFeed that = (StandardFeed) o;
      if (!id.equals(that.id))
        return false;
      return true;
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    public String getId() {
      return id;
    }
  }

  public class Country {
    private String id;
    private String name;

    public Country(String id, String name) {
      this.id = id;
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      Country that = (Country) o;
      if (!id.equals(that.id))
        return false;
      return true;
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    public String getId() {
      return id;
    }
  }

  public class Period {
    private String id;
    private String name;

    public Period(String id, String name) {
      this.id = id;
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      Period that = (Period) o;
      if (!id.equals(that.id))
        return false;
      return true;
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    public String getId() {
      return id;
    }
  }
}