package beast.app.draw;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.BEASTObject;



@Description("Set of plugins to represent partially finished models in GUIs")
public class PluginSet extends BEASTObject {
    public Input<List<BEASTObject>> m_plugins = new Input<List<BEASTObject>>("plugin", "set of the plugins in this collection", new ArrayList<BEASTObject>());

    @Override
    public void initAndValidate() {
    }
}
