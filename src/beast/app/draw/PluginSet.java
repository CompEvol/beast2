package beast.app.draw;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;

@Description("Set of plugins to represent partially finished models in GUIs")
public class PluginSet extends Plugin {
    public Input<List<Plugin>> m_plugins = new Input<List<Plugin>>("plugin", "set of the plugins in this collection", new ArrayList<Plugin>());

    @Override
    public void initAndValidate() {
    }
}
