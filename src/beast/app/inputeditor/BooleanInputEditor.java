package beast.app.inputeditor;

import javax.swing.Box;
import javax.swing.JCheckBox;

import beast.base.BEASTInterface;
import beast.base.Input;
import beast.base.Log;



public class BooleanInputEditor extends InputEditor.Base {
    public BooleanInputEditor(BeautiDoc doc) {
		super(doc);
	}
    //public BooleanInputEditor() {}

	private static final long serialVersionUID = 1L;
    JCheckBox m_entry;


    @Override
    public Class<?> type() {
        return Boolean.class;
    }

    /**
     * create input editor containing a check box *
     */
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_beastObject = beastObject;
        m_input = input;
		this.itemNr = itemNr;
        m_entry = new JCheckBox(formatName(m_input.getName()));
        if (input.get() != null) {
            m_entry.setSelected((Boolean) input.get());
        }
        m_entry.setToolTipText(input.getHTMLTipText());
        m_entry.addActionListener(e -> {
                try {
                	setValue(m_entry.isSelected());
                	refreshPanel();
                    //validateInput();
                    //m_input.setValue(m_entry.isSelected(), m_beastObject);
                } catch (Exception ex) {
                    Log.err.println("BooleanInputEditor " + ex.getMessage());
                }
            });
        add(m_entry);
        add(Box.createHorizontalGlue());
    } // c'tor

} // class BooleanInputEditor
