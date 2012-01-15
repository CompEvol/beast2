package beast.evolution.speciation;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.evolution.tree.Tree;

import java.util.ArrayList;
import java.util.List;

@Description("Finds height of highest tree among a set of trees")
public class TreeTopFinder extends CalculationNode {
    public Input<List<Tree>> treeInputs = new Input<List<Tree>>("tree", "set of trees to search among", new ArrayList<Tree>());

    List<Tree> trees;

    double fOldHeight;
    double fHeight;

    public void initAndValidate() throws Exception {
        fOldHeight = Double.NaN;
        trees = treeInputs.get();
        fHeight = calcHighestTreeHeight();
    }

    public double getHighestTreeHeight() {
        return calcHighestTreeHeight();
    }

    private double calcHighestTreeHeight() {
        double fTop = 0;
        for (Tree tree : trees) {
            fTop = Math.max(tree.getRoot().getHeight(), fTop);
        }
        return fTop;
    }

    @Override
    protected boolean requiresRecalculation() {
        double fTop = calcHighestTreeHeight();
        if (fTop != fHeight) {
            fHeight = fTop;
            return true;
        }
        return false;
    }

    @Override
    protected void store() {
        fOldHeight = fHeight;
        super.store();
    }

    @Override
    protected void restore() {
        fHeight = fOldHeight;
        super.restore();
    }
}
