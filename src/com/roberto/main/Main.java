package com.roberto.main;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.JOptionPane;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import com.roberto.capture.ScreenCapture;
import com.roberto.dropbox.AccessTokenPair;
import com.roberto.dropbox.Dropbox;
import com.roberto.dropbox.DropboxException;

public class Main {
	// private static long start = System.nanoTime();

	private Thread copyAndNotify, captureThread;
	private ByteArrayOutputStream output;

	public static void main(String args[]) throws InterruptedException {
		new Main();
	}

	private Main() throws InterruptedException {
		AccessTokenPair accessKeys = loadKeys(false);
		captureScreen();

		final String filename = Calendar.getInstance().getTime().toString() + ".png";

		copyUrlAndNotify(filename, accessKeys);
		upload(filename, accessKeys);

		copyAndNotify.join();
		// System.out.printf("%.2f%s", (System.nanoTime() - start) / 1000000.0, " ms\n");

	}

	private void upload(String filename, AccessTokenPair accessKeys) {
		BufferedOutputStream out = null;
		try {
			HttpsURLConnection conn = Dropbox.upload(filename, accessKeys);
			out = new BufferedOutputStream(conn.getOutputStream());

			captureThread.join();
			out.write(output.toByteArray());

			conn.getInputStream().available();
			conn.disconnect();
			out.close();
		} catch (DropboxException | IOException | InterruptedException e) {
			showExceptionInfo(e);
			loadKeys(true);
			upload(filename, accessKeys);
		}
	}

	private void captureScreen() {
		captureThread = new Thread("Capture thread") {
			@Override
			public void run() {
				try {
					output = new ByteArrayOutputStream();

					ScreenCapture selecting = new ScreenCapture();
					ExecutorService exec = Executors.newSingleThreadExecutor();
					BufferedImage image = exec.submit(selecting).get();
					exec.shutdown();

					ImageIO.write(image, "png", output);
					image.flush();
				} catch (IOException | InterruptedException | ExecutionException e) {
					showExceptionInfo(e);
				}
			}
		};
		captureThread.setPriority(Thread.MAX_PRIORITY);
		captureThread.start();
	}

	/**
	 * Copies shorten url(or long one if url shrunking fails) to clipboard, notifies a user and terminates the programm
	 * 
	 */
	private void copyUrlAndNotify(final String filename, final AccessTokenPair accessKeys) {
		copyAndNotify = new Thread("copyAndNotify thread") {
			@Override
			public void run() {
				String soundName = "/notify.mp3";
				try {
					Player player = new Player(getClass().getResourceAsStream(soundName));

					captureThread.join();
					Thread.sleep(500);

					StringSelection selection = new StringSelection(Dropbox.share(filename,
							accessKeys));
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

					player.play();
					player.close();
				} catch (JavaLayerException | InterruptedException | IOException e) {
					showExceptionInfo(e);
				}
			}
		};
		copyAndNotify.setPriority(Thread.NORM_PRIORITY - 2);
		copyAndNotify.start();
	}

	private AccessTokenPair loadKeys(boolean doReauth) {
		final Configuration cfg = new Configuration(doReauth);
		final Map<String, String> keys = cfg.getKeysMap();

		String accessKey = keys.get("accessKey");
		String accessSecret = keys.get("accessSecret");

		return new AccessTokenPair(accessKey, accessSecret);

	}

	public static void showExceptionInfo(Exception e) {
		JOptionPane.showMessageDialog(null, e.toString() + " in " + e.getStackTrace()[0]);
	}

}
