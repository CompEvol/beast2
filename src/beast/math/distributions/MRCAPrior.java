package beast.math.distributions;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Description("Prior over set of taxa, useful for defining monophyletic constraints and "
		+ "distributions over MRCA times")
public class MRCAPrior extends Distribution {
	public Input<Tree> m_treeInput = new Input<Tree>("tree", "the tree containing the taxon set", Validate.REQUIRED);
	public Input<TaxonSet> m_taxonset = new Input<TaxonSet>("taxonset",
			"set of taxa for which prior information is available", Validate.REQUIRED);
	public Input<Boolean> m_bIsMonophyleticInput = new Input<Boolean>("monophyletic",
			"whether the taxon set is monophyletic (forms a clade without other taxa) or nor. Default is false.", false);
	public Input<ParametricDistribution> m_distInput = new Input<ParametricDistribution>("distr",
			"distribution used to calculate prior over MRCA time, "
					+ "e.g. normal, beta, gamma. If not specified, monophyletic must be true");

	/** shadow members **/
	ParametricDistribution m_dist;
	Tree m_tree;
	// number of taxa in taxon set
	int m_nNrOfTaxa = -1;
	// array of flags to indicate which taxa are in the set
	boolean[] m_bTaxaSet;
	// stores time to be calculated
	double m_fMRCATime = -1;
	double m_fStoredMRCATime = -1;
	// flag indicating taxon set is monophyletic
	boolean m_bIsMonophyletic = false;
	
	@Override
	public void initAndValidate() throws Exception {
		m_dist = m_distInput.get();
		m_tree = m_treeInput.get();
		List<String> sTaxaNames = new ArrayList<String>();
		for (String sTaxon : m_tree.getTaxaNames()) {
			sTaxaNames.add(sTaxon);
		}
		// determine nr of taxa in taxon set
		List<String> set = m_taxonset.get().asStringList();
		m_nNrOfTaxa = set.size();
		if (m_nNrOfTaxa <= 1) {
			throw new Exception("At least two taxa are required in a taxon set");
		}

		// determine which taxa are in the set
		m_bTaxaSet = new boolean[sTaxaNames.size()];
		for (String sTaxon : set) {
			int iTaxon = sTaxaNames.indexOf(sTaxon);
			if (iTaxon < 0) {
				throw new Exception("Cannot find taxon " + sTaxon + " in data");
			}
			if (m_bTaxaSet[iTaxon]) {
				throw new Exception("Taxon " + sTaxon + " is defined multiple times, while they should be unique");
			}
			m_bTaxaSet[iTaxon] = true;
		}
	}

	@Override
	public double calculateLogP() throws Exception {
		logP = 0;
		calcMRCAtime(m_tree.getRoot(), new int[1]);
		if (m_bIsMonophyleticInput.get() && !m_bIsMonophyletic) {
			return Double.NEGATIVE_INFINITY;
		}
		if (m_dist != null) {
			logP = m_dist.logDensity(m_fMRCATime);
		}
		return logP;
	}
	
	/**
	 * Recursively visit all leaf nodes, and collect number of taxa in the taxon
	 * set. When all taxa in the set are visited, record the time.
	 * **/
	int calcMRCAtime(Node node, int[] nTaxonCount) {
		if (node.isLeaf()) {
			nTaxonCount[0]++;
			if (m_bTaxaSet[node.getNr()]) {
				return 1;
			} else {
				return 0;
			}
		} else {
			int iTaxons = calcMRCAtime(node.m_left, nTaxonCount);
			int nLeftTaxa = nTaxonCount[0]; 
			nTaxonCount[0] = 0;
			if (node.m_right != null) {
				iTaxons += calcMRCAtime(node.m_right, nTaxonCount);
				int nRightTaxa = nTaxonCount[0]; 
				nTaxonCount[0] = nLeftTaxa + nRightTaxa;
				if (iTaxons == m_nNrOfTaxa) {
					// we are at the MRCA, so record the height
					m_fMRCATime = node.getHeight();
					m_bIsMonophyletic = (nTaxonCount[0] == m_nNrOfTaxa);
					return iTaxons + 1;
				}
			}
			return iTaxons;
		}
	}

	
	@Override
	public void store() {
		m_fStoredMRCATime = m_fMRCATime;
		// don't need to store m_bIsMonophyletic since it is never reported 
		// explicitly, only logP and MRCA time are (re)stored
		super.store();
	}

	@Override
	public void restore() {
		m_fMRCATime = m_fStoredMRCATime;
		super.restore();
	}

	@Override
	protected boolean requiresRecalculation() {
		return super.requiresRecalculation();
	}


    /** Loggable interface implementation follows **/
    @Override
	public void init(PrintStream out) throws Exception {
		out.print("logP(mrca("+getID()+"))\tmrcatime(" + getID() + ")\t");
	}

    @Override
	public void log(int nSample, PrintStream out) {
		out.print(getCurrentLogP() + "\t" + m_fMRCATime + "\t");
	}

    @Override
	public void close(PrintStream out) {
		// nothing to do
	}
    
    /** Valuable interface implementation follows, first dimension is log likelihood, second the time **/
    @Override
	public int getDimension() {return 2;}
    @Override
	public double getArrayValue() {
    	return logP;
    }
    @Override
	public double getArrayValue(int iDim) {
    	switch (iDim) {
    	case 0: return logP; 
    	case 1: return m_fMRCATime;
    	default: return 0;
    	}
    }
    
	@Override
	public void sample(State state, Random random) {
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