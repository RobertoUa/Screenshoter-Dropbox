package com.roberto.main;

import static javax.swing.JOptionPane.showMessageDialog;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
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
import com.roberto.capture.ScreenCapture;

public class Main {
	public enum Keys {
		APP_KEY, APP_SECRET, ACCESS_KEY, ACCESS_SECRET
	}

	//long start = System.nanoTime();	
	private String appKey, appSecret, accessKey, accessSecret;

	public static void main(String args[]) {
		new Main();
		//	System.out.printf("%.2f%s", (System.nanoTime() - start) / 1000000.0, " ms\n");
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

	private boolean upload(ByteArrayInputStream imgBytes, Configuration cfg) {
		boolean success = false;
		String currTime = new SimpleDateFormat("dd-MMM-HH:mm:ss").format(
				Calendar.getInstance().getTime()).toString();
		String filename = "/Scrn/" + currTime + ".png";

		AppKeyPair appKeys = new AppKeyPair(appKey, appSecret);

		WebAuthSession session = new WebAuthSession(appKeys, AccessType.DROPBOX);
		AccessTokenPair acc = new AccessTokenPair(accessKey, accessSecret);

		session.setAccessTokenPair(acc);
		DropboxAPI<WebAuthSession> client = new DropboxAPI<>(session);

		try {
			long uid = client.accountInfo().uid;
			int size = imgBytes.available();
			client.putFile("/Public" + filename, imgBytes, size, null, null);
			String url = "http://dl.dropbox.com/u/" + uid + filename;

			StringSelection selection = new StringSelection(url.toString());
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
			success = true;
		} catch (DropboxException e) {
			System.out.println("not done");
			cfg.loadCfg();
			upload(imgBytes, cfg);
			e.printStackTrace();
		} finally {
			try {
				imgBytes.close();
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

	public void playSound() {
		Executors.newSingleThreadExecutor().execute(new Thread(new Runnable() {
			public void run() {
				String soundName = "notify.wav";
				URL is = getClass().getResource(soundName);
				try (AudioInputStream ais = AudioSystem.getAudioInputStream(is)) {
					Clip clip = AudioSystem.getClip();
					clip.open(ais);
					clip.start();
				} catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
					showMessageDialog(null, e.getMessage());
				}
			}
		}));
		try {
			Thread.sleep(3000);//so program wont halt while sound is playing
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
