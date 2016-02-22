package beast.evolution.speciation;


import java.util.ArrayList;
import java.util.List;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.evolution.tree.Tree;


@Description("Finds height of highest tree among a set of trees")
public class TreeTopFinder extends CalculationNode {
    final public Input<List<Tree>> treeInputs = new Input<>("tree", "set of trees to search among", new ArrayList<>());

    List<Tree> trees;

    double oldHeight;
    double height;

    @Override
	public void initAndValidate() {
        oldHeight = Double.NaN;
        trees = treeInputs.get();
        height = calcHighestTreeHeight();
    }

    public double getHighestTreeHeight() {
        return calcHighestTreeHeight();
    }

    private double calcHighestTreeHeight() {
        double top = 0;
        for (Tree tree : trees) {
            top = Math.max(tree.getRoot().getHeight(), top);
        }
        return top;
    }

    @Override
    protected boolean requiresRecalculation() {
        double top = calcHighestTreeHeight();
        if (top != height) {
            height = top;
            return true;
        }
        return false;
    }

    @Override
    protected void store() {
        oldHeight = height;
        super.store();
    }

    @Override
    protected void restore() {
        height = oldHeight;
        super.restore();
    }
}
