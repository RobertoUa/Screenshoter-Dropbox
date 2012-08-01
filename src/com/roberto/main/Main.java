package com.roberto.main;

import static javax.swing.JOptionPane.showMessageDialog;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;
import com.roberto.capture.ScreenCapture;

public class Main {
	public enum Keys {
		APP_KEY, APP_SECRET, ACCESS_KEY, ACCESS_SECRET, UID
	}

	//private static long start = System.nanoTime();
	private String appKey, appSecret, accessKey, accessSecret;
	private String uid;
	private Thread copyAndNotify;

	public static void main(String args[]) throws InterruptedException {
		new Main();
		//	System.out.printf("%.2f%s", (System.nanoTime() - start) / 1000000.0, " ms55\n");
	}

	private Main() throws InterruptedException {
		final Configuration cfg = new Configuration();
		loadKeys(cfg);

		upload(capture(), cfg);

		copyAndNotify.join();
		System.exit(0); // we are done here
	}

	private final void upload(ByteArrayInputStream imgBytes, Configuration cfg) {
		final String currTime = new SimpleDateFormat("dd-MMM-HH:mm:ss").format(
				Calendar.getInstance().getTime()).toString();

		final String filename = "/Scrn/" + currTime + ".png";
		final String url = "http://dl.dropbox.com/u/" + uid + filename;

		copyUrlAndNotify(url);
		AppKeyPair appKeys = new AppKeyPair(appKey, appSecret);
		WebAuthSession session = new WebAuthSession(appKeys, AccessType.DROPBOX);
		AccessTokenPair acc = new AccessTokenPair(accessKey, accessSecret);

		session.setAccessTokenPair(acc);
		DropboxAPI<WebAuthSession> client = new DropboxAPI<>(session);

		try {
			int size = imgBytes.available();
			client.putFile("/Public" + filename, imgBytes, size, null, null);
		} catch (DropboxException e) {
			cfg.loadCfg();
			upload(imgBytes, cfg);
		}
	}

	private final ByteArrayInputStream capture() {
		ByteArrayInputStream input = null;

		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

			ScreenCapture selecting = new ScreenCapture();
			ExecutorService exec = Executors.newSingleThreadExecutor();
			BufferedImage image = exec.submit(selecting).get();
			exec.shutdown();
			ImageIO.write(image, "png", output);

			input = new ByteArrayInputStream(output.toByteArray());
		} catch (IOException | InterruptedException | ExecutionException e) {
			showMessageDialog(null, e.getMessage());
		}
		return input;

	}

	/** Copies shorten url(or long one if url shrunking fails) to clipboard, notifies a user and terminates the programm*/
	private void copyUrlAndNotify(final String url) {
		copyAndNotify = new Thread(new Runnable() {
			@Override
			public void run() {
				String shortUrl = URLshortener.shorten(url);
				StringSelection selection = new StringSelection(shortUrl);
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
				try {
					String soundName = "/notify.mp3";
					Player player = new Player(getClass().getResourceAsStream(soundName));
					player.play();
				} catch (JavaLayerException e) {
					showMessageDialog(null, e.getMessage());
				}
			}
		});
		copyAndNotify.start();

	}

	private void loadKeys(Configuration cfg) {
		final Map<Keys, String> keys = cfg.getKeysMap();

		appKey = keys.get(Keys.APP_KEY);
		appSecret = keys.get(Keys.APP_SECRET);
		accessKey = keys.get(Keys.ACCESS_KEY);
		accessSecret = keys.get(Keys.ACCESS_SECRET);
		uid = keys.get(Keys.UID);
	}

}
