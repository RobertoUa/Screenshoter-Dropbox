package com.roberto.main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class URLshortener {
	private static final String URL_GOOGL_SERVICE = "https://www.googleapis.com/urlshortener/v1/url";

	public static String shorten(String longUrl) {
		DataOutputStream printout = null;
		BufferedReader reader = null;
		String json = "";
		try {
			URL url = new URL(URL_GOOGL_SERVICE);
			URLConnection urlConn = url.openConnection();
			urlConn.setDoInput(true); // Let the run-time system (RTS) know that we want input.
			urlConn.setDoOutput(true); // Let the RTS know that we want to do output.
			urlConn.setUseCaches(false); // No caching, we want the real thing.
			urlConn.setRequestProperty("Content-Type", "application/json"); // Specify the content type.

			printout = new DataOutputStream(urlConn.getOutputStream()); // Send POST output.
			String content = "{\"longUrl\":\"" + longUrl + "\"}";
			printout.writeBytes(content);

			reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

			String temp;
			while ((temp = reader.readLine()) != null) {
				json += temp;
			}

			JSONParser parser = new JSONParser();
			JSONObject jsonObject = (JSONObject) parser.parse(json);
			String link = (String) jsonObject.get("id");

			return link;

		} catch (IOException | ParseException ex) {
			return longUrl;
		} finally {
			try {
				if (printout != null && reader != null) {
					printout.flush();
					printout.close();
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
}
