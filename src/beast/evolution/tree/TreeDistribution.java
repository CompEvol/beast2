package beast.evolution.tree;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.evolution.tree.coalescent.TreeIntervals;

import java.util.List;
import java.util.Random;

@Description("Distribution on a tree, typically a prior such as Coalescent or Yule")
public class TreeDistribution extends Distribution {
	public Input<Tree> m_tree = new Input<Tree>("tree", "tree over which to calculate a prior or likelihood");
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
        if( ti != null ) {
            boolean d = ti.isDirtyCalculation();   assert d;
            return d;
        }
        return m_tree.get().somethingIsDirty();
    }
}
