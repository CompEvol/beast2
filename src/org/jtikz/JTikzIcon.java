package org.jtikz;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

/**
 * Class for painting the JTikZ logo.
 *
 * @author <a href="http://www.sultanik.com/" target="_blank">Evan Sultanik</a>
 */
public class JTikzIcon extends BufferedImage
    implements Icon
{
    Font serif;
    Font italic;
    int w1, w2, w3, stringHeight, maxDescent;

    /**
     * Constructs a new MATES icon of a specific width and height.
     */
    public JTikzIcon(int width, int height)
    {
	super(width, height, TYPE_INT_RGB);
        /* set the proper font widths */
        this.serif = null;
        this.italic = null;
        Graphics g = createGraphics();
        for(int size = 1;;size++) {
            Font serif = new Font("Serif", Font.PLAIN, size);
            FontMetrics sm = g.getFontMetrics(serif);
            Font italic = new Font("Serif", Font.ITALIC, size);
            FontMetrics im = g.getFontMetrics(italic);
            int w1 = sm.stringWidth("JTi");
            int w2 = im.stringWidth("k");
            int w3 = sm.stringWidth("Z");
            if(w1 + w2 + w3 > width && this.serif != null)
                break;
            this.serif = serif;
            this.italic = italic;
            this.w1 = w1;
            this.w2 = w2;
            this.w3 = w3;
            stringHeight = Math.max(sm.getHeight(), im.getHeight());
            maxDescent = Math.max(sm.getMaxDescent(), im.getMaxDescent());
        }
	paint(g);
    }

    /**
     * Draws this icon to the given <code>graphics</code> object, with
     * an offset position of (0, 0).  The dimensions of the drawn icon
     * will be equal to those provided when this icon was constructed.
     */
    public void paint(Graphics graphics)
    {
	paintIcon(null, graphics, 0, 0);
    }

    public int getIconWidth()
    {
	return getWidth();
    }

    public int getIconHeight()
    {
	return getHeight();
    }

    private int round(double n)
    {
	return (int)(n + 0.5);
    }

    public void paintIcon(Component component, Graphics graphics, int x, int y)
    {
	int height = getHeight();
	int width = getWidth() - 1;
        int stringWidth = w1 + w2 + w3;

	if(graphics instanceof Graphics2D) {
	    Graphics2D g2d = (Graphics2D)graphics;
	    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    //g2d.setStroke(new BasicStroke((float)(quarter / 10.0)));
	}

	graphics.setColor(Color.WHITE);
	graphics.fillRect(x, y, width, height);

        graphics.setColor(Color.BLACK);
        graphics.setFont(serif);
        graphics.drawString("JTi", x + (width - stringWidth) / 2, y - maxDescent + height - (height - stringHeight) / 2);
        graphics.drawString("Z", x + (width - stringWidth) / 2 + w1 + w2, y - maxDescent + height - (height - stringHeight) / 2);
        graphics.setFont(italic);
        graphics.drawString("k", x + (width - stringWidth) / 2 + w1, y - maxDescent + height - (height - stringHeight) / 2);
    }

    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame("Test!");
        frame.getContentPane().setPreferredSize(new Dimension(640, 480));
        frame.getContentPane().add(new JLabel(new JTikzIcon(32, 32)));
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
