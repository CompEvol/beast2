package beast.app.draw;

import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;

public class RowHeaderRenderer extends JLabel implements ListCellRenderer {
	private static final long serialVersionUID = 1L;
	private JTable table;
	private Border selectedBorder;
	private Border normalBorder;
	private Font selectedFont;
	private Font normalFont;

	RowHeaderRenderer(JTable table) {
		this.table = table;
		normalBorder = UIManager.getBorder("TableHeader.cellBorder");
		selectedBorder = BorderFactory.createRaisedBevelBorder();
		final JTableHeader header = table.getTableHeader();
		normalFont = header.getFont();
		selectedFont = normalFont.deriveFont(normalFont.getStyle()
				| Font.BOLD);
		setForeground(header.getForeground());
		setBackground(header.getBackground());
		setOpaque(true);
		setHorizontalAlignment(CENTER);
	}

	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		if (table.getSelectionModel().isSelectedIndex(index)) {
			setFont(selectedFont);
			setBorder(selectedBorder);
		} else {
			setFont(normalFont);
			setBorder(normalBorder);
		}
		String label = String.valueOf(index + 1);
		setText(label);
		return this;
	}
}
