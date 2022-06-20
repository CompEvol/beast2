package beast.base.evolution.speciation;

import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.coalescent.PopulationFunction;
import beast.base.evolution.tree.coalescent.RandomTree;



@Description("Generates a random gene tree conditioned on a species tree, such " +
        "that the root of the species tree is lower than any coalescent events in " +
        "the gene tree")
public class RandomGeneTree extends RandomTree {
    final public Input<Tree> speciesTreeInput = new Input<>("speciesTree", "The species tree in which this random gene tree needs to fit", Validate.REQUIRED);

    @Override
    public void initAndValidate() {
        super.initAndValidate();
    }

    @Override
    public Node simulateCoalescentWithMax(List<Node> nodes, PopulationFunction demographic, final double maxHeight) {
        // sanity check - disjoint trees

//        if( ! Tree.Utils.allDisjoint(nodes) ) {
//            throw new RuntimeException("non disjoint trees");
//        }

        if (nodes.size() == 0) {
            throw new IllegalArgumentException("empty nodes set");
        }

        final double lowestHeight = speciesTreeInput.get().getRoot().getHeight();

        for (int attempts = 0; attempts < 1000; ++attempts) {
            try {
                final List<Node> rootNode = simulateCoalescent(nodes, demographic, lowestHeight, maxHeight);
                if (rootNode.size() == 1) {
                    return rootNode.get(0);
                }
            } catch (ConstraintViolatedException e) {
                // TODO: handle exception
            }
        }

        throw new RuntimeException("failed to merge trees after 1000 tries!");
    }
}
