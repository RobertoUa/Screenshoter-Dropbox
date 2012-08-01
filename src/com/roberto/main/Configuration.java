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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.RequestTokenPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.session.WebAuthSession.WebAuthInfo;
import com.roberto.main.Main.Keys;

public class Configuration {

	private Properties properties = new Properties();
	private String cfgPath;
	private File file;
	private static final String cfgFilename = "cfg.ini";

	public Configuration() {
		cfgPath = System.getProperties().getProperty("user.home") + File.separator + ".DBCfg";

		file = new File(cfgPath + File.separator + cfgFilename);
		loadCfg();
	}

	public void loadCfg() {

		checkCfg();

		try { //TODO try-with-resources
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
		String appKey = JOptionPane.showInputDialog("Enter your " + APP_KEY);
		String appSecret = JOptionPane.showInputDialog("Enter your " + APP_SECRET);

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
			JOptionPane
					.showMessageDialog(null, "Press ok to continue once you have authenticated.");
			session.retrieveWebAccessToken(pair);

			AccessTokenPair tokens = session.getAccessTokenPair();
			DropboxAPI<WebAuthSession> client = new DropboxAPI<WebAuthSession>(session);

			saveConfiguration(ACCESS_KEY, tokens.key);
			saveConfiguration(ACCESS_SECRET, tokens.secret);

			String uid = String.valueOf(client.accountInfo().uid);
			saveConfiguration(UID, uid);
		} catch (DropboxException e) {
			Main.showExceptionInfo(e);
		} catch (MalformedURLException e) {
			Main.showExceptionInfo(e);
		} catch (IOException e) {
			Main.showExceptionInfo(e);
		} catch (URISyntaxException e) {
			Main.showExceptionInfo(e);
		}

	}

	private void saveConfiguration(Keys key, String value) {
		try { //TODO try-with-resources
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
