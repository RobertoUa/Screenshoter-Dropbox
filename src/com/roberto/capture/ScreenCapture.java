package com.roberto.capture;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import static java.awt.Toolkit.getDefaultToolkit;

public class ScreenCapture extends ScreenCaptureAdapter {

	private static final long serialVersionUID = 1L;

	private JDialog frame;
	private BufferedImage image;
	private final Color selection = new Color(140, 140, 140, 100);

	private int startY, startX;
	private int x, y;
	private int width, height;
	private int endX, endY;

	private boolean finished;

	public ScreenCapture() {
		final Dimension resolution = getDefaultToolkit().getScreenSize();
		final Rectangle res = new Rectangle(resolution);
		try {
			image = new Robot().createScreenCapture(res);
		} catch (AWTException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
		}
		createAndShowGui(resolution);

	}

	private void createAndShowGui(Dimension resolution) {
		frame = new JDialog();
		frame.setBounds(0, 0, resolution.width, resolution.height);
		frame.setUndecorated(true);
		frame.add(this);
		frame.setResizable(false);
		frame.setVisible(true);
		frame.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		frame.setAlwaysOnTop(true);

		addMouseListener(this);
		addMouseMotionListener(this);
		frame.addKeyListener(this);
		addKeyListener(this);
		setIgnoreRepaint(true);
		frame.repaint();

	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		final Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(image, null, 0, 0);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setColor(selection);
		g2.fillRect(x, y, width, height);
		g2.setColor(Color.WHITE);
		g2.drawRect(x, y, width, height);
		g2.setColor(Color.GRAY);
		//Those are just offset values
		g2.drawString(String.valueOf(width), x + width + 10, y + height + 15);
		g2.drawString(String.valueOf(height), x + width + 10, y + height + 25);
		g2.dispose();
		Toolkit.getDefaultToolkit().sync();

	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON3) {
			System.exit(0);
		}
		startX = e.getX();
		startY = e.getY();

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		image = image.getSubimage(x, y, width, height);
		finished = true;

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		endX = e.getX();
		endY = e.getY();
		x = Math.min(startX, endX);
		y = Math.min(startY, endY);
		width = Math.abs(startX - endX);
		height = Math.abs(startY - endY);

	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			startX = endX;
			startY = endY;
		} else if (e.getKeyCode() == KeyEvent.VK_CONTROL
				|| e.getKeyCode() == KeyEvent.VK_ENTER
				|| e.getKeyCode() == KeyEvent.VK_F) {
			finished = true;
		} else {
			System.exit(0);
		}

	}

	@Override
	public BufferedImage call() {
		while (!finished) {
			repaint();
			try {
				Thread.sleep(10); // 1000 / 10 = 100 Frames per second
			} catch (InterruptedException e) {
				JOptionPane.showMessageDialog(null, e.getMessage());
			}
		}
		frame.dispose();
		return image;
	}

}
