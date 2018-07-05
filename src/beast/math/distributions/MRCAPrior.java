package beast.math.distributions;


import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;


@Description("Prior over set of taxa, useful for defining monophyletic constraints and "
        + "distributions over MRCA times or (sets of) tips of trees")
public class MRCAPrior extends Distribution {
    public final Input<Tree> treeInput = new Input<>("tree", "the tree containing the taxon set", Validate.REQUIRED);
    public final Input<TaxonSet> taxonsetInput = new Input<>("taxonset",
            "set of taxa for which prior information is available");
    public final Input<Boolean> isMonophyleticInput = new Input<>("monophyletic",
            "whether the taxon set is monophyletic (forms a clade without other taxa) or nor. Default is false.", false);
    public final Input<ParametricDistribution> distInput = new Input<>("distr",
            "distribution used to calculate prior over MRCA time, "
                    + "e.g. normal, beta, gamma. If not specified, monophyletic must be true");
    public final Input<Boolean> onlyUseTipsInput = new Input<>("tipsonly",
            "flag to indicate tip dates are to be used instead of the MRCA node. " +
                    "If set to true, the prior is applied to the height of all tips in the taxonset " +
                    "and the monophyletic flag is ignored. Default is false.", false);
    public final Input<Boolean> useOriginateInput = new Input<>("useOriginate", "Use parent of clade instead of clade. Cannot be used with tipsonly, or on the root.", false);

    /**
     * shadow members *
     */
    ParametricDistribution dist;
    Tree tree;
    // number of taxa in taxon set
    int nrOfTaxa = -1;
    // array of flags to indicate which taxa are in the set
    Set<String> isInTaxaSet = new LinkedHashSet<>();

    // array of indices of taxa
    int[] taxonIndex;
    // stores time to be calculated
    double MRCATime = -1;
    double storedMRCATime = -1;
    // flag indicating taxon set is monophyletic
    boolean isMonophyletic = false;
    boolean onlyUseTips = false;
    boolean useRoot = false;
    boolean useOriginate = false;
    
    boolean initialised = false;

    @Override
    public void initAndValidate() {
        dist = distInput.get();
        tree = treeInput.get();
        final List<String> taxaNames = new ArrayList<>();
        for (final String taxon : tree.getTaxaNames()) {
            taxaNames.add(taxon);
        }
        // determine nr of taxa in taxon set
        List<String> set = null;
        if (taxonsetInput.get() != null) {
            set = taxonsetInput.get().asStringList();
            nrOfTaxa = set.size();
        } else {
            // assume all taxa
            nrOfTaxa = taxaNames.size();
        }

        onlyUseTips = onlyUseTipsInput.get();
        useOriginate = useOriginateInput.get();
        if (nrOfTaxa == 1) {
            // ignore test for Monophyletic when it only involves a tree tip
        	if (!useOriginate && !onlyUseTips) {
        		onlyUseTips = true;
        	}
        }
        if (!onlyUseTips && !useOriginate && nrOfTaxa < 2) {
            throw new IllegalArgumentException("At least two taxa are required in a taxon set");
        }
        if (!onlyUseTips && taxonsetInput.get() == null) {
            throw new IllegalArgumentException("Taxonset must be specified OR tipsonly be set to true");
        }
        
       
        if (useOriginate && onlyUseTips) {
        	throw new IllegalArgumentException("'useOriginate' and 'tipsOnly' cannot be both true");
        }
        useRoot = nrOfTaxa == tree.getLeafNodeCount();
        if (useOriginate && useRoot) {
        	throw new IllegalArgumentException("Cannot use originate of root. You can set useOriginate to false to fix this");
        }
        initialised = false;
    }

    boolean [] nodesTraversed;
    int nseen;

