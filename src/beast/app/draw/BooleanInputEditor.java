package beast.app.draw;

import javax.swing.Box;
import javax.swing.JCheckBox;

import beast.app.beauti.BeautiDoc;
import beast.core.BEASTInterface;
import beast.core.Input;



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
    public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
        m_bAddButtons = bAddButtons;
        m_plugin = plugin;
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
                    validateInput();
                    //m_input.setValue(m_entry.isSelected(), m_plugin);
                } catch (Exception ex) {
                    System.err.println("BooleanInputEditor " + ex.getMessage());
                }
            });
        add(m_entry);
        add(Box.createHorizontalGlue());
    } // c'tor

} // class BooleanInputEditor
