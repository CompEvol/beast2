package beast.app.draw;


import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

/**
 * Miniature round button
 */
public class SmallButton extends JButton {
	private static final long serialVersionUID = 1L;
	/** flag to indicate the button is pressed */
	protected boolean m_bPressed = false; 

	public SmallButton(String label, boolean bIsEnabled) {
		super(label);
		//this.label = label;
		enableEvents(AWTEvent.MOUSE_EVENT_MASK);
		setBackground(new Color(128,128,255));
		setPreferredSize(new Dimension(15,15));
		setSize(15,15);
		setEnabled(bIsEnabled);
	} // c'tor

	/**
	 * paints the SmallButton
	 */
	public void paint(Graphics g) {
		//super.paint(g);
		int s=14;
		if (isEnabled()) {
			if (m_bPressed) {
				GradientPaint m_gradientPaint = new GradientPaint(new Point(0, 0), Color.WHITE, new Point(getWidth(), getHeight()), getBackground().darker().darker());
				((Graphics2D) g).setPaint(m_gradientPaint);
			} else {
				//g.setColor(getBackground());
				GradientPaint m_gradientPaint = new GradientPaint(new Point(0, 0), Color.WHITE, new Point(getWidth(), getHeight()), getBackground());
				((Graphics2D) g).setPaint(m_gradientPaint);
			}
		} else {
			g.setColor(new Color(240,240,240));
		}
		g.fillArc(0, 0, s, s, 0, 360);
		g.setColor(getBackground().darker().darker().darker());
		g.drawArc(0, 0, s, s, 0, 360);
		getBorder().paintBorder(this, g, 0,0,15,15);
		Font f = getFont();
		if (f != null) {
			FontMetrics fm = getFontMetrics(getFont());
			g.setColor(getForeground());
			g.drawString(getText(), 
					s / 2 - fm.stringWidth(getText()) / 2 +0, 
					s / 2 + fm.getMaxDescent() + 1);
		}
	} // paint


	@Override
	public void processMouseEvent(MouseEvent e) {
		switch (e.getID()) {
		case MouseEvent.MOUSE_PRESSED:
			m_bPressed = true;
			break;
		case MouseEvent.MOUSE_RELEASED:
			if (m_bPressed == true) {
				m_bPressed = false;
			}
			break;
//		case MouseEvent.MOUSE_ENTERED:
//			break;
		case MouseEvent.MOUSE_EXITED:
			if (m_bPressed == true) {
				m_bPressed = false;
			}
			break;
		}
		super.processMouseEvent(e);
		repaint();
	} // processMouseEvent

} // class SmallButton