    protected Node getCommonAncestor(Node n1, Node n2) {
        // assert n1.getTree() == n2.getTree();
        if( ! nodesTraversed[n1.getNr()] ) {
            nodesTraversed[n1.getNr()] = true;
            nseen += 1;
        }
        if( ! nodesTraversed[n2.getNr()] ) {
            nodesTraversed[n2.getNr()] = true;
            nseen += 1;
        }
        while (n1 != n2) {
	        double h1 = n1.getHeight();
	        double h2 = n2.getHeight();
	        if ( h1 < h2 ) {
	            n1 = n1.getParent();
	            if( ! nodesTraversed[n1.getNr()] ) {
	                nodesTraversed[n1.getNr()] = true;
	                nseen += 1;
	            }
	        } else if( h2 < h1 ) {
	            n2 = n2.getParent();
	            if( ! nodesTraversed[n2.getNr()] ) {
	                nodesTraversed[n2.getNr()] = true;
	                nseen += 1;
	            }
	        } else {
	            //zero length branches hell
	            Node n;
	            double b1 = n1.getLength();
	            double b2 = n2.getLength();
	            if( b1 > 0 ) {
	                n = n2;
	            } else { // b1 == 0
	                if( b2 > 0 ) {
	                    n = n1;
	                } else {
	                    // both 0
	                    n = n1;
	                    while( n != null && n != n2 ) {
	                        n = n.getParent();
	                    }
	                    if( n == n2 ) {
	                        // n2 is an ancestor of n1
	                        n = n1;
	                    } else {
	                        // always safe to advance n2
	                        n = n2;
	                    }
	                }
	            }
	            if( n == n1 ) {
                    n = n1 = n.getParent();
                } else {
                    n = n2 = n.getParent();
                }
	            if( ! nodesTraversed[n.getNr()] ) {
	                nodesTraversed[n.getNr()] = true;
	                nseen += 1;
	            } 
	        }
        }
        return n1;
    }

    // A lightweight version for finding the most recent common ancestor of a group of taxa.
    // return the node-ref of the MRCA.

    // would be nice to use nodeRef's, but they are not preserved :(
    public Node getCommonAncestor() {
        if (!initialised) {
            initialise();
        }
        nodesTraversed = new boolean[tree.getNodeCount()];
        Node n = getCommonAncestorInternal();
        assert ! (useRoot && !n.isRoot() ) ;
        return n;
    }

    private Node getCommonAncestorInternal() {
        Node cur = tree.getNode(taxonIndex[0]);

        for (int k = 1; k < taxonIndex.length; ++k) {
            cur = getCommonAncestor(cur, tree.getNode(taxonIndex[k]));
        }
        return cur;
    }

    @Override
    public double calculateLogP() {
    	if (!initialised) {
    		initialise();
    	}
        logP = 0;
        if (onlyUseTips) {
            // tip date
        	if (dist == null) {
        		return logP;
        	}
            for (final int i : taxonIndex) {
                MRCATime = tree.getNode(i).getDate();
                logP += dist.logDensity(MRCATime);
            }
            return logP;
        } else if (useRoot) {
        	if (dist != null) {
                MRCATime = tree.getRoot().getDate();
                logP += dist.logDensity(MRCATime);
        	}
    		return logP;
        } else {
            // internal node
            if( false) {
                calcMRCAtime(tree.getRoot(), new int[1]);
            } else {
            	Node m;
            	if (taxonIndex.length == 1) {
            		isMonophyletic = true;
            		m = tree.getNode(taxonIndex[0]);
            	} else {
	                nseen = 0;
                	m = getCommonAncestor();
	                isMonophyletic = (nseen == 2 * taxonIndex.length - 1);
            	}
            	if (useOriginate) {
            		if (!m.isRoot()) {
            			MRCATime = m.getParent().getDate();
            		} else {
            			MRCATime = m.getDate();
            		}
            	} else {
            		MRCATime = m.getDate();
            	}
            }
        }
        if (isMonophyleticInput.get() && !isMonophyletic) {
    		logP = Double.NEGATIVE_INFINITY;
    		return Double.NEGATIVE_INFINITY;
        }
        if (dist != null) {
            logP = dist.logDensity(MRCATime); // - dist.offsetInput.get());
        }
        return logP;
    }

    protected void initialise() {
        // determine which taxa are in the set
    	
        List<String> set = null;
        if (taxonsetInput.get() != null) {
            set = taxonsetInput.get().asStringList();
        }
        final List<String> taxaNames = new ArrayList<>();
        for (final String taxon : tree.getTaxaNames()) {
            taxaNames.add(taxon);
        }

        taxonIndex = new int[nrOfTaxa];
        if ( set != null )  {  // m_taxonset.get() != null) {
            isInTaxaSet.clear();
            int k = 0;
            for (final String taxon : set) {
                final int taxonIndex_ = taxaNames.indexOf(taxon);
                if (taxonIndex_ < 0) {
                    throw new RuntimeException("Cannot find taxon " + taxon + " in data");
                }
                if (isInTaxaSet.contains(taxon)) {
                    throw new RuntimeException("Taxon " + taxon + " is defined multiple times, while they should be unique");
                }
                isInTaxaSet.add(taxon);
                taxonIndex[k++] = taxonIndex_;
            }
        } else {
            for (int i = 0; i < nrOfTaxa; i++) {
                taxonIndex[i] = i;
            }
        }
        initialised = true;
 	}

