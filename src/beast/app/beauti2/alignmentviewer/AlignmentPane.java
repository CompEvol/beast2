package beast.app.beauti2.alignmentviewer;

import javax.swing.*;
import java.awt.*;

/**
 * @author Andrew Rambaut
 * @version $Id: AlignmentPane.java,v 1.2 2005/11/11 16:40:41 rambaut Exp $
 */
public class AlignmentPane extends JComponent implements Scrollable {
	private final TaxonPane taxonPane;
    private final RulerPane rulerPane;

    private AlignmentBuffer alignment = null;
    private int rowCount = 0;
    private int columnCount = 0;
    private int rowHeight = 0;
    private int columnWidth = 0;
    private int ascent = 0;

	private int col1 = 0;
    private int col2 = 0;
    private int row1 = 0;
    private int row2 = 0;

    private RowDecorator rowDecorator = null;
    private ColumnDecorator columnDecorator = null;
    private CellDecorator cellDecorator = null;

    public AlignmentPane(TaxonPane taxonPane, RulerPane rulerPane) {
        this.taxonPane = taxonPane;
        this.rulerPane = rulerPane;
        setFont( new Font("monospaced", Font.PLAIN, 12));
    }

    public void setRowDecorator(RowDecorator rowDecorator) {
        this.rowDecorator = rowDecorator;
    }

    public void setColumnDecorator(ColumnDecorator columnDecorator) {
        this.columnDecorator = columnDecorator;
    }

    public void setCellDecorator(CellDecorator cellDecorator) {
        this.cellDecorator = cellDecorator;
    }

    public void setAlignmentBuffer(AlignmentBuffer alignment) {
        this.alignment = alignment;

        if (alignment != null) {
            rowCount = alignment.getSequenceCount();
            columnCount = alignment.getSiteCount();
        } else {
            rowCount = 0;
            columnCount = 0;

        }

        FontMetrics fm = getFontMetrics(getFont());
        rowHeight = fm.getHeight();
        columnWidth = fm.charWidth('M');
        ascent = fm.getAscent();

        if (taxonPane != null) {
            taxonPane.setRowHeight(rowHeight);
        }
        rulerPane.setColumnWidth(columnWidth);

        setPreferredSize(new Dimension(columnWidth * columnCount, rowHeight * rowCount));
        invalidate();
    }

	public Rectangle getVisibleArea() {
		return new Rectangle(col1, row1, col2 - col1 + 1, row2 - row1 + 1);
	}

	public void setTopRow(int row) {
		Rectangle rect = getVisibleRect();
		rect.y = getTop(row);
		scrollRectToVisible(rect);
	}

	public void setLeftColumn(int col) {
		Rectangle rect = getVisibleRect();
		rect.x = getLeft(col);
		scrollRectToVisible(rect);
	}

	public void setCentreColumn(int col) {
		Rectangle rect = getVisibleRect();
		rect.x = Math.max(0, (getLeft(col) + getLeft(col) + columnWidth - rect.width) / 2);
		scrollRectToVisible(rect);
	}

	public void setRightColumn(int col) {
		Rectangle rect = getVisibleRect();
		rect.x = Math.max(0, getLeft(col) + columnWidth - rect.width);
		scrollRectToVisible(rect);
	}

    public void paint(Graphics graphics) {
        if (alignment == null) return;

        Graphics2D g2 = (Graphics2D)graphics;

        String[] stateTable = alignment.getStateTable();

        Rectangle bounds = g2.getClipBounds();
        col1 = Math.min(getColumn(bounds.x), columnCount - 1);
        col2 = Math.min(getColumn(bounds.x + bounds.width), columnCount - 1);
        row1 = Math.min(getRow(bounds.y), rowCount - 1);
        row2 = Math.min(getRow(bounds.y + bounds.height), rowCount - 1);

        int x1 = getLeft(col1);
        int x2 = getLeft(col2 + 1);
        int y1 = getTop(row1);
        int y2 = getTop(row2 + 1);

        byte[] seq = new byte[columnCount];

        int y = y1;
        if (rowDecorator != null) {
            for (int row = row1; row <= row2; row++) {
                Paint rowBackground = rowDecorator.getRowBackground(row);
                if (rowBackground != null) {
                    g2.setPaint(rowBackground);
                    Rectangle rect = new Rectangle(x1, y, x2 - x1, rowHeight);
                    g2.fill(rect);
                }
                y += rowHeight;
            }
        }

        int x = x1;
//        if (columnDecorator != null) {
//            for (int col = col1; col <= col2; col++) {
//                Paint columnBackground = columnDecorator.getColumnBackground(col);
//                if (columnBackground != null) {
//                    g2.setPaint(columnBackground);
//                    Rectangle rect = new Rectangle(x, y1, columnWidth, y2 - y1);
//                    g2.fill(rect);
//                }
//                x += columnWidth;
//            }
//        }

        y = y1;
        for (int row = row1; row <= row2; row++) {
            alignment.getStates(row, col1, col2, seq);

            if (cellDecorator != null) {
                x = x1;
                int i = 0;
                for (int col = col1; col <= col2; col++) {

                    g2.setPaint(cellDecorator.getCellBackground(row, col, seq[i]));
                    Rectangle rect = new Rectangle(x, y, x + columnWidth, y + rowHeight);
                    g2.fill(rect);

                    g2.setPaint(cellDecorator.getCellForeground(row, col, seq[i]));
                    g2.drawString(stateTable[seq[i]], x, y + ascent);

                    x += columnWidth;
                    i++;
                }
            } else {
                g2.setPaint(Color.BLACK);
                x = x1;
                int i = 0;
                for (int col = col1; col <= col2; col++) {
                    g2.drawString(stateTable[seq[i]], x, y + ascent);

                    x += columnWidth;
                    i++;
                }
            }
            y += rowHeight;
        }

	    x = x1;
	    if (columnDecorator != null) {
	        for (int col = col1; col <= col2; col++) {
	            Paint columnBackground = columnDecorator.getColumnBackground(col);
	            if (columnBackground != null) {
	                g2.setPaint(columnBackground);
	                Rectangle rect = new Rectangle(x, y1, columnWidth, y2 - y1);
	                g2.fill(rect);
	            }
	            x += columnWidth;
	        }
	    }

    }

    public int getColumn(int x) {
        return x / columnWidth;
    }

    public int getLeft(int column) {
        return column * columnWidth;
    }

    public int getColumnWidth() {
        return columnWidth;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public int getRow(int y) {
        return y / rowHeight;
    }

    public int getTop(int row) {
        return row * rowHeight;
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public int getRowCount() {
        return rowCount;
    }

    public Dimension getPreferredSize() {
        return new Dimension(columnWidth * columnCount, rowHeight * rowCount);
    }

   public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return columnWidth;
        } else {
            return rowHeight;
        }
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.HORIZONTAL) {
            return columnWidth * 10;
        } else {
            return rowHeight * 10;
        }
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
