package beast.app.draw;

import java.awt.Dimension;

public class StringInputEditor extends InputEditor {
	private static final long serialVersionUID = 1L;

	public StringInputEditor() {
		super();
	}
	
	@Override
	public Class<?> type() {return String.class;}


	@Override
	void setUpEntry() {
		super.setUpEntry();
		Dimension size = new Dimension(200,20);
		m_entry.setMinimumSize(size);
		m_entry.setPreferredSize(size);
		m_entry.setSize(size);
	}

} // class StringInputEditor
