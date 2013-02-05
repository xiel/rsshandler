package com.rsshandler.servlets;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.TreeMap;

public class VideoServlet extends HttpServlet {

   private Logger logger = Logger.getLogger(this.getClass().getName());
   private final VideoFormatsMap VIDEO_FORMATS_MAP = new VideoFormatsMap();
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
      if (urlMap == null) {
         // Perhaps the video has been removed, has been set as private,
         // or YT have changed the attribute name.
         throw new IllegalArgumentException("Couldn't find URL map.");
      }

      // 2. Get all the available URLs.
      VideoURL[] videoURLs = getVideoURLs(urlMap);
      if (videoURLs == null || videoURLs.length == 0) {
         if (urlMap.indexOf("liveplay") >= 0)
            throw new IllegalArgumentException("Can't download live play videos.");

         throw new IllegalArgumentException("Couldn't find links in URL map: " + urlMap);
      }

      // 3. Get the prefered URL according to the specified format.
      VideoURL preferredVideoURL = getPreferredVideoURL(videoURLs, fmt, fb);
      if (preferredVideoURL == null) {
         throw new IllegalArgumentException("Couldn't find preferred URL :" + getFormatString(fmt) + ".");
      }

      String preferredURL = preferredVideoURL.getURL();
      if (preferredURL == null)
         throw new IllegalArgumentException("Couldn't parse URL from " + preferredVideoURL);

