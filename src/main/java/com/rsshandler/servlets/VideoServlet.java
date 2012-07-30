package com.rsshandler.servlets;

import java.io.FileNotFoundException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TreeMap;

public class VideoServlet extends HttpServlet {

   private static final String URL_REGEX = "url=(http.+?videoplayback.+?id=.+?)(\\\\u0026|&)quality="; // match the URL starting from http until before quality

   private Logger logger = Logger.getLogger(this.getClass().getName());
   private final VideoFormatsMap videoFormatsMap = new VideoFormatsMap();
   private static final String YOUTUBE_URL = "http://www.youtube.com/watch?v=";
   private boolean proxy;

   public VideoServlet(boolean proxy) {
      this.proxy = proxy;
   }

   @Override
   protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      CookieManager cookieManager = new CookieManager();
      CookieHandler.setDefault(cookieManager);
      cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_NONE);

      List<String> headers = Collections.list(request.getHeaderNames());
      for (String name : headers) {
         logger.info(String.format("Header: %s, %s", name, request.getHeader(name)));
      }

      String id = request.getParameter("id");
      int format = Integer.parseInt(request.getParameter("format"));
      int fallback = Integer.parseInt(request.getParameter("fb"));
      logger.info(String.format("Video: %s, %s, %s", id, getFormatString(format), fallback));

      URL url = new URL(YOUTUBE_URL + id);
      URLConnection connection = url.openConnection();
      logger.info(String.format("Request properties: %s", connection.getRequestProperties()));
      InputStream inputStream = null;
      try {
         inputStream = connection.getInputStream();
      } catch (IOException ex) {
         // User deleted videos cause a FileNotFoundException.
         logger.log(Level.WARNING, "{0}", ex.toString());
         response.sendError(HttpServletResponse.SC_NOT_FOUND, url.getPath() + ":\n" + ex.toString());
      }

      if (inputStream != null) {
         String str = Utils.readString(inputStream);
         logger.info(String.format("Headers: %s", connection.getHeaderFields()));

         String redirect = null;
         try {
            redirect = getVideoLink(str, format, fallback);
         } catch (IllegalArgumentException ex) {
            logger.log(Level.WARNING, "Video URL: {0}\n{1}", new Object[]{YOUTUBE_URL + id, ex.toString()});
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, url.getPath() + ":\n" + ex.toString());
         }

         if (redirect != null) {
            if (proxy) {
               logger.info(String.format("Proxy: %s", redirect));
               proxyVideo(redirect, response);
            } else {
               logger.info(String.format("Redirect: %s", redirect));
               response.sendRedirect(redirect);
            }
         }
      }
   }

   private void proxyVideo(String redirect, HttpServletResponse response) throws IOException {
      URL url = new URL(redirect);
      URLConnection connection = url.openConnection();
      InputStream is = connection.getInputStream();
      OutputStream os = response.getOutputStream();
      byte arr[] = new byte[4096];
      int len = -1;
      int total = 0;
      while ((len = is.read(arr)) != -1) {
         os.write(arr, 0, len);
         total += len;
      }
      logger.info(String.format("Sent: %s bytes", total));
   }

   private String getVideoLink(String content, int fmt, int fb) throws UnsupportedEncodingException {

      // 1. Get the URL map. Parsing the map is much faster than parsing the whole content.
      String urlMap = getURLMap(content);
      if (urlMap == null)
      {
         // Perhaps the video has been removed, has been set as private,
         // or they've changed the attribute name. try to parse the whole content.
         logger.info("URL map can't be found. Trying to parse the content.");
         urlMap = content;
      }

      // 2. Get all the available URLs.
      String[] links = getLinks(urlMap);
      if (links == null || links.length == 0)
      {
         if (urlMap.indexOf("liveplay") >= 0) {
            throw new IllegalArgumentException("Can't download live play videos.");
         }
         else {
            throw new IllegalArgumentException("Couldn't find links in URL map.\n" + urlMap);
         }
      }

      // 3. Get the prefered URL according to the specified format.
      String preferredURL = getPreferredURL(links, fmt, fb);
      if (preferredURL == null)
      {
         throw new IllegalArgumentException("Couldn't find preferred URL :" + getFormatString(fmt) + ".");
      }

      // 4. Clean up the URL, or return the original if it can't be cleaned
      return getCleanedURL(preferredURL);
   }


   // 1.
   private String getURLMap(String content) {
      logger.info("Looking for URL map in content.");
      int urlMapIndex = content.indexOf("\"url_encoded_fmt_stream_map\"");
      if (urlMapIndex == -1)
      {
         logger.info("Couldn't find URL map in content.");
         return null;
      }
      logger.info("URL map found.");
      String urlMap = content.substring(urlMapIndex);
      urlMapIndex = urlMap.indexOf("\", ");
      if (urlMapIndex != -1)
      {
         urlMap = urlMap.substring(0, urlMapIndex);
      }
      return urlMap;
   }

   // 2.
   private String[] getLinks(String urlMap) throws UnsupportedEncodingException {
      logger.info("Parsing all available download links.");

      urlMap = URLDecoder.decode(urlMap, "UTF-8");

      Pattern pattern = Pattern.compile(URL_REGEX, Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(urlMap);
      // calculate the number of matches
      int numOfMatches = 0;
      while (matcher.find()) {
         numOfMatches++;
      }
      matcher.reset();

      if (numOfMatches == 0)
         return null;

      String[] links = new String[numOfMatches];
      int i = 0;
      while (matcher.find()) {
         links[i] = matcher.group(1);
         i++;
      }

      logger.log(Level.INFO, "Found {0} download links.", links.length);
      return links;
   }

   // 3.
   private String getPreferredURL(String[] links, int format, int fallback) {

      logger.log(Level.INFO, "Looking for best URL. Preferred format: {0}. Fallback: {1}", new Object[]{getFormatString(format), fallback == 1 ? "Yes" : "No"});

      String url = getLinkWithFormat(links, format);
      if (url == null && fallback == 1) {

         Object vf = videoFormatsMap.get(format);
         while (vf != null) {
            VideoFormat v = (VideoFormat) vf;

            int fallbackFormat = v.getFallback();

            url = getLinkWithFormat(links, fallbackFormat);

            if (url != null) {
               break;
            }

            vf = videoFormatsMap.get(fallbackFormat);
         }
      }

      return url;
   }

   private String getLinkWithFormat(String[] links, int format) {
      String url = null;
      for (int i = 0; i < links.length; i++) {
         String u = links[i];

         if (u.indexOf("itag=" + format) > -1 || u.indexOf("itag%3D" + format) > -1) {
            url = u;
            break;
         }
      }
      if (url != null)
         logger.log(Level.INFO, "Found URL for format: {0}.", getFormatString(format));
      else
         logger.log(Level.INFO, "Couldn''t find URL for format: {0}.", getFormatString(format));
      return url;
   }

   // 4.
   private String getCleanedURL(String url) throws UnsupportedEncodingException {
      /*
       * assuming url is in a format similar to the following:
       * http://o-o---preferred---stc-ruh7---v5---lscache8.c.youtube.com/videoplayback?upn=XfFiHq_PxfU&sparams=cp,id,ip,ipbits,itag,ratebypass,source,upn,expire&fexp=902906,914075,909909,910206,915507,907217,907335,921602,922600,919804,924500,924700,913547,904721,920706,924402,907344,912706,902518&ms=au&itag=45&ipbits=8&signature=A85C9CEA83DD7C9AD2F889D9159F072F844753A4.2294976BD6B1D9F3DC7538A2C7C95729CA823F84&mv=m&sver=3&mt=1343263040&ratebypass=yes&source=youtube&expire=1343287441&key=yt1&ip=77.30.93.26&cp=U0hTSFJTVV9JS0NOM19KTVlFOk5UenZTRXdmQXYz&id=ee1f32370653a086
       *
       * 4.1. Unescape the percent escapes.
       * 4.2. If url does not contain a "newshard" parameter with a "yes" value,
       *      it may cause a 403 error. So append the parameter to the url
       */
      logger.log(Level.INFO, "Cleaning URL: {0}", url);

      String cleanedURL = URLDecoder.decode(url, "UTF-8");
      if (url.indexOf("newshard") == -1) {
         cleanedURL = cleanedURL + "&newshard=yes";
      }

      return cleanedURL;
   }



   private String getFormatString(int format) {
      /** itag format mapping:
        *  37 = 1920X1080 MP4
        *  46 = 1920X1080 WebM
        *  22 = 1280X720  MP4
        *  45 = 1280X720  WebM
        *  35 = Large     FLV
        *  44 = Large     WebM
        *  34 = Medium    FLV
        *  18 = Medium    MP4
        *  43 = Medium    WebM
        *  5  = Small     FLV
        */
      switch (format) {
         case 37:
            return "HD-1080 (MP4)";
         case 22:
            return "HD-720 (MP4)";
         case 18:
            return "Medium (MP4)";
         case 35:
            return "Large (FLV)";
         case 34:
            return "Medium (FLV)";
         case 5:
            return "Small (FLV)";
         case 46:
            return "HD-1080 (WebM)";
         case 45:
            return "HD-720 (WebM)";
         case 44:
            return "Large (WebM)";
         case 43:
            return "Medium (WebM)";
         default:
            return "Unknown (" + format + ")";
      }
   }

   public class VideoFormat {

      private int resid = 0;  // resolution id
      private int type = 0;  // We only support FLV & MP4 currently
      private int fallback = 0;  // the id for the fallback resolution

      public VideoFormat(int r, int t, int f) {
         resid = r;
         type = t;
         fallback = f;
      }

      public int getFallback() {
         return fallback;
      }

      public int getResId() {
         return resid;
      }

      public int getType() {
         return type;
      }
   }

   private class VideoFormatsMap extends TreeMap<Integer, VideoFormat> {
      public VideoFormatsMap() {
         // Check getFormatString(int) to understand the numbers.
         // This map is used in getPreferedURL to navigate through the fallbacks.
         this.put(5 , new VideoFormat(5, 1, 0));
         this.put(34, new VideoFormat(34, 1, 5));
         this.put(35, new VideoFormat(35, 1, 34));
         this.put(18, new VideoFormat(18, 2, 0));
         this.put(22, new VideoFormat(22, 2, 18));
         this.put(37, new VideoFormat(37, 2, 22));
      }
   }
}