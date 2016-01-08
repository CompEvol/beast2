package beast.app.beauti;

import javax.swing.Box;
import javax.swing.JComponent;

import beast.app.draw.BEASTObjectInputEditor;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.evolution.speciation.SpeciesTreePrior;



public class SpeciesTreePriorInputEditor extends BEASTObjectInputEditor {
	private static final long serialVersionUID = 1L;

	public SpeciesTreePriorInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> type() {
		return SpeciesTreePrior.class;
	}
	
	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
		super.init(input, beastObject, itemNr, isExpandOption, addButtons);
	}

    @Override
	protected void addComboBox(JComponent box, Input<?> input, BEASTInterface beastObject) {
    	m_bAddButtons = true;
    	String label = "Species Tree Population Size";
    	addInputLabel(label, label);
    	m_bAddButtons = false;
    	add(Box.createHorizontalGlue());
    }
}