      return preferredURL;
   }

   private String getURLMap(String content) {
      logger.info("Looking for URL map in content.");
      int urlMapIndex = content.indexOf("\"url_encoded_fmt_stream_map\"");
      if (urlMapIndex == -1) {
         return null;
      }
      logger.info("URL map found.");
      String urlMap = content.substring(urlMapIndex);
      urlMapIndex = urlMap.indexOf("\", ");
      if (urlMapIndex != -1) {
         urlMap = urlMap.substring(0, urlMapIndex);
      }

      urlMap = urlMap.replaceAll("\\\\u0026", "&");

      return urlMap;
   }

   private VideoURL[] getVideoURLs(String urlMap) {
      logger.info("Parsing all available download URLs.");

      String[] rawURLs = urlMap.split(",");

      if (rawURLs.length == 1) {
         // the URLs couldn't be split by a comma. YT have changed the source?
         logger.log(Level.INFO, "Couldn't parse the URL map. Falling back to regular expressions.");

         // assuming that the urlMap starts with a key (e.g. type, itag, sig, url, etc.)
         // get this key, and count its occurrences in the urlMap. If we have more
         // than 1, then we'll use this key to build a dynamic regex to extract the URLs
         int indexOfEqualsSign = urlMap.indexOf("=");
         
         if (indexOfEqualsSign == -1)
            indexOfEqualsSign = urlMap.indexOf("%3D");

         if (indexOfEqualsSign != -1) {
            String key = urlMap.substring(0, indexOfEqualsSign);
            logger.log(Level.INFO, "Found term: \"{0}\"", key);

            Pattern pattern = Pattern.compile(key, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(urlMap);
            int numMatches = 0;
            while (matcher.find())
               numMatches++;

            logger.log(Level.INFO, "{0} occurrences found for term \"{1}\" in the URL map.", new Object[]{numMatches, key});
//            if (numMatches == 1) {
//               logger.log(Level.WARNING, "\"{0}\" not suitable for use in a regex." + key);
//               return null;
//            }

            final String regex = "(" + key + "=.*?)(?=[^0-9a-zA-Z]" + key + "|$)";
            logger.log(Level.INFO, "Using regex {0}", regex);

            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(urlMap);
            numMatches = 0;
            while (matcher.find())
               numMatches++;

            logger.log(Level.INFO, "Found {0} matches.", numMatches);
            if (numMatches == 0) return null;

            matcher.reset();

            rawURLs = new String[numMatches];
            int i = 0;
            while (matcher.find())
               rawURLs[i++] = matcher.group(1);
         }

         else {
            logger.log(Level.WARNING, "Failed to parse the URL map:\n{0}", urlMap);
            return null;
         }
      }
      
      VideoURL[] videoURLs = new VideoURL[rawURLs.length];
      for (int i = 0; i < videoURLs.length; i++)
         videoURLs[i] = new VideoURL(rawURLs[i]);

      logger.log(Level.INFO, "Found {0} download URLs.", videoURLs.length);
      return videoURLs;
   }

   private VideoURL getPreferredVideoURL(VideoURL[] urls, int format, int fallback) {

      logger.log(Level.INFO, "Looking for best URL. Preferred format: {0}. Fallback: {1}", new Object[]{getFormatString(format), fallback == 1 ? "Yes" : "No"});

      VideoURL url = getVideoURLWithFormat(urls, format);

      if (url == null && fallback == 1) {

         Object vf = VIDEO_FORMATS_MAP.get(format);
         while (vf != null) {
            VideoFormat v = (VideoFormat) vf;

            int fallbackFormat = v.getFallback();

            url = getVideoURLWithFormat(urls, fallbackFormat);

            if (url != null)
               break;

            vf = VIDEO_FORMATS_MAP.get(fallbackFormat);
         }
      }

      return url;
   }

   private VideoURL getVideoURLWithFormat(VideoURL[] urls, int format) {
      VideoURL url = null;
      for (int i = 0; i < urls.length; i++) {
         int fmt = urls[i].getFormat();

         if (fmt == format) {
            url = urls[i];
            break;
         }
      }
      if (url != null)
         logger.log(Level.INFO, "Found URL for format: {0}.", getFormatString(format));
      else
         logger.log(Level.INFO, "Couldn''t find URL for format: {0}.", getFormatString(format));
      return url;
   }

   private class VideoURL {
      /*
       * The purpose of this helper class is to encapsulate the parsing of
       * individual URLs from the rest of the class.
       *
       * The expected format for rawURL (without the new lines):
       *   type=video%2Fwebm%3B+codecs%3D%22vp8.0%2C+vorbis%22\u0026
       *   itag=45\u0026
       *   url=http%3A%2F%2Fr5---sn-nuj-g0il.c.youtube.com%2Fvideoplayback%3Fexpire%3D1359772705%26sver%3D3%26itag%3D45%26id%3Daf7c8e8b14a72445%26cp%3DU0hUTldSUF9FT0NONF9PTFRIOjBWbzQ0bU5ZWllw%26ms%3Dau%26mt%3D1359751214%26sparams%3Dcp%252Cid%252Cip%252Cipbits%252Citag%252Cratebypass%252Csource%252Cupn%252Cexpire%26mv%3Dm%26source%3Dyoutube%26fexp%3D913606%252C901700%252C916612%252C922910%252C928006%252C920704%252C912806%252C922403%252C922405%252C929901%252C913605%252C925710%252C920201%252C913302%252C919009%252C911116%252C926403%252C910221%252C901451%252C919114%26upn%3Dbsvgwu996kc%26newshard%3Dyes%26ipbits%3D8%26ratebypass%3Dyes%26ip%3D1.11.1.111%26key%3Dyt1\u0026
       *   sig=502B8CB8901B3D9F993CA700679A1B2D1001AE33.38E86E9E9231C98C2C80FF67C48FED16E8AEA259\u0026
       *   fallback_host=tc.v20.cache4.c.youtube.com\u0026
       *   quality=hd720
       */
      private String rawURL;
      private VideoFormat format;

      private final String MAIN_SPLIT_STR = "\u0026";

      private final String ITAG_REGEX = "itag(=|%3D)([0-9]+)";
      private final int ITAG_REGEX_GROUP = 2;

      // The following regexes will be used only if the expected format has changed.
      private final String[] URL_REGEXES = new String[] {
         // Should be sorted by reliablility, where the most reliable at the top
         // and the least reliable at the bottom
         "url=(http.+?videoplayback.+id=.+?)(\\\\u0026|&)(quality|fallback_host|$)=",
         "(http.+?videoplayback.+id=.+?)(\\\\u0026|&|$)"
      };
      private final String SIG_REGEX = "(sig|signature)(=|%3D)([0-9a-zA-Z]+\\.[0-9a-zA-Z]+)";
      private final int URL_REGEX_GROUP = 1;
      private final int SIG_REGEX_GROUP = 3;

      private VideoURL(String raw) {
         rawURL = raw;
         format = null;
      }

      private int getFormat() {
         if (format != null)
            return format.getResId();

         String[] splits = rawURL.split(MAIN_SPLIT_STR);
         for (int i = 0; i < splits.length; i++) {
            String string = splits[i];
            Pattern pattern = Pattern.compile(ITAG_REGEX, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(string);
            if (matcher.find()) {
               try {
                  int fmt = Integer.parseInt(matcher.group(ITAG_REGEX_GROUP));
                  format = VIDEO_FORMATS_MAP.get(fmt);
                  break;
               }
               catch (NumberFormatException e) {}
            }
         }
         if (format == null)
            return -1;
         return format.getResId();
      }

      private String getURL() throws UnsupportedEncodingException {
         logger.log(Level.INFO, "Cleaning URL.");

         String[] splits = rawURL.split(MAIN_SPLIT_STR);
         Map<String, String> mainParams = new HashMap<String, String>();
         for (int i = 0; i < splits.length; i++) {
            String string = splits[i];
            String[] keyAndVal = getKeyAndVal(string);
            if (keyAndVal != null) {
               assert keyAndVal.length == 2;
               logger.log(Level.INFO, "Found main parameter ({0} : {1})", new Object[]{keyAndVal[0], keyAndVal[1]});
               mainParams.put(keyAndVal[0], keyAndVal[1]);
            }
            else
               logger.log(Level.WARNING, "Could not parse key and value from {0}.", string);
         }

         String url = null;
         if (mainParams.containsKey("url"))
            url = URLDecoder.decode(mainParams.get("url"), "UTF-8");

         else {
            // Use regex on the raw url
            logger.log(Level.INFO, "URL not found. Trying to parse the URL using a regular expression.");
            for (int i = 0; i < URL_REGEXES.length; i++) {
               Pattern pattern = Pattern.compile(URL_REGEXES[i], Pattern.CASE_INSENSITIVE);
               Matcher matcher = pattern.matcher(rawURL);
               if (matcher.find()) {
                  url = URLDecoder.decode(matcher.group(URL_REGEX_GROUP), "UTF-8");
                  logger.log(Level.INFO, "Found URL using regex number {0}", i);
                  break;
               }
            }
         }

         if (url != null)
            url = validateParametersInURL(url, mainParams);
         else
            logger.log(Level.WARNING, "Could not find URL in: {0}", rawURL);
         
         return url;
      }

      private String validateParametersInURL(String url,
                                             Map<String, String> mainParams) {
         // check that it contains a signature and an itag

         // 1. signature. Three possible cases:
         // 1.1 url may not contain a signature
         // 1.2 url may contain a signature with the name "sig"
         // 1.3 url may contain a signature with the correct name "signature"
         if (!url.contains("signature=") && !url.contains("sig=")) {
            // Case 1:
            // Check if mainParams contains one, otherwise use a regex.
            String sig = mainParams.get("signature");
            if (sig == null)
               sig = mainParams.get("sig");

            if (sig == null) {
               // Fallback to a regex.
               Pattern pattern = Pattern.compile(SIG_REGEX, Pattern.CASE_INSENSITIVE);
               Matcher matcher = pattern.matcher(rawURL);
               if (matcher.find()) {
                  sig = matcher.group(SIG_REGEX_GROUP);
                  url = url + "&signature=" + sig;
               }
               else
                  logger.log(Level.WARNING, "Could not find signature for URL: {0}", rawURL);
            }
            else
               url = url + "&signature=" + sig;
         }

         else if (url.contains("sig=")) {
            // Case 2: replace sig with signature
            url = url.replaceAll("sig=", "signature=");
         }

         else {
            // Case 3:
            // just check that there isn't a mismatch between it and the
            // one in mainParams, if any.
            String sig = mainParams.get("signature");
            if (sig == null)
               sig = mainParams.get("sig");

            if (sig != null) {
               int indexOfSig = url.indexOf(sig);
               if (indexOfSig == -1)
                  // mismatch between the signatures
                  logger.log(Level.WARNING, "Mismatch between signature {0} and the one in the URL: {1}", new Object[] {sig, url});
            }
         }

         // 2. itag. Just check that it exists, and that it doesn't conflict
         //    with the one in mainParams, if any.
         String itag = mainParams.get("itag");
         if (url.contains("itag=")) {
            // check that it doesn't conflict with the one in mainParams, if any.
            if (itag != null && url.indexOf("itag=" + itag) == -1)
               // mismatch
               logger.log(Level.WARNING, "Mismatch between itag {0} and the one in the URL: {1}", new Object[] {itag, url});
         }

         else {
            // add the one in mainParams, if any
            if (itag != null)
               url = url + "&itag=" + itag;
            else {
               // url and mainParams has no itag, use the value returned by getFormat
               int fmt = this.getFormat();
               logger.log(Level.WARNING, "Could not find itag. Using {0}", fmt);
               url = url + "&itag=" + fmt;
            }
         }

         return url;
      }

      private String[] getKeyAndVal(String str) {

         Pattern pattern = Pattern.compile("^([^=]*)=(.*)$", Pattern.CASE_INSENSITIVE);
         Matcher matcher = pattern.matcher(str);

         if (matcher.find())
            return new String[] {matcher.group(1), matcher.group(2)};

         return null;
      }

      @Override
      public String toString() {
         return rawURL;
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
         this.put(5, new VideoFormat(5, 1, 0));
         this.put(34, new VideoFormat(34, 1, 5));
         this.put(35, new VideoFormat(35, 1, 34));
         this.put(18, new VideoFormat(18, 2, 0));
         this.put(22, new VideoFormat(22, 2, 18));
         this.put(37, new VideoFormat(37, 2, 22));
      }
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
}
