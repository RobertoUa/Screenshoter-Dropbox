package com.roberto.main;

import static com.roberto.main.Main.Keys.ACCESS_KEY;
import static com.roberto.main.Main.Keys.ACCESS_SECRET;
import static com.roberto.main.Main.Keys.APP_KEY;
import static com.roberto.main.Main.Keys.APP_SECRET;
import static javax.swing.JOptionPane.showInputDialog;
import static javax.swing.JOptionPane.showMessageDialog;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.RequestTokenPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.WebAuthSession.WebAuthInfo;
import com.roberto.main.Main.Keys;

public class Configuration{
	
	private Properties properties = new Properties();
	private String cfgPath;
	private File file;
	private static final String cfgFilename = "cfg.ini";

	public Configuration() {
		cfgPath = System.getProperties().getProperty("user.home")
				+ File.separator + ".DBCfg";

		file = new File(cfgPath + File.separator + cfgFilename);
		loadCfg();
	}

	public void loadCfg() {
		checkCfg();
		try (BufferedInputStream input = new BufferedInputStream(
				new FileInputStream(file))) {
			properties.load(input);
		} catch (IOException e) {
			storeAppKeyPair();
			loadCfg();
		}
	
		if (!properties.containsKey(APP_KEY.toString())
				|| !properties.containsKey(APP_SECRET.toString())
				|| !properties.containsKey(ACCESS_KEY.toString())
				|| !properties.containsKey(ACCESS_SECRET.toString())) {
			storeAppKeyPair();
			loadCfg();
		}
	

	}

	private void storeAppKeyPair() {
		String appKey = showInputDialog("Enter your " + APP_KEY);
		String appSecret = showInputDialog("Enter your " + APP_SECRET);

		if (appKey == null || appSecret == null) {
			file.delete();
			System.exit(0);
		}

		saveConfiguration(APP_KEY, appKey);
		saveConfiguration(APP_SECRET, appSecret);
		AppKeyPair appKeys = new AppKeyPair(appKey, appSecret);
		WebAuthSession session = new WebAuthSession(appKeys, AccessType.DROPBOX);
		try {
			WebAuthInfo authInfo = session.getAuthInfo();
			RequestTokenPair pair = authInfo.requestTokenPair;
			String url = authInfo.url;
			Desktop.getDesktop().browse(new URL(url).toURI());
			showMessageDialog(null,
					"Press ok to continue once you have authenticated.");
			session.retrieveWebAccessToken(pair);

			AccessTokenPair tokens = session.getAccessTokenPair();
			saveConfiguration(ACCESS_KEY, tokens.key);
			saveConfiguration(ACCESS_SECRET, tokens.secret);
		} catch (DropboxException | IOException | URISyntaxException e) {
			showMessageDialog(null, e.getMessage());
		}

	}

	private void saveConfiguration(Keys key, String value) {
		try (FileOutputStream write = new FileOutputStream(file)) {
			properties.setProperty(key.toString(), value);
			properties.store(write, "DONT TOUCH THEM");
		} catch (IOException e) {
			showMessageDialog(null, e.getMessage());
		}

	}

	private void checkCfg() {
		boolean exist = file.exists();
		if (!exist) {
			try {
				new File(cfgPath).mkdir();
				file.createNewFile();
			} catch (IOException e) {
				showMessageDialog(null, e.getMessage());
			}
		}
	}

	@SuppressWarnings("serial")
	public Map<Keys, String> getKeysMap() {
		return new HashMap<Keys, String>() {
			{
				put(APP_KEY, properties.getProperty(APP_KEY.toString()));
				put(APP_SECRET, properties.getProperty(APP_SECRET.toString()));
				put(ACCESS_KEY, properties.getProperty(ACCESS_KEY.toString()));
				put(ACCESS_SECRET,
						properties.getProperty(ACCESS_SECRET.toString()));

			}
		};
	}

}
