package com.roberto.main;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.roberto.dropbox.AccessTokenPair;
import com.roberto.dropbox.Dropbox;
import com.roberto.dropbox.DropboxException;

public class Configuration {

	private final static String ACCESS_KEY = "ACCESS_KEY";
	private final static String ACCESS_SECRET = "ACCESS_SECRET";
	private final static String UID = "UID";

	private final static String OAUTH_TOKEN = "oauth_token";
	private final static String OAUTH_TOKEN_SCRT = "oauth_token_secret";

	private Properties properties = new Properties();
	private String cfgPath;
	private File file;
	private static final String cfgFilename = "cfg.ini";

	public Configuration() {
		cfgPath = System.getProperties().getProperty("user.home") + File.separator
				+ ".screenshoter";

		file = new File(cfgPath + File.separator + cfgFilename);
		loadCfg();
	}

	public void loadCfg() {
		checkCfg();
		try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file))) {
			properties.load(input);
		} catch (IOException e) {
			storeAppKeyPair();
			loadCfg();
		}

		if (!properties.containsKey(ACCESS_KEY) || !properties.containsKey(ACCESS_SECRET)
				|| !properties.containsKey(UID)) {
			storeAppKeyPair();
			loadCfg();
		}
	}

	private void storeAppKeyPair() {
		try {
			Map<String, String> result = Dropbox.auth(null);

			Desktop.getDesktop().browse(
					URI.create("https://www.dropbox.com/1/oauth/authorize?oauth_token="
							+ result.get(OAUTH_TOKEN) + "&oauth_callback=http://localhost:1337/"));
			waitForAuth();

			AccessTokenPair acc = new AccessTokenPair(result.get(OAUTH_TOKEN),
					result.get(OAUTH_TOKEN_SCRT));
			result = Dropbox.auth(acc);

			saveConfiguration(ACCESS_KEY, result.get(OAUTH_TOKEN));
			saveConfiguration(ACCESS_SECRET, result.get(OAUTH_TOKEN_SCRT));
			saveConfiguration(UID, result.get(UID.toLowerCase()));
		} catch (DropboxException e) {
			Main.showExceptionInfo(e);
		} catch (MalformedURLException e) {
			Main.showExceptionInfo(e);
		} catch (IOException e) {
			Main.showExceptionInfo(e);
		}

	}

	private void waitForAuth() {
		try (ServerSocket serverSocket = new ServerSocket(1337);
				Socket clientSocket = serverSocket.accept();
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						clientSocket.getInputStream()))) {

			while (true) {
				String cominginText = "";
				cominginText = in.readLine();
				if (!"null".equals(cominginText)) {
					out.println("DONE");
					break;
				}
				System.out.println(cominginText);
				Thread.sleep(500);
			}
		} catch (IOException e) {
			Main.showExceptionInfo(e);
		} catch (InterruptedException e) {
			Main.showExceptionInfo(e);
		}

	}

	private void saveConfiguration(String key, String value) {
		try (FileOutputStream write = new FileOutputStream(file)) {
			properties.setProperty(key.toString(), value);
			properties.store(write, "DONT TOUCH THEM");
		} catch (IOException e) {
			Main.showExceptionInfo(e);
		}

	}

	private void checkCfg() {
		boolean exist = file.exists();
		if (!exist) {
			try {
				new File(cfgPath).mkdir();
				file.createNewFile();
			} catch (IOException e) {
				Main.showExceptionInfo(e);
			}
		}
	}

	@SuppressWarnings("serial")
	public final Map<String, String> getKeysMap() {
		return Collections.unmodifiableMap(new HashMap<String, String>() {{		
				put("uid", properties.getProperty(UID));
				put("accessKey", properties.getProperty(ACCESS_KEY));
				put("accessSecret", properties.getProperty(ACCESS_SECRET));				
			}});

	}
}
