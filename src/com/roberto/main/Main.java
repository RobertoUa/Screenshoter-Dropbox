package com.roberto.main;

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
import javax.swing.JOptionPane;

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

	private static long start = System.nanoTime();
	private String appKey, appSecret, accessKey, accessSecret;
	private String uid;
	private Thread copyAndNotify, captureThread;
	private ByteArrayInputStream imgBytes;

	public static void main(String args[]) throws InterruptedException {
		new Main();

	}

	private Main() throws InterruptedException {
		loadKeys();
		captureScreen();

		String currTime = new SimpleDateFormat("dd-MMM-HH:mm:ss").format(Calendar.getInstance()
				.getTime());
		String filename = "/Scrn/" + currTime + ".png";

		copyUrlAndNotify(filename);
		upload(filename);

		copyAndNotify.join();
		System.out.printf("%.2f%s", (System.nanoTime() - start) / 1000000.0, " ms\n");
		System.exit(0); // we are done here
	}

	private void upload(String filename) {
		AppKeyPair appKeys = new AppKeyPair(appKey, appSecret);
		WebAuthSession session = new WebAuthSession(appKeys, AccessType.DROPBOX);
		AccessTokenPair acc = new AccessTokenPair(accessKey, accessSecret);

		session.setAccessTokenPair(acc);
		DropboxAPI<WebAuthSession> client = new DropboxAPI<WebAuthSession>(session);

		try {
			captureThread.join();
			int size = imgBytes.available();
			client.putFile("/Public" + filename, imgBytes, size, null, null);
		} catch (DropboxException e) {
			loadKeys();
			upload(filename);
		} catch (InterruptedException e) {
			showExceptionInfo(e);
		}
	}

	private void captureScreen() {
		captureThread = new Thread("Capture thread") {
			@Override
			public void run() {
				try {
					ByteArrayOutputStream output = new ByteArrayOutputStream();
					ScreenCapture selecting = new ScreenCapture();
					ExecutorService exec = Executors.newSingleThreadExecutor();
					BufferedImage image = exec.submit(selecting).get();
					exec.shutdown();

					ImageIO.write(image, "png", output);
					imgBytes = new ByteArrayInputStream(output.toByteArray());
				} catch (IOException e) {
					showExceptionInfo(e);
				} catch (InterruptedException e) {
					showExceptionInfo(e);
				} catch (ExecutionException e) {
					showExceptionInfo(e);
				}
			}
		};
		captureThread.setPriority(Thread.NORM_PRIORITY + 2);
		captureThread.start();

	}

	/** Copies shorten url(or long one if url shrunking fails) to clipboard, notifies a user and terminates the programm*/
	private void copyUrlAndNotify(final String filename) {
		copyAndNotify = new Thread("copyAndNotify thread") {
			@Override
			public void run() {
				String url = "http://dl.dropbox.com/u/" + uid + filename;
				String shortUrl = URLshortener.shorten(url);
				StringSelection selection = new StringSelection(shortUrl);
				String soundName = "/notify.mp3";
				try {

					Player player = new Player(getClass().getResourceAsStream(soundName));
					captureThread.join();

					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
					player.play();
				} catch (JavaLayerException e) {
					showExceptionInfo(e);
				} catch (InterruptedException e) {
					showExceptionInfo(e);
				}
			}
		};
		copyAndNotify.setPriority(Thread.NORM_PRIORITY - 2);
		copyAndNotify.start();

	}

	private void loadKeys() {
		final Configuration cfg = new Configuration();
		final Map<Keys, String> keys = cfg.getKeysMap();

		appKey = keys.get(Keys.APP_KEY);
		appSecret = keys.get(Keys.APP_SECRET);
		accessKey = keys.get(Keys.ACCESS_KEY);
		accessSecret = keys.get(Keys.ACCESS_SECRET);
		uid = keys.get(Keys.UID);
	}

	public static void showExceptionInfo(Exception e) {
		JOptionPane.showMessageDialog(null, e.toString() + " in " + e.getStackTrace()[0]);
	}

}
