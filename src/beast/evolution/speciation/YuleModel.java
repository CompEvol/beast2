package beast.evolution.speciation;

import beast.core.Description;
import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

// From Gernhard 2008, Yule density (p; conditioned on n nodes) should be:
// double p = 0.0;
// p = lambda^(n-1) * exp(-lambda*rootHeight);
// for (int i = 1; i < n; i++) {
//    p *= exp(-lambda*height[i])
// }

@Description("Pure birth model (i.e. no deaths)")	 
public class YuleModel extends SpeciationLikelihood {
    public Input<RealParameter> birthDiffRateParameter = 
            new Input<RealParameter>("birthDiffRate", "birth difference rate parameter, lambda - mu in birth/death model");
    public Input<Boolean> m_pConditionlOnRoot =
            new Input<Boolean>("conditionalOnRoot", "Whether to condition on the root (default false)", false);

    protected boolean conditionalOnRoot;
    
    @Override
    public void initAndValidate() throws Exception {
    	super.initAndValidate();
    	conditionalOnRoot = m_pConditionlOnRoot.get();
    }

    @Override
    public double calculateTreeLogLikelihood(Tree tree) {
        final int taxonCount = tree.getNodeCount()/2+1;
        final double r = birthDiffRateParameter.get().getValue();
        final double rho = 1;
        final double a = 0;
        
        double logL = logTreeProbability(taxonCount, r, rho, a);

        logL += logNodeProbability(tree.getRoot(), r, rho, a, taxonCount);
        
        return logL;
    }
    
    /** calculate contribution of the tree to the log likelihood
     *
     * @param taxonCount
     * @param r   relative birth rate (birth rate - death rate)
     * @param rho parameter in Gernhard 2008 birth death model
     * @param a  death/birth rates ratio
     * @return
     **/
    protected double logTreeProbability(int taxonCount, double r, double rho, double a) {
        double c1 = logCoeff(taxonCount);
        if( ! conditionalOnRoot ) {
            c1 += (taxonCount - 1) * Math.log(r * rho) + taxonCount * Math.log(1 - a);
        }
        return c1;
    }

    /** default implementation, equivalent with unscaled tree in Grerhard 2008 model
     * @param taxonCount
     * @return
     **/
    protected double logCoeff(int taxonCount) {return 0.0;}	
	
    /** recursively calculate contribution of the nodes to the log likelihood
     * @param node
     * @param r
     * @param rho
     * @param a
     * @param taxonCount
     * @return
     **/
    protected double logNodeProbability(Node node, double r, double rho, double a, int taxonCount) {
    	if (node.isLeaf()) {
    		if (includeExternalNodesInLikelihoodCalculation()) {
    			return calcLogNodeProbability(node, r, rho, a, taxonCount);
    		} else {
    			return 0;
    		}
    	} else {
    		double fLogP = logNodeProbability(node.m_left, r, rho, a, taxonCount);
    		fLogP += logNodeProbability(node.m_right, r, rho, a, taxonCount);
    		fLogP += calcLogNodeProbability(node, r, rho, a, taxonCount);
    		return fLogP;
    	}
    }

    /** contribution of a single node to the log likelihood 
    * r = relative birth rate (birth rate - death rate)
    * rho = rho parameter in Gernhard 2008 birth death model
    * a = death rate relative to birth rate
    *
     * @param node
     * @param r
     * @param rho
     * @param a
     * @param taxonCount
     * @return
     **/
    protected double calcLogNodeProbability(Node node, double r, double rho, double a, int taxonCount) {
        final double height = node.getHeight();
        final double mrh = -r * height;

        if( ! conditionalOnRoot ) {
            final double z = Math.log(rho + ((1 - rho) - a) * Math.exp(mrh));
            double l = -2 * z + mrh;

            if(node.isRoot()) {
                l += mrh - z;
            }
            return l;
        } else {
            double l;
            if( !node.isRoot() ) {
                final double z = Math.log(1 - a * Math.exp(mrh));
                l = -2 * z + mrh;
            } else {
                // Root dependent coefficient from each internal node
                final double ca = 1 - a;
                final double emrh = Math.exp(-mrh);
                if( emrh != 1.0 ) {
                  //l = (tree.getTaxonCount() - 2) * Math.log(r * ca * (1 + ca /(emrh - 1)));
                  l = (taxonCount - 2) * Math.log(r * ca * (1 + ca /(emrh - 1)));
                } else {  // use exp(x)-1 = x for x near 0
                  //l = (tree.getTaxonCount() - 2) * Math.log(ca * (r + ca/height));
                  l = (taxonCount - 2) * Math.log(ca * (r + ca/height));
                }
            }
            return l;
        }
    } // calcLogNodeProbability

    public boolean includeExternalNodesInLikelihoodCalculation() {
        return false;
    }
}
