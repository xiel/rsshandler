# Build Environment #
Rsshandler is simply a Java application that can be run from many different operating systems.  In turn it can also be built from the source code on these same platforms pending their is support for all the other prerequisites.

# Prerequisites #
For any system you will need the following software to build the application.

  * JDK 1.6
  * Gradle (http://gradle.org/)
  * Mercurial client (http://mercurial.selenic.com/downloads/)

Installation of the above software packages are beyond the scope of this document.  If you need help with any of the above then it is recommended you seek help in the respective communities.

# Windows #

## Post Install Settings ##
After installing the JDK and Gradle, setup your environment variables.  You will need the following at the System scope.

  * %JAVA\_HOME%
  * Add %JAVA\_HOME% to your PATH
  * Add Gradle bin location to your PATH

```

JAVA_HOME=C:\Program Files\Java\jdk1.6.0_22
PATH= %SystemRoot%\system32;%SystemRoot%;%JAVA_HOME%\bin;;C:\utils\gradle-0.9-rc-3\bin;
```

## Testing ##
Open a new command line window (run->cmd.exe) and type java, which should produce a help screen. If you received a command not found error then check your paths.  Any corrections to your environment variables will require a restart of cmd.exe.

## Source ##
Next step is to fetch a local copy of the source code. The basic instructions to accomplish this with Rsshandler can be found on the Source page (http://code.google.com/p/rsshandler/source/checkout).
The mercurial client should be available on your right click menu as "TortoiseHG".

  * Right click a working directory (e.g. c:\workspaces\) and select TortoiseHG->Clone...
  * Enter the desired settings into the clone dialog
    * Source Path: https://rsshandler.googlecode.com/hg/
    * Destination Path: c:\workspace\rsshandler\
  * Click 'Clone"

## Build ##
First you will need to generate a keystore with which to allow jar signing to work properly.

http://www.mobilefish.com/tutorials/java/java_quickguide_keytool.html

Once you have a local keystore confiured, then you will be able to build.

  * create a batch file in c:\workspace\rsshandler\  that invokes the build with the appropriate properties
```

REM builds & signs jars to local debug directory
gradle -Pkeyalias=<keyalias> -Pkeyfile=<mykey.keystore> -Pkeypass=<keypassword> uploadLocalAll
```
  * Start cmd.exe
  * change to your working directory (c:\workspace\rsshandler)
  * invoke the batch file created above

If you encounter any errors then you can use -S to get detailed information to help resolve.
> ```
gradle -S -Pkeyalias=<keyalias> -Pkeyfile=<mykey.keystore> -Pkeypass=<keypassword> uploadLocalAll```


## Running the new build ##
There are two ways to run this build depending on if the GUI is desired.

Commandline
```
java -cp google-collect-1.0-rc1.jar;jsr305.jar;mail.jar;gdata-client-1.0.jar;gdata-core-1.0.jar;gdata-media-1.0.jar;gdata-youtube-2.0.jar;servlet-api-2.5.jar;rsshandler-3.0.1.jar;miglayout-3.7.2.jar;jetty-util-7.0.1.v20091125.jar;jetty-servlet-7.0.1.v20091125.jar;jetty-server-7.0.1.v20091125.jar;jetty-security-7.0.1.v20091125.jar;jetty-io-7.0.1.v20091125.jar;jetty-http-7.0.1.v20091125.jar;jetty-continuation-7.0.1.v20091125.jar  com.rsshandler.PodcastServerImpl 8083```

GUI
```
java -cp google-collect-1.0-rc1.jar;jsr305.jar;mail.jar;gdata-client-1.0.jar;gdata-core-1.0.jar;gdata-media-1.0.jar;gdata-youtube-2.0.jar;servlet-api-2.5.jar;rsshandler-3.0.1.jar;miglayout-3.7.2.jar;jetty-util-7.0.1.v20091125.jar;jetty-servlet-7.0.1.v20091125.jar;jetty-server-7.0.1.v20091125.jar;jetty-security-7.0.1.v20091125.jar;jetty-io-7.0.1.v20091125.jar;jetty-http-7.0.1.v20091125.jar;jetty-continuation-7.0.1.v20091125.jar  com.rsshandler.Main 8083
```

If you just want to run the GUI without additional command line windows (e.g. java log output), then use this command.
```
start javaw -cp servlet-api-2.5.jar;rsshandler-3.0.1.jar;miglayout-3.7.2.jar;jetty-util-7.0.1.v20091125.jar;jetty-servlet-7.0.1.v20091125.jar;jetty-server-7.0.1.v20091125.jar;jetty-security-7.0.1.v20091125.jar;jetty-io-7.0.1.v20091125.jar;jetty-http-7.0.1.v20091125.jar;jetty-continuation-7.0.1.v20091125.jar  com.rsshandler.Main 8083```

# Linux #

TBD

# OS/X #

TBD