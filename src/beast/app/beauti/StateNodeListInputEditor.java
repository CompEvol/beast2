package beast.app.beauti;

import java.util.List;

import beast.app.draw.ListInputEditor;
import beast.core.BEASTInterface;
import beast.core.Input;
import beast.core.StateNode;



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
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
		m_buttonStatus = ButtonStatus.NONE;
		super.init(input, beastObject, itemNr, bExpandOption, bAddButtons);
	}

}
