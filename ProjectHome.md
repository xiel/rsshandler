This program allows to subscribe to YouTube video feeds as podcasts from iTunes, PSP, and other podcast software, to watch them later or offline.

# Installation #
  1. Program requires Java to run. Download Java at http://www.java.com/. Download it and restart browser or computer if required.
  1. Click at http://rsshandler.googlecode.com/files/rsshandler.jnlp . It will start launching and downloading of required components. Accept all prompts. It will start program and podcast server.

Program uses Java Web Start technology. You can read about it at http://www.java.com/en/download/faq/java_webstart.xml . Program requires all permisions to your computer, because it runs local podcast server and creates outgoing connections to YouTube.


# Usage #
## Create podcast URL ##
  1. Start server
  1. Enter type of feed you need: user (channel), playlist, favorites.
  1. Enter feed id. It is user name for user feed (for example Google), playlist id for playlists (for example 288B4A1BBFEF7424), user id for favorites (for example Google).
  1. Click _Generate podcast URL_ button.
  1. Copy/paste this url into your podcast application (for example iTunes)
  1. Watch

## Updating podcasts ##
Server must be running all the time when you doing updates (or all the time when podcast application is running, if updates are automatic)
  1. Just launch application, server starts automaticaly


# Troubleshooting #
## Podcast application cann't find server ##
Check that podcast URL is accessible. Just copy/paste URL into web browser. In Firefox you should see correctly formatted web page with pocast items, in other browsers you just may see a lot of text. If there is error that host is not accessible, probably application found incorrect host for your computer. Change it manually to correct host name or to IP address.

## How to check logs ##
There is description of how to turn on logging and tracing and where to find logs at http://java.sun.com/j2se/1.5.0/docs/guide/deployment/deployment-guide/tracing_logging.html