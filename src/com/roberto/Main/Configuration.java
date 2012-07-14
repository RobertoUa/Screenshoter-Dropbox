package com.roberto.Main;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import javax.swing.JOptionPane;

import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.RequestTokenPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.WebAuthSession.WebAuthInfo;

public class Configuration {
	private Properties properties = new Properties();
	private String cfgPath;
	private static final String cfgFilename = "cfg.xml";
	private static final String APP_KEY = "APP_KEY";
	private static final String APP_SECRET = "APP_SECRET";
	private static final String ACCESS_KEY = "ACCESS_KEY";
	private static final String ACCESS_SECRET = "ACCESS_SECRET";

	public Configuration() {
		cfgPath = System.getProperties().getProperty("user.home")
				+ File.separator + ".DBCfg";
		File file = new File(cfgPath + File.separator + cfgFilename);
		loadCFG(file);
	}

	private void loadCFG(File file) {
		checkCfg(file);
		try (BufferedInputStream input = new BufferedInputStream(
				new FileInputStream(file))) {
			properties.loadFromXML(input);
		} catch (IOException e) {
			storeAppKeyPair(file);
			loadCFG(file);
		}
		if (!properties.containsKey(APP_KEY)
				|| !properties.containsKey(APP_SECRET)
				|| !properties.containsKey(ACCESS_KEY)
				|| !properties.containsKey(ACCESS_SECRET)) {
			storeAppKeyPair(file);
			loadCFG(file);
		}

	}

	private void storeAppKeyPair(File file) {
		String appKey = JOptionPane.showInputDialog("Enter your " + APP_KEY);
		String appSecret = JOptionPane.showInputDialog("Enter your "
				+ APP_SECRET);
		saveConfiguration(APP_KEY, appKey, file);
		saveConfiguration(APP_SECRET, appSecret, file);
		AppKeyPair appKeys = new AppKeyPair(appKey, appSecret);
		WebAuthSession session = new WebAuthSession(appKeys, AccessType.DROPBOX);
		try {
			WebAuthInfo authInfo = session.getAuthInfo();
			RequestTokenPair pair = authInfo.requestTokenPair;
			String url = authInfo.url;
			Desktop.getDesktop().browse(new URL(url).toURI());
			JOptionPane.showMessageDialog(null,
					"Press ok to continue once you have authenticated.");
			session.retrieveWebAccessToken(pair);

			AccessTokenPair tokens = session.getAccessTokenPair();
			saveConfiguration(ACCESS_KEY, tokens.key, file);
			saveConfiguration(ACCESS_SECRET, tokens.secret, file);
		} catch (DropboxException | IOException | URISyntaxException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
		}

	}

	private void saveConfiguration(String key, String value, File file) {
		try (FileOutputStream write = new FileOutputStream(file)) {
			properties.setProperty(key, value);
			properties.storeToXML(write, "DONT TOUCH THEM");
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
		}

	}

	private void checkCfg(File file) {
		boolean exist = file.exists();
		if (!exist) {
			try {
				new File(cfgPath).mkdir();
				file.createNewFile();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, e.getMessage());
			}
		}
	}

	public String getAPP_KEY() {
		return properties.getProperty(APP_KEY);
	}

	public String getAPP_SECRET() {
		return properties.getProperty(APP_SECRET);
	}

	public String getACCESS_KEY() {
		return properties.getProperty(ACCESS_KEY);
	}

	public String getACCESS_SECRET() {
		return properties.getProperty(ACCESS_SECRET);
	}
}
