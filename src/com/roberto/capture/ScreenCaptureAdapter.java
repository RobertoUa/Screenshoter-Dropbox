package com.roberto.capture;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

/**Adapter class for ScreenCapture with some fiels*/
public abstract class ScreenCaptureAdapter extends JPanel implements
		KeyListener, MouseInputListener, Callable<BufferedImage> {
	private static final long serialVersionUID = 1L;

	protected final Color selection = new Color(140, 140, 140, 100);
	protected JDialog frame;
	protected BufferedImage image;

	protected int startY, startX;
	protected int x, y;
	protected int width, height;
	protected int endX, endY;

	protected boolean finished = false;

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {

	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mouseDragged(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}

	@Override
	public BufferedImage call() throws Exception {

		return null;
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {

	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

}
