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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.JOptionPane;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import com.roberto.capture.ScreenCapture;
import com.roberto.dropbox.AccessTokenPair;
import com.roberto.dropbox.Dropbox;
import com.roberto.dropbox.DropboxException;

public class Main {
	private static long start = System.nanoTime();

	private CountDownLatch latch = new CountDownLatch(1);

	public static void main(String args[]) {
		new Main();
	}

	private Main() {
		Map<String, String> keys = loadKeys(false);
		String accessKey = keys.get("accessKey");
		String accessSecret = keys.get("accessSecret");
		String uid = keys.get("uid");
		AccessTokenPair accessKeys = new AccessTokenPair(accessKey, accessSecret);

		ExecutorService exec = Executors.newSingleThreadExecutor();
		Future<byte[]> image = exec.submit(CaptureScreen.INSTANCE);

		String currTime = new SimpleDateFormat("dd-MMM-HH:mm:ss").format(Calendar.getInstance()
				.getTime());
		;

		final String filename = "/Scrn/" + currTime + ".png";
		copyUrlAndNotify(filename, uid);
		upload(filename, accessKeys, image);

		exec.shutdown();
		System.out.printf("%.2f%s", (System.nanoTime() - start) / 1000000.0, " ms\n");

	}

	private void upload(String filename, AccessTokenPair accessKeys, Future<byte[]> image) {
		HttpsURLConnection conn = null;
		try {
			conn = Dropbox.upload(filename, accessKeys);

			try (BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream())) {
				byte[] imageBytes = image.get();

				if (imageBytes == null) {
					throw new NullPointerException("the screenshot is null");
				}
				out.write(imageBytes);
			} catch (NullPointerException | InterruptedException | ExecutionException e) {
				Main.showExceptionInfo(e);
			}

			latch.countDown();
			conn.getInputStream();
		} catch (DropboxException | IOException e) {
			Main.showExceptionInfo(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private enum CaptureScreen implements Callable<byte[]> {
		INSTANCE;

		@Override
		public byte[] call() throws IOException, ExecutionException, InterruptedException {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			BufferedImage image = new ScreenCapture().call();
			PngEncoder.encode(image, output);
			image.flush();

			return output.toByteArray();

		}
	}

	/**
	 * Copies shorten url(or long one if url shrunking fails) to clipboard, notifies a user and terminates the programm
	 */
	private void copyUrlAndNotify(String filename, String uid) {
		final String soundName = "/notify.mp3";
		final String url = "http://dl.dropbox.com/u/" + uid + filename;
		new Thread("copyAndNotify thread") {
			@Override
			public void run() {
				Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 2);
				try {
					StringSelection selection = new StringSelection(URLshortener.shorten(url));
					Player player = new Player(getClass().getResourceAsStream(soundName));

					latch.await();
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
					player.play();
					player.close();
				} catch (JavaLayerException | InterruptedException e) {
					showExceptionInfo(e);
				}
			}
		}.start();

	}

	private final Map<String, String> loadKeys(boolean doReauth) {
		final Configuration cfg = new Configuration(doReauth);
		return cfg.getKeysMap();

	}

	public static void showExceptionInfo(Exception e) {
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, e.toString() + " in " + e.getStackTrace()[0]);
		throw new RuntimeException();
	}

}
