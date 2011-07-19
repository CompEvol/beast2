package beast.app.beauti;

import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;

@Description("allows specification of unparsed XML fragments in Beauti templates")
public class XML extends Plugin {
	public Input<String> m_sValue = new Input<String>("value","xml fragment in CDATA block");

	@Override
	public void initAndValidate() throws Exception {
	}
}
