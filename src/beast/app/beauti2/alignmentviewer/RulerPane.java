package beast.app.beauti2.alignmentviewer;

import javax.swing.*;
import java.awt.*;

/**
 * @author Andrew Rambaut
 * @version $Id: RulerPane.java,v 1.1 2005/11/01 23:52:04 rambaut Exp $
 */
public class RulerPane extends JComponent {
    private AlignmentBuffer alignment = null;
    private ColumnDecorator columnDecorator = null;
    private int columnCount = 0;

    private int colWidth = 0;
    private int ascent = 0;

    public int getColumnWidth() {

        return colWidth;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public RulerPane() {
        setFont( new Font("sansserif", Font.PLAIN, 10));
    }

    public void setAlignmentBuffer(AlignmentBuffer alignment) {
        this.alignment = alignment;

        if (alignment != null) {
            columnCount = alignment.getSiteCount();
        } else {
            columnCount = 0;
        }

        FontMetrics fm = getFontMetrics(getFont());
        ascent = fm.getAscent();

        setup();
    }

    public void setColumnWidth(int columnWidth) {
        this.colWidth = columnWidth;
        setup();
    }

    public void setColumnDecorator(ColumnDecorator columnDecorator) {
        this.columnDecorator = columnDecorator;
    }

    private void setup() {
        setPreferredSize(new Dimension(colWidth * columnCount, 16));
        invalidate();
    }

    public void paint(Graphics graphics) {
        if (alignment == null) return;

        Graphics2D g2 = (Graphics2D)graphics;

        int col1 = 0;
        int col2 = columnCount - 1;

        int y1 = 0;
        int y2 = getHeight();

        int y = ascent;

        if (columnDecorator != null) {
            int x = 0;
            for (int col = col1; col <= col2; col++) {
                Paint columnBackground = columnDecorator.getColumnBackground(col);
                if (columnBackground != null) {
                    g2.setPaint(columnBackground);
                    g2.fillRect(x, y1, colWidth, y2);
                }
                x += colWidth;
            }
        }

        g2.setPaint(Color.BLACK);

        int x = 0;
        if (col1 == 0) {
            g2.drawString("1", x, y);
            g2.drawLine(x, y2, x, y + 1);
            x += colWidth;
            col1++;
        }

        for (int col = col1; col <= col2; col++) {
            if ((col + 1) % 5 == 0) {
                g2.drawString(Integer.toString(col + 1), x, y);
                g2.drawLine(x, y2, x, y + 1);
            } else {
                g2.drawLine(x, y2, x, y + 4);
            }
            x += colWidth;
        }
    }

}
