package beast.evolution.operators;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.util.Randomizer;

@Description("Randomly moves tip dates on a tree by randomly selecting one from (a subset of) taxa")
public class TipDatesRandomWalker extends TreeOperator {
	// perhaps multiple trees may be necessary if they share the same taxon?
	// public Input<List<Tree>> m_treesInput = new Input<List<Tree>>("tree" ,"tree to operate on", new ArrayList<Tree>(), Validate.REQUIRED);

    public Input<Double> windowSizeInput =
        new Input<Double>("windowSize", "the size of the window both up and down when using uniform interval OR standard deviation when using Gaussian", Input.Validate.REQUIRED);
	public Input<TaxonSet> m_taxonsetInput = new Input<TaxonSet>("taxonset","limit scaling to a subset of taxa. By default all tips are scaled.");
    public Input<Boolean> useGaussianInput =
        new Input<Boolean>("useGaussian", "Use Gaussian to move instead of uniform interval. Default false.", false);

    /** node indices of taxa to choose from **/
	int [] m_iTaxa;

    double windowSize = 1;
    boolean m_bUseGaussian;

    @Override
    public void initAndValidate() throws Exception {
        windowSize = windowSizeInput.get();
        m_bUseGaussian = useGaussianInput.get();
        
        // determine taxon set to choose from
        if (m_taxonsetInput.get() != null) {
			List<String> sTaxaNames = new ArrayList<String>();
			for (String sTaxon : m_tree.get().getTaxaNames()) {
				sTaxaNames.add(sTaxon);
			}
			
			List<String> set = m_taxonsetInput.get().asStringList();
			int nNrOfTaxa = set.size();
			m_iTaxa = new int[nNrOfTaxa];
			int k = 0;
			for (String sTaxon : set) {
				int iTaxon = sTaxaNames.indexOf(sTaxon);
				if (iTaxon < 0) {
					throw new Exception("Cannot find taxon " + sTaxon + " in tree");
				}
				m_iTaxa[k++] = iTaxon;
			}
		} else {
			m_iTaxa = new int[m_tree.get().getTaxaNames().length];
			for (int i = 0; i < m_iTaxa.length; i++) {
				m_iTaxa[i] = i;
			}
		}
	}
	
    @Override
    public double proposal() {
        // randomly select leaf node
        int i = Randomizer.nextInt(m_iTaxa.length);
        Node node = m_tree.get().getNode(m_iTaxa[i]);

        double value = node.getHeight();
        double newValue = value;
        if (m_bUseGaussian) {
        	newValue += Randomizer.nextGaussian()* windowSize; 
        } else {
        	newValue += Randomizer.nextDouble() * 2 * windowSize - windowSize;
        }

        if (newValue > node.getParent().getHeight()) {
        	return Double.NEGATIVE_INFINITY;
        }
        if (newValue == value) {
        	// this saves calculating the posterior
        	return Double.NEGATIVE_INFINITY;
        }
        node.setHeight(newValue);

        return 0.0;
    }


    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
        double fDelta = calcDelta(logAlpha);
        fDelta += Math.log(windowSize);
        windowSize = Math.exp(fDelta);
    }

}
