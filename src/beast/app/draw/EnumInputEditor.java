package beast.app.draw;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComboBox;

import beast.app.beauti.BeautiDoc;
import beast.core.Input;
import beast.core.Plugin;

/**
 * Input editor for enumeration inputs *
 */
public class EnumInputEditor extends InputEditor.Base {
    public EnumInputEditor(BeautiDoc doc) {
		super(doc);
	}
    //public EnumInputEditor() {}

	private static final long serialVersionUID = 1L;
    JComboBox m_selectPluginBox;

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
    public void init(Input<?> input, Plugin plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
        m_bAddButtons = bAddButtons;
        m_input = input;
        m_plugin = plugin;
		this.itemNr = itemNr;

        addInputLabel();
        List<String> sAvailableValues = new ArrayList<String>();
        for (int i = 0; i < input.possibleValues.length; i++) {
            sAvailableValues.add(input.possibleValues[i].toString());
        }
        if (sAvailableValues.size() > 1) {
            m_selectPluginBox = new JComboBox(sAvailableValues.toArray(new String[0]));
            String sSelectString = input.get().toString();
            m_selectPluginBox.setSelectedItem(sSelectString);

            m_selectPluginBox.addActionListener(new ActionListener() {
                // implements ActionListener
                public void actionPerformed(ActionEvent e) {
                    String sSelected = (String) m_selectPluginBox.getSelectedItem();
                    try {
                    	setValue(sSelected);
                        //lm_input.setValue(sSelected, m_plugin);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            });
            m_selectPluginBox.setToolTipText(input.getTipText());
            add(m_selectPluginBox);
            add(Box.createGlue());
        }
    } // init


} // class EnumInputEditor
