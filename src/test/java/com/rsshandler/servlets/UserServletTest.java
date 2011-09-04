package com.rsshandler.servlets;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;

public class UserServletTest {

  @Test
  public void testTest() {
    UserServlet servlet = new UserServlet();
    String content = "<rss><item><link>http://www.youtube.com/watch?v=12</link><author>author</author></item><item><link>http://www.youtube.com/watch?v=34</link><author>author</author></item><item><link>http://www.youtube.com/watch?v=56</link><author>author</author></item></rss>";
    String result = servlet.replaceEnclosures(content, 18, "testhost", "1234");
    assertEquals("<rss><item><link>http://www.youtube.com/watch?v=12</link><enclosure url=\"http://testhost:1234/video.mp4?format=18&id=12\" duration=\"35\" type=\"video/mp4\"/><author>author</author></item><item><link>http://www.youtube.com/watch?v=34</link><enclosure url=\"http://testhost:1234/video.mp4?format=18&id=34\" duration=\"35\" type=\"video/mp4\"/><author>author</author></item><item><link>http://www.youtube.com/watch?v=56</link><enclosure url=\"http://testhost:1234/video.mp4?format=18&id=56\" duration=\"35\" type=\"video/mp4\"/><author>author</author></item></rss>", result);
  }

}
