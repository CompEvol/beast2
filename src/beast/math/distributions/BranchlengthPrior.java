package beast.math.distributions;

import java.util.List;
import java.util.Random;

import beast.core.Citation;
import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.State;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

/**
 * @author Gereon Kaiping
 *
 */
@Citation("Chang, W., Cathcart, C., Hall, D., Garrett, A., 2015."
		+" Ancestry-constrained phylogenetic analysis supports the Indo-European steppe hypothesis."
		+" Language 91, 194-244. doi:10.1353/lan.2015.0005") 
@Description("Prior over set of taxa for defining distributions over branch lengths"
		+ " leading (sets of) tips of trees")
public class BranchlengthPrior extends Distribution {
	public final Input<Tree> treeInput = new Input<Tree>("tree",
			"the tree containing the taxon set", Validate.REQUIRED);
	public final Input<TaxonSet> taxonsetInput = new Input<TaxonSet>(
			"taxonset", "set of taxa for which prior information is available",
			Validate.REQUIRED);
	public final Input<ParametricDistribution> distInput = new Input<ParametricDistribution>(
			"distr",
			"distribution used to calculate prior over branch lengths, "
					+ "e.g. normal, beta, gamma.", Validate.REQUIRED);

	/**
	 * shadow members *
	 */
	protected ParametricDistribution dist;
	protected Tree tree;
	// number of taxa in taxon set
	protected int nrOfTaxa = -1;
	// array of flags to indicate which taxa are in the set
	protected boolean[] isInTaxaSet;
	// array of indices of taxa
	protected int[] taxonIndex;

	@Override
	public void initAndValidate() throws Exception {
		dist = distInput.get();
		tree = treeInput.get();
		final List<String> sTaxaNames = tree.getTaxonset().asStringList();
		// determine nr of taxa in taxon set
		List<String> set = taxonsetInput.get().asStringList();
		nrOfTaxa = set.size();

		// determine which taxa are in the set
		taxonIndex = new int[nrOfTaxa];
		isInTaxaSet = new boolean[sTaxaNames.size()];
		int k = 0;
		for (final String sTaxon : set) {
			final int iTaxon = sTaxaNames.indexOf(sTaxon);
			if (iTaxon < 0) {
				throw new Exception("Cannot find taxon " + sTaxon + " in data");
			}
			if (isInTaxaSet[iTaxon]) {
				throw new Exception(
						"Taxon "
								+ sTaxon
								+ " is defined multiple times, while they should be unique");
			}
			isInTaxaSet[iTaxon] = true;
			taxonIndex[k++] = iTaxon;
		}

	}

	@Override
	public double calculateLogP() throws Exception {
		logP = 0;
		// tip date minus parent date
		for (final int i : taxonIndex) {
			Node node = tree.getNode(i);
			logP += dist.logDensity(node.getDate()-node.getParent().getDate());
		}
		return logP;
	}

	@Override
	public List<String> getArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getConditions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sample(State state, Random random) {
		// TODO Auto-generated method stub
	}

}
