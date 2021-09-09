package beast.app.beauti;

import java.util.ArrayList;
import java.util.List;

import beast.app.inputeditor.BeautiConnector;
import beast.base.BEASTObject;
import beast.base.Description;
import beast.base.Input;

@Description("Dummy container for representing template fragment")
public class Fragment extends BEASTObject {
	final public Input<String> valueInput = new Input<>("value","for representing CDATA section");
	final public Input<List<BeautiConnector>> connectorsInput = new Input<>("connect", "for representing BEAUti connectors", new ArrayList<>());

	@Override
	public void initAndValidate() {
	}
}
