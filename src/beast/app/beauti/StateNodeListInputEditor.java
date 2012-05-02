package beast.app.beauti;

import java.util.List;

import beast.app.draw.ListInputEditor;
import beast.core.Input;
import beast.core.Plugin;
import beast.core.StateNode;

public class StateNodeListInputEditor extends ListInputEditor {
	private static final long serialVersionUID = 1L;

	public StateNodeListInputEditor(BeautiDoc doc) {
		super(doc);
		// TODO Auto-generated constructor stub
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
	public void init(Input<?> input, Plugin plugin, int itemNr, ExpandOption bExpandOption, boolean bAddButtons) {
		m_buttonStatus = ButtonStatus.NONE;
		super.init(input, plugin, itemNr, bExpandOption, bAddButtons);
	}

}
