# Command Line Mode #

You can run server as standalone application without GUI. This maybe usefull if you want to run program on server without GUI interface, to run it as service or simply because you like command line.

There are some limitations:

  * you have to configure proxy manually, you can read about it at http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html
  * no automatic updates

To run it from command line, you must complete following steps:

  1. you must have java executables in your path variables (you can check it by running "java -version")
  1. you must download all files with extension "jar" from http://code.google.com/p/rsshandler/downloads/list to one folder (you need only one file that starts with "rsshandler", pick one with highest version number)
  1. run this command from this folder
```
java -cp servlet-api-2.5.jar;rsshandler-2.0.jar;miglayout-3.7.2.jar;jetty-util-7.0.1.v20091125.jar;jetty-servlet-7.0.1.v20091125.jar;jetty-server-7.0.1.v20091125.jar;jetty-security-7.0.1.v20091125.jar;jetty-io-7.0.1.v20091125.jar;jetty-http-7.0.1.v20091125.jar;jetty-continuation-7.0.1.v20091125.jar com.rsshandler.PodcastServerImpl {PORT} {PROXY_MODE}
```
where

  * _{PORT}_ is port that you want to use, default is 8083

  * _{PROXY\_MODE}_ is whether you want to run it in proxy mode, or in redirect mode (proxy mode pumps all videos through server and this can create huge traffic, while redirect only redirects users to YouTube)