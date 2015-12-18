package beast.app.beauti;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import beast.app.draw.InputEditor;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.evolution.speciation.GeneTreeForSpeciesTreeDistribution;



public class GeneTreeForSpeciesTreeDistributionInputEditor extends InputEditor.Base {
	private static final long serialVersionUID = 1L;

	public GeneTreeForSpeciesTreeDistributionInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> type() {
		return GeneTreeForSpeciesTreeDistribution.class;
	}

	@Override
	public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
        m_bAddButtons = bAddButtons;
        m_input = input;
        m_plugin = plugin;
        this.itemNr= itemNr;
        String sID = plugin.getID();
        if (sID.contains(".t:")) {
        	sID = sID.substring(sID.indexOf(".t:") + 3);
        }
        add(new JLabel("Gene Tree " + sID));
        add(Box.createGlue());
	}
	
	static final int OTHER = 3;
	String [] sValues = new String[]{"autosomal_nuclear", "X", "Y or mitochondrial", "other"};
	Double [] fValues = new Double[]{2.0, 1.5, 0.5, -1.0};
	JComboBox m_selectPluginBox;
	
	public InputEditor createPloidyEditor() {
		InputEditor editor = new InputEditor.Base(doc) {
			@Override
			public Class<?> type() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void init(Input<?> input, BEASTInterface plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
				m_plugin = plugin;
				m_input = input;
				m_bAddButtons = bAddButtons;
				this.itemNr = itemNr;
				addInputLabel();
				
	            m_selectPluginBox = new JComboBox(sValues);
	            setSelection();
	            String sSelectString = input.get().toString();
	            m_selectPluginBox.setSelectedItem(sSelectString);

	            m_selectPluginBox.addActionListener(new ActionListener() {
	                // implements ActionListener
	                public void actionPerformed(ActionEvent e) {
	                    int i = m_selectPluginBox.getSelectedIndex();
	                    if (i == OTHER) {
	                    	setSelection();
	                    	return;
	                    }
	                    try {
	                    	setValue(fValues[i]);
	                        //lm_input.setValue(sSelected, m_plugin);
	                    } catch (Exception e1) {
	                        e1.printStackTrace();
	                    }
	                }
	            });
	            m_selectPluginBox.setToolTipText(input.getHTMLTipText());
	            add(m_selectPluginBox);
	            add(Box.createGlue());
			}

			private void setSelection() {
				Double value = (Double) m_input.get();
				m_selectPluginBox.setSelectedIndex(OTHER);
				for (int i = 0; i < fValues.length; i++) {
					if (value.equals(fValues[i])) {
						m_selectPluginBox.setSelectedIndex(i);
					}
				}
			}
			
		};
		editor.init(((GeneTreeForSpeciesTreeDistribution)m_plugin).ploidyInput, 
			m_plugin, -1, ExpandOption.FALSE, true);
		return editor;
	}
    
}
