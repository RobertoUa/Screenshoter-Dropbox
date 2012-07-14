package com.roberto.Main;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JOptionPane;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;

/**
 */
public class Main {
	//	private static long start = System.nanoTime();

	/**
	 * Method main.
	 * @param args String[]
	 */
	public static void main(String args[]) {
		new Main();

	}

	/**
	 * Constructor for Main.
	 */
	public Main() {
		Configuration cfg = new Configuration();
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		upload(capture(toolkit), toolkit, cfg);
		playSound();

	}

	/**
	 * Method upload.
	 * @param inputStream ByteArrayInputStream
	 * @param toolkit Toolkit
	 * @param cfg Configuration
	 */
	private void upload(ByteArrayInputStream inputStream, Toolkit toolkit,
			Configuration cfg) {

		String appKey = cfg.getAPP_KEY();
		String appSec = cfg.getAPP_SECRET();
		String accessKey = cfg.getACCESS_KEY();
		String accessSec = cfg.getACCESS_SECRET();

		String currTime = Calendar.getInstance().getTime().toString();
		String filename = "/Screenshots/" + currTime + ".png";

		AppKeyPair appKeys = new AppKeyPair(appKey, appSec);
		WebAuthSession session = new WebAuthSession(appKeys, AccessType.DROPBOX);
		AccessTokenPair acc = new AccessTokenPair(accessKey, accessSec);

		session.setAccessTokenPair(acc);
		DropboxAPI<WebAuthSession> client = new DropboxAPI<>(session);

		try {
			int size = inputStream.available();
			client.putFile(filename, inputStream, size, null, null);
			String link = client.share(filename).url;
			StringSelection selection = new StringSelection(link);
			toolkit.getSystemClipboard().setContents(selection, null);

		} catch (DropboxException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, e.getMessage());
			}
			//	double end = (System.nanoTime() - start) / 1000000.0;
			//	System.out.printf("%.2f%s", end, " ms\n");

		}

	}

	/**
	 * Method capture.
	 * @param toolkit Toolkit
	 * @return ByteArrayInputStream
	 */
	private ByteArrayInputStream capture(Toolkit toolkit) {
		ByteArrayInputStream input = null;

		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

			ScreenCapture selecting = new ScreenCapture(toolkit);
			ExecutorService exec = Executors.newSingleThreadExecutor();
			BufferedImage image = exec.submit(selecting).get();

			ImageIO.write(image, "png", output);
			exec.shutdown();

			input = new ByteArrayInputStream(output.toByteArray());

		} catch (IOException | InterruptedException | ExecutionException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
		}
		return input;

	}

	/**
	 * Method playSound.
	 */
	public void playSound() {
		Executors.newSingleThreadExecutor().execute(new Thread(new Runnable() {
			public void run() {
				String soundName = "woohoo.wav";
				URL is = getClass().getResource(soundName);
				System.out.println();
				try (AudioInputStream ais = AudioSystem.getAudioInputStream(is)) {
					Clip clip = AudioSystem.getClip();
					clip.open(ais);
					clip.start();
				} catch (IOException | LineUnavailableException
						| UnsupportedAudioFileException e) {
					JOptionPane.showMessageDialog(null, e.getMessage());
				}
			}
		}));
		try {
			Thread.sleep(3000);
			System.exit(0);
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
		}
	}

}
