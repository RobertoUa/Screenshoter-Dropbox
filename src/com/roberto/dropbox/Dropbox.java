package com.roberto.dropbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

public class Dropbox {

	public static final AppKeyPair APP_KEYS = new AppKeyPair("nzhmkbz5nta78ix", "6475uih5e79dkwe");
	private static final String CONTENT_SERVER = "https://api-content.dropbox.com/";
	private static final String API_SERVER = "https://api.dropbox.com/";
	private static final int VERSION = 1;

	public static HttpsURLConnection upload(String filename, AccessTokenPair acc)
			throws IOException {

		String params = "?overwrite=false&parent_rev=&locale=en";
		URL url = new URL(CONTENT_SERVER + VERSION + "/files_put/dropbox/Public" + filename
				+ params);

		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		conn.addRequestProperty("Authorization", Dropbox.buildOAuthHeader(APP_KEYS, acc));
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setUseCaches(false);
		conn.setRequestMethod("PUT");

		return conn;

	}

	public static Map<String, String> auth(AccessTokenPair acc) throws DropboxException,
			IOException {

		URL url;
		String str;
		if (acc == null) {
			url = new URL(API_SERVER + VERSION + "/oauth/request_token");
			str = Dropbox.buildOAuthHeader(APP_KEYS, null);

		} else {
			url = new URL(API_SERVER + VERSION + "/oauth/access_token");
			str = Dropbox.buildOAuthHeader(APP_KEYS, acc);
		}

		URLConnection conn = url.openConnection();
		conn.setRequestProperty("Authorization", str);

		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line, entity = "";
		while ((line = rd.readLine()) != null) {
			entity += line;
		}

		rd.close();
		
		return Dropbox.parseAsQueryString(entity);
	}

	private static Map<String, String> parseAsQueryString(String response) throws DropboxException {

		Scanner scanner = new Scanner(response).useDelimiter("&");

		Map<String, String> result = new HashMap<String, String>();

		while (scanner.hasNext()) {
			String nameValue = scanner.next();
			String[] parts = nameValue.split("=");
			if (parts.length != 2) {
				throw new DropboxException("Bad query string from Dropbox.");
			}
			result.put(parts[0], parts[1]);
		}
		scanner.close();
		return result;
	}

	public static String buildOAuthHeader(AppKeyPair appKeyPair, AccessTokenPair signingTokenPair) {
		StringBuilder buf = new StringBuilder();
		buf.append("OAuth oauth_version=\"1.0\"");
		buf.append(", oauth_signature_method=\"PLAINTEXT\"");
		buf.append(", oauth_consumer_key=\"").append(encode(appKeyPair.key)).append("\"");

		String sig;
		if (signingTokenPair != null) {
			buf.append(", oauth_token=\"").append(encode(signingTokenPair.key)).append("\"");
			sig = encode(appKeyPair.secret) + "&" + encode(signingTokenPair.secret);
		} else {
			sig = encode(appKeyPair.secret) + "&";
		}
		buf.append(", oauth_signature=\"").append(sig).append("\"");

		return buf.toString();
	}

	private static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			AssertionError ae = new AssertionError("UTF-8 isn't available");
			ae.initCause(ex);
			throw ae;
		}
	}
}
