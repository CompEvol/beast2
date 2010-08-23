package beast.app.draw;

import javax.swing.AbstractListModel;
import javax.swing.JTable;

public class TableRowHeaderModel extends AbstractListModel {
	private static final long serialVersionUID = 1L;
	
	private JTable table;

	public TableRowHeaderModel(JTable table) {
		this.table = table;
	}

	public int getSize() {
		return table.getRowCount();
	}

	public Object getElementAt(int index) {
		return null;
	}
}
