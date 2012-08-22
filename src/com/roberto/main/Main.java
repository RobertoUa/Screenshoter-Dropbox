package com.roberto.main;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
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
	//private static long start = System.nanoTime();

	private String uid;
	private Thread copyAndNotify, captureThread;
	private ByteArrayOutputStream output;
	private AccessTokenPair accessKeys;

	public static void main(String args[]) throws InterruptedException {
		new Main();
	}

	private Main() throws InterruptedException {
		loadKeys();

		captureScreen();

		String currTime = new SimpleDateFormat("dd-MMM-HH:mm:ss").format(Calendar.getInstance()
				.getTime());
		final String filename = "/Scrn/" + currTime + ".png";

		copyUrlAndNotify(filename);
		upload(filename);

		copyAndNotify.join();
	//	System.out.printf("%.2f%s", (System.nanoTime() - start) / 1000000.0, " ms\n");

	}

	private void upload(String filename) {
		BufferedOutputStream out = null;
		try {
			HttpsURLConnection conn = Dropbox.upload(filename, accessKeys);
			out = new BufferedOutputStream(conn.getOutputStream());

			captureThread.join();
			out.write(output.toByteArray());

			conn.getInputStream().available();
			conn.disconnect();
			out.close();
		} catch (DropboxException e) {
			loadKeys();
			upload(filename);
		} catch (IOException e) {
			showExceptionInfo(e);
		} catch (InterruptedException e) {
			showExceptionInfo(e);
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
				} catch (IOException e) {
					showExceptionInfo(e);
				} catch (InterruptedException e) {
					showExceptionInfo(e);
				} catch (ExecutionException e) {
					showExceptionInfo(e);
				}
			}
		};
		captureThread.setPriority(Thread.NORM_PRIORITY + 1);
		captureThread.start();
	}

	/** Copies shorten url(or long one if url shrunking fails) to clipboard, notifies a user and terminates the programm */
	private void copyUrlAndNotify(final String filename) {
		copyAndNotify = new Thread("copyAndNotify") {
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
		captureThread.setPriority(Thread.NORM_PRIORITY - 1);
		copyAndNotify.start();
	}

	private void loadKeys() {
		final Configuration cfg = new Configuration();
		final Map<String, String> keys = cfg.getKeysMap();

		String accessKey = keys.get("accessKey");
		String accessSecret = keys.get("accessSecret");
		uid = (String) keys.get("uid");
		accessKeys = new AccessTokenPair(accessKey, accessSecret);

	}

	public static void showExceptionInfo(Exception e) {
		JOptionPane.showMessageDialog(null, e.toString() + " in " + e.getStackTrace()[0]);
	}

}
