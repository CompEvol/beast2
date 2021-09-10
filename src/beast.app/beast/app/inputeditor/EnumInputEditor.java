package beast.app.inputeditor;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComboBox;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;



/**
 * Input editor for enumeration inputs *
 */
public class EnumInputEditor extends InputEditor.Base {
    public EnumInputEditor(BeautiDoc doc) {
		super(doc);
	}
    //public EnumInputEditor() {}

	private static final long serialVersionUID = 1L;
    JComboBox<String> m_selectPluginBox;

    @Override
    public Class<?> type() {
        return Enum.class;
    }

    /**
     * construct an editor consisting of
     * o a label
     * o a combo box for selecting another value in the enumeration
     */
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
		this.itemNr = itemNr;

        addInputLabel();
        List<String> availableValues = new ArrayList<>();
        for (int i = 0; i < input.possibleValues.length; i++) {
            availableValues.add(input.possibleValues[i].toString());
        }
        if (availableValues.size() > 1) {
            m_selectPluginBox = new JComboBox<>(availableValues.toArray(new String[0]));
            Dimension maxDim = m_selectPluginBox.getPreferredSize();
            m_selectPluginBox.setMaximumSize(maxDim);

            String selectString = input.get().toString();
            m_selectPluginBox.setSelectedItem(selectString);

            m_selectPluginBox.addActionListener(e -> {
                    String selected = (String) m_selectPluginBox.getSelectedItem();
                    try {
                    	setValue(selected);
                        //lm_input.setValue(selected, m_beastObject);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                });
            m_selectPluginBox.setToolTipText(input.getHTMLTipText());
            add(m_selectPluginBox);
            add(Box.createGlue());
        }
    } // init


} // class EnumInputEditor
