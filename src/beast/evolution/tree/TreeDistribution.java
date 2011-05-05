package beast.evolution.tree;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.evolution.tree.coalescent.TreeIntervals;

import java.util.List;
import java.util.Random;

@Description("Prior on a tree, such as Coalescent or Yule")
public class TreeDistribution extends Distribution {
	public Input<Tree> m_tree = new Input<Tree>("tree", "species tree over which to calculate speciation likelihood");
    public Input<TreeIntervals> treeIntervals = new Input<TreeIntervals>("treeIntervals", "Intervals for a phylogenetic beast tree", Validate.XOR, m_tree);

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
        final TreeIntervals ti = treeIntervals.get();
        return (ti != null && ti.isDirtyCalculation()) || m_tree.get().somethingIsDirty();
    }
}
