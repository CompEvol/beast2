package beast.app.beauti;

import java.util.ArrayList;
import java.util.List;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;

@Description("Dummy container for representing template fragment")
public class Fragment extends BEASTObject {
	public Input<String> valueInput = new Input<String>("value","for representing CDATA section");
	public Input<List<BeautiConnector>> connectorsInput = new Input<List<BeautiConnector>>("connect", "for representing BEAUti connectors", new ArrayList<>());

	@Override
	public void initAndValidate() throws Exception {
	}
}
