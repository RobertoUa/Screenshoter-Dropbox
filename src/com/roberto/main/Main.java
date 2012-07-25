package com.roberto.main;

import static javax.swing.JOptionPane.showMessageDialog;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;

public class Main {
	public enum Keys {
		APP_KEY, APP_SECRET, ACCESS_KEY, ACCESS_SECRET
	}

	private String appKey, appSecret, accessKey, accessSecret;

	public static void main(String args[]) {
		//long start = System.nanoTime();	

		new Main();

		//	double end = (System.nanoTime() - start) / 1000000.0;
		//	System.out.printf("%.2f%s", end, " ms\n");

	}

	public Main() {
		final Configuration cfg = new Configuration();
		loadKeys(cfg);
		boolean success = upload(capture(), cfg);
		if (success) {
			playSound();
		} else {
			showMessageDialog(null, "A screenshot wasn't uploaded");
		}

	}

	private boolean upload(ByteArrayInputStream inputStream, Configuration cfg) {
		boolean success = false;
		final String currTime = Calendar.getInstance().getTime().toString();
		final String filename = "/Screenshots/" + currTime + ".png";

		AppKeyPair appKeys = new AppKeyPair(appKey, appSecret);

		WebAuthSession session = new WebAuthSession(appKeys, AccessType.DROPBOX);
		AccessTokenPair acc = new AccessTokenPair(accessKey, accessSecret);

		session.setAccessTokenPair(acc);
		DropboxAPI<WebAuthSession> client = new DropboxAPI<>(session);

		try {
			final int size = inputStream.available();
			client.putFile(filename, inputStream, size, null, null);
			final String link = client.share(filename).url;
			final StringSelection selection = new StringSelection(link);
			final Toolkit toolkit = Toolkit.getDefaultToolkit();
			toolkit.getSystemClipboard().setContents(selection, null);
			success = true;
		} catch (DropboxException e) {
			cfg.loadCfg(0);
			upload(inputStream, cfg);
			success = false;
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				showMessageDialog(null, e.getMessage());
			}

		}
		return success;
	}

	private final ByteArrayInputStream capture() {
		ByteArrayInputStream input = null;

		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

			ScreenCapture selecting = new ScreenCapture();
			ExecutorService exec = Executors.newSingleThreadExecutor();
			BufferedImage image = exec.submit(selecting).get();

			ImageIO.write(image, "png", output);
			exec.shutdown();

			input = new ByteArrayInputStream(output.toByteArray());

		} catch (IOException | InterruptedException | ExecutionException e) {
			showMessageDialog(null, e.getMessage());
		}
		return input;

	}

	public final void playSound() {
		Executors.newSingleThreadExecutor().execute(new Thread(new Runnable() {
			public void run() {
				String soundName = "woohoo.wav";
				URL is = getClass().getResource(soundName);
				try (AudioInputStream ais = AudioSystem.getAudioInputStream(is)) {
					Clip clip = AudioSystem.getClip();
					clip.open(ais);
					clip.start();
				} catch (IOException | LineUnavailableException
						| UnsupportedAudioFileException e) {
					showMessageDialog(null, e.getMessage());
				}
			}
		}));
		try {
			Thread.sleep(3000);
			System.exit(0);
		} catch (InterruptedException e) {
			showMessageDialog(null, e.getMessage());
		}
	}

	private void loadKeys(Configuration cfg) {
		final Map<Keys, String> keys = cfg.getKeysMap();

		appKey = keys.get(Keys.APP_KEY);
		appSecret = keys.get(Keys.APP_SECRET);
		accessKey = keys.get(Keys.ACCESS_KEY);
		accessSecret = keys.get(Keys.ACCESS_SECRET);
	}
}
