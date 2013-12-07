package ru.evgeny.dolgiy.mousesimulate;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MouseSimulatePanel extends JPanel implements MouseListener,
		MouseMotionListener {

	public final static int HANDLER_NO_SET = -1;

	private List<Rectangle> mAreaList = new LinkedList<Rectangle>();
	private List<Color> mColorsList = new LinkedList<Color>();
	private Color mDefaultColor = new Color(255, 255, 255, 30);
	private Color mTransparentColor = new Color(0, 0, 0, 1);

	private int mCurrentHandler = HANDLER_NO_SET;
	private Rectangle mCurrentArea = null;

	private boolean mIsMouseSimulate = false;
	private ScheduledExecutorService mClickExecutor = Executors
			.newSingleThreadScheduledExecutor();
	private Robot mBot = new Robot();

	public MouseSimulatePanel() throws AWTException {
		super();
		setBackground(mTransparentColor);
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public int newArea() {
		return newArea(null);
	}

	public int newArea(Color color) {
		if (color == null)
			color = mDefaultColor;
		Rectangle rectangle = new Rectangle();

		mAreaList.add(rectangle);
		mColorsList.add(color);

		return mAreaList.size() - 1;
	}

	public void removeArea(int handler) throws IllegalAccessException {
		if (handler < 0 || handler >= mAreaList.size())
			throw new IllegalAccessException();

		mAreaList.remove(handler);
		mColorsList.remove(handler);
		if (mCurrentHandler == handler)
			mCurrentHandler = HANDLER_NO_SET;

		repaint();
	}

	public void removeAllArea() {
		mAreaList.clear();
		mColorsList.clear();
		repaint();
	}

	public void processArea(int handler) throws IllegalAccessException {
		if (handler < 0 || handler >= mAreaList.size())
			throw new IllegalAccessException();

		mCurrentHandler = handler;
	}

	public Rectangle getAreaBoundsOnScreen(int handler)
			throws IllegalAccessException {
		if (handler < 0 || handler >= mAreaList.size())
			throw new IllegalAccessException();

		Rectangle area = mAreaList.get(handler);
		Rectangle bounds = new Rectangle();

		Point location = new Point();
		location.x = getLocationOnScreen().x + area.getLocation().x;
		location.y = getLocationOnScreen().y + area.getLocation().y;

		bounds.setLocation(location);
		bounds.setSize(area.getSize());
		return bounds;
	}

	public void cancelProcessing() {
		mCurrentHandler = HANDLER_NO_SET;
	}

	public void executeMouseEvents(List<MouseSimulateEvent> eventsSequence) {

		if (eventsSequence == null)
			return;

		Runnable clearTask = new Runnable() {
			@Override
			public void run() {
				mIsMouseSimulate = true;
				repaint();
			}
		};

		for (int i = 0; i < eventsSequence.size(); i++) {
			MouseSimulateEvent event = eventsSequence.get(i);

			MouseTask task = new MouseTask(event);
			mClickExecutor.schedule(clearTask, event.delay,
					TimeUnit.MILLISECONDS);
			mClickExecutor.schedule(task, event.delay, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
		mCurrentArea = null;
		mCurrentHandler = HANDLER_NO_SET;
	}

	@Override
	public void mousePressed(MouseEvent e) {

		if (mCurrentHandler == HANDLER_NO_SET || mCurrentHandler < 0)
			return;

		if (mCurrentArea == null) {
			mCurrentArea = mAreaList.get(mCurrentHandler);
		}

		mCurrentArea.setLocation(e.getPoint());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mCurrentArea = null;
		mCurrentHandler = HANDLER_NO_SET;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (mCurrentHandler == HANDLER_NO_SET || mCurrentHandler < 0)
			return;

		if (mCurrentArea != null) {
			Point p = e.getPoint();
			int width = Math.max(mCurrentArea.x - e.getX(), e.getX()
					- mCurrentArea.x);
			int height = Math.max(mCurrentArea.y - e.getY(), e.getY()
					- mCurrentArea.y);
			mCurrentArea.setSize(width, height);
			repaint();
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}

	@Override
	protected void paintComponent(Graphics g) {
		if (g instanceof Graphics2D) {
			Graphics2D g2d = (Graphics2D) g;
			if (mIsMouseSimulate) {
				g2d.setBackground(new Color(0, 0, 0, 0));
				g2d.clearRect(0, 0, getWidth(), getHeight());
				System.out.println("clear");
			} else {
				System.out.println("restore");
				g2d.setBackground(mTransparentColor);
				g2d.clearRect(0, 0, getWidth(), getHeight());

				if (mAreaList.size() == mColorsList.size()) {
					for (int i = 0; i < mAreaList.size(); i++) {

						Rectangle area = mAreaList.get(i);
						Color color = mColorsList.get(i);

						g2d.setPaint(color);
						g2d.fill(area);
						g2d.setPaint(Color.GRAY);
						g2d.draw(area);
					}
				}
			}
			g2d.dispose();
		}
	}

	private class MouseTask implements Runnable {

		private MouseSimulateEvent mEvent;

		public MouseTask(MouseSimulateEvent event) {
			mEvent = event;
		}

		public void run() {

			if (mEvent == null || mEvent.action == null)
				return;

			switch (mEvent.action) {
			case MOVE:
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						moveMouse(mEvent.x, mEvent.y);
					}
				});
				break;
			case CLICK:
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						clickMouse(mEvent.x, mEvent.y);
					}
				});
				break;
			case NONE:
			default:
				break;
			}

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					mIsMouseSimulate = false;
					repaint();
				}
			});
		}

		private void moveMouse(int x, int y) {
			mBot.mouseMove(x, y);
		}

		private void clickMouse(int x, int y) {
			mBot.mouseMove(x, y);
			mBot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			mBot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		}
	}

	public static class MouseSimulateEvent {

		public enum Action {
			NONE, MOVE, CLICK
		};

		public Action action = Action.NONE;
		public int x = 0;
		public int y = 0;
		public long delay = 0;
	}

}
