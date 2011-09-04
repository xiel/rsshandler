package com.rsshandler.servlets;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class Utils {
	  
	public static String readString(InputStream is) throws IOException {
    Reader reader = new BufferedReader(new InputStreamReader(is, "utf8"));
    char arr[] = new char[4096];
    int len = -1;
    CharArrayWriter writer = new CharArrayWriter();
    while ((len = reader.read(arr)) != -1) {
      writer.write(arr, 0, len);
    }
    return writer.toString();
  }
}
