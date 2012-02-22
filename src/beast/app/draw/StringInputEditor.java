package beast.app.draw;

import beast.app.beauti.BeautiDoc;

public class StringInputEditor extends InputEditor.Base {
    private static final long serialVersionUID = 1L;

    //public StringInputEditor()) {}
    public StringInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public Class<?> type() {
        return String.class;
    }


    @Override
    void setUpEntry() {
        super.setUpEntry();
        //Dimension size = new Dimension(200,20);
        //m_entry.setMinimumSize(size);
//		m_entry.setPreferredSize(size);
//		m_entry.setSize(size);
    }

} // class StringInputEditor
