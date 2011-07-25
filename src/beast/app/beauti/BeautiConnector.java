package beast.app.beauti;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;

@Description("Specifies which part of the template get connected to the main network")
public class BeautiConnector extends Plugin {
	public Input<String> sSourceID = new Input<String>("srcID","ID of the plugin to be connected", Validate.REQUIRED);
	public Input<String> sTargetID = new Input<String>("targetID","ID of plugin to connect to", Validate.REQUIRED);
	public Input<String> sInputName = new Input<String>("inputName","name of the input of the plugin to connect to", Validate.REQUIRED);

	@Override
	public void initAndValidate() throws Exception {
		// nothing to do
	}
}
