package beast.app.beauti;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import beast.app.inputeditor.BeautiDoc;
import beast.app.inputeditor.InputEditor;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.speciation.GeneTreeForSpeciesTreeDistribution;



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
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr= itemNr;
        String id = beastObject.getID();
        if (id.contains(".t:")) {
        	id = id.substring(id.indexOf(".t:") + 3);
        }
        add(new JLabel("Gene Tree " + id));
        add(Box.createGlue());
	}
	
	static final int OTHER = 3;
	String [] valuesString = new String[]{"autosomal_nuclear", "X", "Y or mitochondrial", "other"};
	Double [] _values = new Double[]{2.0, 1.5, 0.5, -1.0};
	JComboBox<String> m_selectBeastObjectBox;
	
	public InputEditor createPloidyEditor() {
		InputEditor editor = new InputEditor.Base(doc) {
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> type() {
				return null;
			}
			
			@Override
			public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
				m_beastObject = beastObject;
				m_input = input;
				m_bAddButtons = addButtons;
				this.itemNr = itemNr;
				addInputLabel();
				
	            m_selectBeastObjectBox = new JComboBox<>(valuesString);
	            setSelection();
	            String selectString = input.get().toString();
	            m_selectBeastObjectBox.setSelectedItem(selectString);

	            m_selectBeastObjectBox.addActionListener(e -> {
	                    int i = m_selectBeastObjectBox.getSelectedIndex();
	                    if (i == OTHER) {
	                    	setSelection();
	                    	return;
	                    }
	                    try {
	                    	setValue(_values[i]);
	                        //lm_input.setValue(selected, m_beastObject);
	                    } catch (Exception e1) {
	                        e1.printStackTrace();
	                    }
	                });
	            m_selectBeastObjectBox.setToolTipText(input.getHTMLTipText());
	            add(m_selectBeastObjectBox);
	            add(Box.createGlue());
			}

			private void setSelection() {
				Double value = (Double) m_input.get();
				m_selectBeastObjectBox.setSelectedIndex(OTHER);
				for (int i = 0; i < _values.length; i++) {
					if (value.equals(_values[i])) {
						m_selectBeastObjectBox.setSelectedIndex(i);
					}
				}
			}
			
		};
		editor.init(((GeneTreeForSpeciesTreeDistribution)m_beastObject).ploidyInput, 
			m_beastObject, -1, ExpandOption.FALSE, true);
		return editor;
	}
    
}
