package com.roberto.main;

import static com.roberto.main.Main.Keys.ACCESS_KEY;
import static com.roberto.main.Main.Keys.ACCESS_SECRET;
import static com.roberto.main.Main.Keys.APP_KEY;
import static com.roberto.main.Main.Keys.APP_SECRET;
import static com.roberto.main.Main.Keys.UID;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.roberto.dropbox.AccessTokenPair;
import com.roberto.dropbox.AppKeyPair;
import com.roberto.dropbox.Dropbox;
import com.roberto.dropbox.DropboxException;
import com.roberto.main.Main.Keys;

public class Configuration {

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
		try { // TODO try-with-resources
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
			properties.load(input);
		} catch (IOException e) {
			storeAppKeyPair();
			loadCfg();
		}

		if (!properties.containsKey(APP_KEY.toString())
				|| !properties.containsKey(APP_SECRET.toString())
				|| !properties.containsKey(ACCESS_KEY.toString())
				|| !properties.containsKey(ACCESS_SECRET.toString())
				|| !properties.containsKey(UID.toString())) {
			storeAppKeyPair();
			loadCfg();
		}
	}

	private void storeAppKeyPair() {
		try {
			Desktop.getDesktop().browse(URI.create("https://www.dropbox.com/developers/apps"));
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			String appKey = JOptionPane.showInputDialog("Enter your " + APP_KEY);
			String appSecret = JOptionPane.showInputDialog("Enter your " + APP_SECRET);

			if (appKey == null || appSecret == null) {
				file.delete();
				System.exit(0);
			}

			saveConfiguration(APP_KEY, appKey);
			saveConfiguration(APP_SECRET, appSecret);
			AppKeyPair appKeys = new AppKeyPair(appKey, appSecret);
			Map<String, String> result = Dropbox.auth(appKeys, null);

			Desktop.getDesktop().browse(
					URI.create("https://www.dropbox.com/1/oauth/authorize?oauth_token="
							+ result.get("oauth_token")));

			JOptionPane
					.showMessageDialog(null, "Press ok to continue once you have authenticated.");

			AccessTokenPair acc = new AccessTokenPair(result.get("oauth_token"),
					result.get("oauth_token_secret"));
			result = Dropbox.auth(appKeys, acc);

			saveConfiguration(ACCESS_KEY, result.get("oauth_token"));
			saveConfiguration(ACCESS_SECRET, result.get("oauth_token_secret"));
			saveConfiguration(UID, result.get("uid"));
		} catch (DropboxException e) {
			Main.showExceptionInfo(e);
		} catch (MalformedURLException e) {
			Main.showExceptionInfo(e);
		} catch (IOException e) {
			Main.showExceptionInfo(e);
		} catch (ClassNotFoundException e) {
			Main.showExceptionInfo(e);
		} catch (InstantiationException e) {
			Main.showExceptionInfo(e);
		} catch (IllegalAccessException e) {
			Main.showExceptionInfo(e);
		} catch (UnsupportedLookAndFeelException e) {
			Main.showExceptionInfo(e);
		}

	}

	private void saveConfiguration(Keys key, String value) {
		try { // TODO try-with-resources
			FileOutputStream write = new FileOutputStream(file);
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
	public final Map<Keys, String> getKeysMap() {
		return Collections.unmodifiableMap(new EnumMap<Keys, String>(Keys.class) {
			{
				put(APP_KEY, properties.getProperty(APP_KEY.toString()));
				put(APP_SECRET, properties.getProperty(APP_SECRET.toString()));
				put(ACCESS_KEY, properties.getProperty(ACCESS_KEY.toString()));
				put(ACCESS_SECRET, properties.getProperty(ACCESS_SECRET.toString()));
				put(UID, properties.getProperty(UID.toString()));

			}
		});

	}
}
