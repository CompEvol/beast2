package beast.evolution.speciation;

import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

public abstract class UltrametricSpeciationModel extends SpeciesTreeDistribution {

    public UltrametricSpeciationModel() {
        super();
    }

    @Override
    public double calculateTreeLogLikelihood(final Tree tree) {        
        final int taxonCount = tree.getLeafNodeCount();
            
        double logL = logTreeProbability(taxonCount);
    
        final Node [] nodes = tree.getNodesAsArray();
        for (int i = taxonCount; i < nodes.length; i++) { // exclude tips
            assert ( ! nodes[i].isLeaf() );
            logL += logNodeProbability(nodes[i], taxonCount);
        }
        
        if (includeExternalNodesInLikelihoodCalculation()) { 
            for (int i = 0; i < taxonCount; i++) { // + logL of tips
                logL += logNodeProbability(nodes[i], taxonCount);
            }
        }
    
        return logL;
    }

    /** calculate contribution of the tree to the log likelihood
     *
     * @param taxonCount     
     * @return
     **/
    protected abstract double logTreeProbability(final int taxonCount);

    /** contribution of a single node to the log likelihood     
    *
     * @param node     
     * @param taxonCount
     * @return
     **/
    protected abstract double logNodeProbability(Node node, int taxonCount);
    
    /**
     * @return true if calls to logNodeProbability for terminal nodes (tips) are required
     */
    public abstract boolean includeExternalNodesInLikelihoodCalculation();


}