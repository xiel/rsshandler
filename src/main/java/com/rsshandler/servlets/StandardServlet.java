package com.rsshandler.servlets;

import javax.servlet.http.HttpServletRequest;

public class StandardServlet extends RssServlet {

   @Override
   protected String getPostFetchFixups(String s, String overridetitle, HttpServletRequest request) {
      logger.info("Running StandardServlet post fetch fixups");
      String ret = s;
      if (ret != null) {
         int indexOfTimeStart = ret.indexOf("&amp;time=");
         if (indexOfTimeStart != -1) {
            indexOfTimeStart += 10;
            int indexOfTimeEnd = ret.substring(indexOfTimeStart, indexOfTimeStart + 20).indexOf("&amp;");
            if (indexOfTimeEnd == -1) {
               // maybe time was the last parameter
               indexOfTimeEnd = ret.substring(indexOfTimeStart, indexOfTimeStart + 20).indexOf("'/>");
            }
            if (indexOfTimeEnd != -1) {
               indexOfTimeEnd += indexOfTimeStart;
               int indexOfTitleStart = ret.indexOf("<title>");
               int indexOfTitleEnd = ret.indexOf("</title>");
               System.out.println(indexOfTimeStart + "," + indexOfTimeEnd + "  " + indexOfTitleStart + "," + indexOfTitleEnd);
               if (indexOfTitleStart != -1 && indexOfTitleEnd != -1) {
                  String oldTitle = ret.substring(indexOfTitleStart + 7, indexOfTitleEnd);
                  String time = ret.substring(indexOfTimeStart, indexOfTimeEnd);
                  if (time.contains("all_time")) {
                     time = "All Time";
                  } else if (time.contains("this_week")) {
                     time = "This Week";
                  } else if (time.contains("today")) {
                     time = "Today";
                  } else if (time.contains("this_month")) {
                     time = "This Month";
                  }
                  String newTitle = oldTitle + " " + time;
                  ret = ret.replaceAll("</url><title>.*?</title>", "</url><title>" + newTitle + "</title>");
                  ret = ret.replaceAll("</lastBuildDate><title>.*?</title>", "</lastBuildDate><title>" + newTitle + "</title>");
               }
            }
         }
      }

      logger.info("Completed StandardServlet post fetch fixups");
      return ret;
   }

   @Override
   protected String getRssUrl(HttpServletRequest request) {
      String type = request.getParameter("id");
      String country = request.getParameter("country");
      String path = "http://gdata.youtube.com/feeds/api/standardfeeds/";
      if (country != null) {
         path += country + "/";
      }
      return path + type;
   }

   @Override
   protected boolean isTimeApplicable(HttpServletRequest request) {
      String type = request.getParameter("id");
      if (type.equals("top_rated") || type.equals("top_favorites") || type.equals("most_viewed") || type.equals("most_popular") || type.equals("most_discussed") || type.equals("most_responded")) {
         return true;
      }
      return false;
   }
}
