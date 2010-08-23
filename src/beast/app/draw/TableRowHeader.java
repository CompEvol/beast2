package beast.app.draw;

import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

public class TableRowHeader extends JList {
	private static final long serialVersionUID = 1L;
	private JTable table;

	public TableRowHeader(JTable table) {
		super(new TableRowHeaderModel(table));
		this.table = table;
		setFixedCellHeight(table.getRowHeight());
		setFixedCellWidth(preferredHeaderWidth());
		setCellRenderer(new RowHeaderRenderer(table));
		setSelectionModel(table.getSelectionModel());
	}

	/**
	 * * Returns the bounds of the specified range of items in JList *
	 * coordinates. Returns null if index isn't valid. * * @param index0 the
	 * index of the first JList cell in the range * @param index1 the index
	 * of the last JList cell in the range * @return the bounds of the
	 * indexed cells in pixels
	 */
	public Rectangle getCellBounds(int index0, int index1) {
		Rectangle rect0 = table.getCellRect(index0, 0, true);
		Rectangle rect1 = table.getCellRect(index1, 0, true);
		int y, height;
		if (rect0.y < rect1.y) {
			y = rect0.y;
			height = rect1.y + rect1.height - y;
		} else {
			y = rect1.y;
			height = rect0.y + rect0.height - y;
		}
		return new Rectangle(0, y, getFixedCellWidth(), height);
	}

	// assume that row header width should be big enough to display row
	// number
	// Integer.MAX_VALUE completely
	private int preferredHeaderWidth() {
		JLabel longestRowLabel = new JLabel("65356");
		JTableHeader header = table.getTableHeader();
		longestRowLabel.setBorder(header.getBorder());// UIManager.getBorder("TableHeader.cellBorder"));
		longestRowLabel.setHorizontalAlignment(JLabel.CENTER);
		longestRowLabel.setFont(header.getFont());
		return longestRowLabel.getPreferredSize().width;
	}

}
