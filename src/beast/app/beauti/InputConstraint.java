package beast.app.beauti;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;

@Description("allows the specification of a limited set of candidate plugins for particular Plugins")
public class InputConstraint extends Plugin {
	public Input<List<Plugin>> m_plugins = new Input<List<Plugin>>("plugin","one or more plugins for which to limit potential inputs", new ArrayList<Plugin>());
	public Input<String> m_inputName = new Input<String>("inputname","name of the input");
	public Input<List<Plugin>> m_candidates = new Input<List<Plugin>>("candidate","candidate plugin, one of these must be chosen as input", new ArrayList<Plugin>());

	@Override
	public void initAndValidate() throws Exception { 
		// nothing to do
	}
}
