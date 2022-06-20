package beast.base.evolution.tree;


import java.util.List;
import java.util.Random;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Distribution;
import beast.base.inference.State;


@Description("Distribution on a tree, typically a prior such as Coalescent or Yule")
public class TreeDistribution extends Distribution {
    final public Input<TreeInterface> treeInput = new Input<>("tree", "tree over which to calculate a prior or likelihood");
    final public Input<TreeIntervals> treeIntervalsInput = new Input<>("treeIntervals", "Intervals for a phylogenetic beast tree", Validate.XOR, treeInput);

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
