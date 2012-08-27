package com.roberto.main;

import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


public final class PngEncoder implements ImageConsumer {
	private ImageProducer imageProducer;
	private ByteArrayOutputStream byteOutput;
	private ByteArrayOutputStream byteOutput2;
	private DeflaterOutputStream deflaterOutputStream;
	private Deflater deflater;
	private CRC32 crc32;
	private byte[] byteArray;
	private boolean notComplete;
	private static final byte[] bytes = { -119, 80, 78, 71, 13, 10, 26, 10 };
	private static final byte[] field = { 0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126 };

	private PngEncoder(Image paramImage, OutputStream paramOutputStream) throws IOException {
		byteOutput = new ByteArrayOutputStream();
		byteOutput2 = new ByteArrayOutputStream();
		deflater = new Deflater();
		crc32 = new CRC32();
		paramOutputStream.write(bytes);
		write(paramImage, new DataOutputStream(paramOutputStream));
		paramOutputStream.write(field);
	}

	public static synchronized void encode(Image paramImage, OutputStream paramOutputStream)
			throws IOException {
		new PngEncoder(paramImage, paramOutputStream);
	}

	private synchronized void write(Image paramImage, DataOutputStream paramDataOutputStream)
			throws IOException {
		if ((paramImage == null) || (paramDataOutputStream == null))
			return;
		byteOutput2.reset();
		byteOutput2.write("IDAT".getBytes());
		deflater.reset();
		deflaterOutputStream = new DeflaterOutputStream(byteOutput2, deflater);
		notComplete = true;
		imageProducer = paramImage.getSource();
		imageProducer.startProduction(this);
		while (notComplete)
			try {
				wait();
			} catch (InterruptedException e) {
				Main.showExceptionInfo(e);
			}
		deflaterOutputStream.finish();
		writeData(paramDataOutputStream);
	}

	public void setDimensions(int paramInt1, int paramInt2) {
		byteOutput.reset();
		try {
			DataOutputStream localDataOutputStream = new DataOutputStream(byteOutput);
			localDataOutputStream.write("IHDR".getBytes());
			localDataOutputStream.writeInt(paramInt1);
			localDataOutputStream.writeInt(paramInt2);
			byte[] bytes = {8, 2, 0, 0, 0 };
			localDataOutputStream.write(bytes);
		} catch (IOException e) {
			Main.showExceptionInfo(e);
		}
	}

	public void setColorModel(ColorModel paramColorModel) {
	}

	public void setPixels(int paramInt1, int paramInt2, int paramInt3, int paramInt4,
			ColorModel paramColorModel, int[] paramArrayOfInt, int paramInt5, int paramInt6) {
		if ((paramColorModel == null) || (paramArrayOfInt == null))
			return;
		if ((byteArray == null) || (byteArray.length < paramInt6 * 3 + 1))
			byteArray = new byte[paramInt6 * 3 + 1];
		int i = 0;
		for (int j = 0; j < paramInt4; j++) {
			byteArray[0] = 0;
			int k = 0;
			int m = 1;
			while (k < paramInt6) {
				byteArray[m] = (byte) (paramArrayOfInt[i] >> 16 & 0xFF);
				byteArray[(m + 1)] = (byte) (paramArrayOfInt[i] >> 8 & 0xFF);
				byteArray[(m + 2)] = (byte) (paramArrayOfInt[i] & 0xFF);
				m += 3;
				i++;
				k++;
			}
			try {
				deflaterOutputStream.write(byteArray, 0, paramInt6 * 3 + 1);
			} catch (IOException e) {
				Main.showExceptionInfo(e);
			}
		}
	}

	public void setPixels(int paramInt1, int paramInt2, int paramInt3, int paramInt4,
			ColorModel paramColorModel, byte[] paramArrayOfByte, int paramInt5, int paramInt6) {
	}

	public synchronized void imageComplete(int paramInt) {
		imageProducer.removeConsumer(this);
		notComplete = false;
		notifyAll();
	}

	private void writeData(DataOutputStream paramDataOutputStream) throws IOException {
		writeBytes(byteOutput, paramDataOutputStream);
		writeBytes(byteOutput2, paramDataOutputStream);
	}

	private void writeBytes(ByteArrayOutputStream paramByteArrayOutputStream,
			DataOutputStream paramDataOutputStream) throws IOException {
		if ((paramByteArrayOutputStream != null) && (paramDataOutputStream != null)) {
			paramDataOutputStream.writeInt(paramByteArrayOutputStream.size() - 4);
			byte[] arrayOfByte = paramByteArrayOutputStream.toByteArray();
			paramDataOutputStream.write(arrayOfByte);
			crc32.reset();
			crc32.update(arrayOfByte);
			paramDataOutputStream.writeInt((int) crc32.getValue());
		}
	}

	public void setProperties(Hashtable<?, ?> paramHashtable) {
	}

	public void setHints(int paramInt) {
	}

}
