# How to run RssHandler as Windows service #

This should work with current (2.0) version of RssHandler and 3.4.0 version of [Java Service Wrapper](http://wrapper.tanukisoftware.org/doc/english/download.jsp).

  1. Download at http://wrapper.tanukisoftware.org/doc/english/download.jsp , delta pack,  Community edition
  1. Unpack it to some folder (it is good if your folder will not be too deep in file structure, because I faced problems because of this)
  1. Create "rsshandler" folder in unpacked folder
  1. Download all jars needed for RssHandler from Download tab to "rsshandler" folder (at the moment there are following jars):
```
jetty-continuation-7.0.1.v20091125.jar
jetty-http-7.0.1.v20091125.jar
jetty-io-7.0.1.v20091125.jar
jetty-security-7.0.1.v20091125.jar
jetty-server-7.0.1.v20091125.jar
jetty-servlet-7.0.1.v20091125.jar
jetty-util-7.0.1.v20091125.jar
miglayout-3.7.2.jar
rsshandler-2.0.jar
servlet-api-2.5.jar 
```
  1. Update conf/wrapper.conf with this information:
```
#********************************************************************
# Wrapper License Properties (Ignored by Community Edition)
#********************************************************************
# Include file problems can be debugged by removing the first '#'
#  from the following line:
##include.debug
#include ../conf/wrapper-license.conf
#include ../conf/wrapper-license-%WRAPPER_HOST_NAME%.conf
#********************************************************************
# Wrapper Java Properties
#********************************************************************
# Java Application
wrapper.java.command=java
# Tell the Wrapper to log the full generated Java command line.
#wrapper.java.command.loglevel=INFO
# Java Main class.  This class must implement the WrapperListener interface
#  or guarantee that the WrapperManager class is initialized.  Helper
#  classes are provided to do this for you.  See the Integration section
#  of the documentation for details.
wrapper.java.mainclass=org.tanukisoftware.wrapper.WrapperSimpleApp
# Java Classpath (include wrapper.jar)  Add class path elements as
#  needed starting from 1
wrapper.java.classpath.1=../lib/wrappertest.jar
wrapper.java.classpath.2=../lib/wrapper.jar
wrapper.java.classpath.3=../rsshandler/jetty-continuation-7.0.1.v20091125.jar
wrapper.java.classpath.4=../rsshandler/jetty-http-7.0.1.v20091125.jar
wrapper.java.classpath.5=../rsshandler/jetty-io-7.0.1.v20091125.jar
wrapper.java.classpath.6=../rsshandler/jetty-security-7.0.1.v20091125.jar
wrapper.java.classpath.7=../rsshandler/jetty-server-7.0.1.v20091125.jar
wrapper.java.classpath.8=../rsshandler/jetty-servlet-7.0.1.v20091125.jar
wrapper.java.classpath.9=../rsshandler/jetty-util-7.0.1.v20091125.jar
wrapper.java.classpath.10=../rsshandler/miglayout-3.7.2.jar
wrapper.java.classpath.11=../rsshandler/rsshandler-2.0.jar
wrapper.java.classpath.12=../rsshandler/servlet-api-2.5.jar
# Java Library Path (location of Wrapper.DLL or libwrapper.so)
wrapper.java.library.path.1=../lib
# Java Bits.  On applicable platforms, tells the JVM to run in 32 or 64-bit mode.
wrapper.java.additional.auto_bits=TRUE
# Initial Java Heap Size (in MB)
#wrapper.java.initmemory=3
# Maximum Java Heap Size (in MB)
#wrapper.java.maxmemory=64
# Application parameters.  Add parameters as needed starting from 1
wrapper.app.parameter.1=com.rsshandler.PodcastServerImpl
wrapper.app.parameter.2=8085
wrapper.app.parameter.3=true
#********************************************************************
# Wrapper Logging Properties
#********************************************************************
# Enables Debug output from the Wrapper.
# wrapper.debug=TRUE
# Format of output for the console.  (See docs for formats)
wrapper.console.format=PM
# Log Level for console output.  (See docs for log levels)
wrapper.console.loglevel=INFO
# Log file to use for wrapper output logging.
wrapper.logfile=../logs/wrapper.log
# Format of output for the log file.  (See docs for formats)
wrapper.logfile.format=LPTM
# Log Level for log file output.  (See docs for log levels)
wrapper.logfile.loglevel=INFO
# Maximum size that the log file will be allowed to grow to before
#  the log is rolled. Size is specified in bytes.  The default value
#  of 0, disables log rolling.  May abbreviate with the 'k' (kb) or
#  'm' (mb) suffix.  For example: 10m = 10 megabytes.
wrapper.logfile.maxsize=0
# Maximum number of rolled log files which will be allowed before old
#  files are deleted.  The default value of 0 implies no limit.
wrapper.logfile.maxfiles=0
# Log Level for sys/event log output.  (See docs for log levels)
wrapper.syslog.loglevel=NONE
#********************************************************************
# Wrapper General Properties
#********************************************************************
# Allow for the use of non-contiguous numbered properties
wrapper.ignore_sequence_gaps=TRUE
# Title to use when running as a console
wrapper.console.title=Test Wrapper Sample Application
#********************************************************************
# Wrapper Windows NT/2000/XP Service Properties
#********************************************************************
# WARNING - Do not modify any of these properties when an application
#  using this configuration file has been installed as a service.
#  Please uninstall the service before modifying this section.  The
#  service can then be reinstalled.
# Name of the service
wrapper.name=rsshandler
# Display name of the service
wrapper.displayname=RssHandler server
# Description of the service
wrapper.description=RssHandler server service
# Service dependencies.  Add dependencies as needed starting from 1
wrapper.ntservice.dependency.1=
# Mode in which the service is installed.  AUTO_START, DELAY_START or DEMAND_START
wrapper.ntservice.starttype=AUTO_START
# Allow the service to interact with the desktop.
wrapper.ntservice.interactive=false
```
  1. Run bin/InstallTestWrapper-NT.bat
  1. Run bin/StartTestWrapper-NT.bat or you can start it via _Administrative tools_
  1. Service should be working

## Tips ##
If there is problem, it is good to check logs folder.

Service can be managed as any normal Windows service via _Administrative tools_.

Instructions based on http://wrapper.tanukisoftware.org/doc/english/integrate.html Method 1. No customization of RssHandler was done.