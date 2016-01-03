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

import javax.swing.JLabel;

/**
 * Miniature round label
 */
public class SmallLabel extends JLabel {
    private static final long serialVersionUID = 1L;
    public Color m_circleColor = Color.blue;

    public SmallLabel(String label, Color circleColor) {
        super(label);
        //this.label = label;
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        setBackground(new Color(208, 208, 255));
        setPreferredSize(new Dimension(15, 15));
        setSize(15, 15);
        setMinimumSize(new Dimension(15, 15));
        setMaximumSize(new Dimension(15, 15));
        m_circleColor = circleColor;
        super.setVisible(true);
    } // c'tor

    /**
     * paints the SmallButton
     */
    @Override
	public void paint(Graphics g) {
        if (m_bIsEnabled) {
            int s = 14;
            GradientPaint m_gradientPaint = new GradientPaint(new Point(0, 0), Color.WHITE, new Point(getWidth(), getHeight()), m_circleColor);
            ((Graphics2D) g).setPaint(m_gradientPaint);
            //g.setColor(m_circleColor);
            g.fillArc(0, 0, s, s, 0, 360);
            g.setColor(getBackground().darker().darker().darker());
            g.drawArc(0, 0, s, s, 0, 360);
            Font f = getFont();
            if (f != null) {
                FontMetrics fm = getFontMetrics(getFont());
                g.setColor(getForeground());
                g.drawString(getText(),
                        s / 2 - fm.stringWidth(getText()) / 2 + 0,
                        s / 2 + fm.getMaxDescent() + 1);
            }
        }
    } // paint

    boolean m_bIsEnabled = true;

    @Override
    public void setVisible(boolean bIsEnabled) {
        m_bIsEnabled = bIsEnabled;
    }

} // class SmallButton
