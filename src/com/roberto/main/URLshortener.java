package com.roberto.main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class URLshortener {
	private static final String URL_GOOGL_SERVICE = "https://www.googleapis.com/urlshortener/v1/url";

	public static String shorten(final String longUrl) {
		String link = "";
		String json = "";
		URLConnection urlConn = null;
		try {
			URL url = new URL(URL_GOOGL_SERVICE);
			urlConn = url.openConnection();
			urlConn.setDoInput(true); // Let the run-time system (RTS) know that we want input.
			urlConn.setDoOutput(true); // Let the RTS know that we want to do output.
			urlConn.setUseCaches(false); // No caching, we want the real thing.
			urlConn.setRequestProperty("Content-Type", "application/json"); // Specify the content type.
		} catch (IOException ex) {
			return longUrl;
		}

		try (DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream())) {
			String content = "{\"longUrl\":\"" + longUrl + "\"}";
			printout.writeBytes(content);
		} catch (IOException e) {
			return longUrl;
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				urlConn.getInputStream()))) {

			String temp;
			while ((temp = reader.readLine()) != null) {
				json += temp;
			}
			Scanner scanner = new Scanner(json).useDelimiter("\"");
			while (scanner.hasNext()) {
				link = scanner.next();
				if (link.contains("goo")) {
					scanner.close();
					break;
				}
			}
		} catch (IOException e) {
			return longUrl;
		}
		return link;
	}
}