package beast.app.beauti;

import java.util.List;

import beast.app.inputeditor.BeautiDoc;
import beast.app.inputeditor.ListInputEditor;
import beast.base.BEASTInterface;
import beast.base.Input;
import beast.inference.StateNode;



public class StateNodeListInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;

	public StateNodeListInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
	@Override
	public Class<?> type() {
		return List.class;
	}
	
	@Override
	public Class<?> baseType() {
		return StateNode.class;
	}
	
	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
		m_buttonStatus = ButtonStatus.NONE;
		super.init(input, beastObject, itemNr, isExpandOption, addButtons);
	}

}
