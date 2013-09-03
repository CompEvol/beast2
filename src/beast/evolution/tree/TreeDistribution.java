package beast.evolution.tree;


import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.Input.Validate;
import beast.evolution.tree.coalescent.TreeIntervals;


@Description("Distribution on a tree, typically a prior such as Coalescent or Yule")
public class TreeDistribution extends Distribution {
    public Input<Tree> treeInput = new Input<Tree>("tree", "tree over which to calculate a prior or likelihood");
    public Input<TreeIntervals> treeIntervalsInput = new Input<TreeIntervals>("treeIntervals", "Intervals for a phylogenetic beast tree", Validate.XOR, treeInput);

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }

    @Override
    public void sample(State state, Random random) {
    }

    @Override
    protected boolean requiresRecalculation() {
        final TreeIntervals ti = treeIntervalsInput.get();
        if (ti != null) {
            //boolean d = ti.isDirtyCalculation();
            //assert d;
            assert ti.isDirtyCalculation();
            return true;
        }
        return treeInput.get().somethingIsDirty();
    }
    
 	/** Indicate that the tree distribution can deal with dated tips in the tree
	 * Some tree distributions like the Yule prior cannot handle this.
	 * @return true by default
	 */
	public boolean canHandleTipDates() {
		return true;
	}
}