	/**
     * Recursively visit all leaf nodes, and collect number of taxa in the taxon
     * set. When all taxa in the set are visited, record the time.
     * *
     * @param node
     * @param taxonCount2
     */
    int calcMRCAtime(final Node node, final int[] taxonCount2) {
        if (node.isLeaf()) {
            taxonCount2[0]++;
            if (isInTaxaSet.contains(node.getID())) {
                return 1;
            } else {
                return 0;
            }
        } else {
            int taxonCount = calcMRCAtime(node.getLeft(), taxonCount2);
            final int leftTaxa = taxonCount2[0];
            taxonCount2[0] = 0;
            if (node.getRight() != null) {
                taxonCount += calcMRCAtime(node.getRight(), taxonCount2);
                final int rightTaxa = taxonCount2[0];
                taxonCount2[0] = leftTaxa + rightTaxa;
                if (taxonCount == nrOfTaxa) {
                	if (nrOfTaxa == 1 && useOriginate) {
            			MRCATime = node.getDate();
                        isMonophyletic = true;
                        return taxonCount + 1;
                	}
                    // we are at the MRCA, so record the height
                	if (useOriginate) {
                		Node parent = node.getParent();
                		if (parent != null) {
                			MRCATime = parent.getDate();
                		} else {
                			MRCATime = node.getDate();
                		}
                	} else {
                		MRCATime = node.getDate();
                	}
                    isMonophyletic = (taxonCount2[0] == nrOfTaxa);
                    return taxonCount + 1;
                }
            }
            return taxonCount;
        }
    }


    @Override
    public void store() {
        storedMRCATime = MRCATime;
        // don't need to store m_bIsMonophyletic since it is never reported
        // explicitly, only logP and MRCA time are (re)stored
        super.store();
    }

    @Override
    public void restore() {
        MRCATime = storedMRCATime;
        super.restore();
    }

    @Override
    protected boolean requiresRecalculation() {
        return super.requiresRecalculation();
    }


    /**
     * Loggable interface implementation follows *
     */
    @Override
    public void init(final PrintStream out) {
    	if (!initialised) {
    		initialise();
    	}
        if (onlyUseTips) {
            if (dist != null) {
                out.print("logP(mrca(" + getID() + "))\t");
            }
            for (final int i : taxonIndex) {
                out.print("height(" + tree.getTaxaNames()[i] + ")\t");
            }
        } else {
        	if (!isMonophyleticInput.get()) {
        		out.print("monophyletic(" + taxonsetInput.get().getID() + ")\t");
        	}
            if (dist != null) {
                out.print("logP(mrca(" + taxonsetInput.get().getID() + "))\t");
            }
            // this is sometimes a date and sometimes an age -- we need to be more explicit in the name.
            // This is also a problem in BEAUti when specifying an MRCAPrior
            out.print("mrca." + tree.getDateType() + "(" + taxonsetInput.get().getID() + (useOriginate ? ".originate" : "") +")\t");
        }
    }

    @Override
    public void log(final long sample, final PrintStream out) {
        if (onlyUseTips) {
            if (dist != null) {
                out.print(getCurrentLogP() + "\t");
            }
            for (final int i : taxonIndex) {
                out.print(tree.getNode(i).getDate() + "\t");
            }
        } else {
        	if (!isMonophyleticInput.get()) {
        		out.print((isMonophyletic ? 1 : 0) + "\t");
        	}
            if (dist != null) {
                out.print(getCurrentLogP() + "\t");
            } else {
                calcMRCAtime(tree.getRoot(), new int[1]);
            }
            out.print(MRCATime + "\t");
        }
    }

    @Override
    public void close(final PrintStream out) {
        // nothing to do
    }

    /**
     * Valuable interface implementation follows, first dimension is log likelihood, second the time *
     */
    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    public double getArrayValue() {
    	if (Double.isNaN(logP)) {
    		try {
    			calculateLogP();
    		}catch (Exception e) {
    			logP  = Double.NaN;
    		}
    	}
        return logP;
    }

    @Override
    public double getArrayValue(final int dim) {
    	if (Double.isNaN(logP)) {
    		try {
    			calculateLogP();
    		}catch (Exception e) {
    			logP  = Double.NaN;
    		}
    	}
        switch (dim) {
            case 0:
                return logP;
            case 1:
                return MRCATime;
            default:
                return 0;
        }
    }

    @Override
    public void sample(final State state, final Random random) {
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }
}