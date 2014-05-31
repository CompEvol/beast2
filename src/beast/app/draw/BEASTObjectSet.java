package beast.app.draw;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.BEASTObject;
import beast.core.BEASTInterface;



@Description("Set of plugins to represent partially finished models in GUIs")
public class BEASTObjectSet extends BEASTObject {
    public Input<List<BEASTInterface>> m_plugins = new Input<List<BEASTInterface>>("plugin", "set of the plugins in this collection", new ArrayList<BEASTInterface>());

    @Override
    public void initAndValidate() {
    }
}
