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
        + "distributions over MRCA times or (sets of) tips of trees")
public class MRCAPrior extends Distribution {
    public final Input<Tree> m_treeInput = new Input<Tree>("tree", "the tree containing the taxon set", Validate.REQUIRED);
    public final Input<TaxonSet> m_taxonset = new Input<TaxonSet>("taxonset",
            "set of taxa for which prior information is available");
    public final Input<Boolean> m_bIsMonophyleticInput = new Input<Boolean>("monophyletic",
            "whether the taxon set is monophyletic (forms a clade without other taxa) or nor. Default is false.", false);
    public final Input<ParametricDistribution> m_distInput = new Input<ParametricDistribution>("distr",
            "distribution used to calculate prior over MRCA time, "
                    + "e.g. normal, beta, gamma. If not specified, monophyletic must be true");
    public final Input<Boolean> m_bOnlyUseTipsInput = new Input<Boolean>("tipsonly",
            "flag to indicate tip dates are to be used instead of the MRCA node. " +
                    "If set to true, the prior is applied to the height of all tips in the taxonset " +
                    "and the monophyletic flag is ignored. Default is false.", false);

    /**
     * shadow members *
     */
    ParametricDistribution m_dist;
    Tree m_tree;
    // number of taxa in taxon set
    int m_nNrOfTaxa = -1;
    // array of flags to indicate which taxa are in the set
    boolean[] m_bTaxaSet;
    // array of indices of taxa
    int[] m_iTaxa;
    // stores time to be calculated
    double m_fMRCATime = -1;
    double m_fStoredMRCATime = -1;
    // flag indicating taxon set is monophyletic
    boolean m_bIsMonophyletic = false;
    boolean m_bOnlyUseTips = false;

    @Override
    public void initAndValidate() throws Exception {
        m_dist = m_distInput.get();
        m_tree = m_treeInput.get();
        final List<String> sTaxaNames = new ArrayList<String>();
        for (final String sTaxon : m_tree.getTaxaNames()) {
            sTaxaNames.add(sTaxon);
        }
        // determine nr of taxa in taxon set
        List<String> set = null;
        if (m_taxonset.get() != null) {
            set = m_taxonset.get().asStringList();
            m_nNrOfTaxa = set.size();
        } else {
            // assume all taxa
            m_nNrOfTaxa = sTaxaNames.size();
        }

        m_bOnlyUseTips = m_bOnlyUseTipsInput.get();
        if (m_nNrOfTaxa == 1) {
            // ignore test for Monophyletic when it only involves a tree tip
            m_bOnlyUseTips = true;
        }
        if (!m_bOnlyUseTips && m_nNrOfTaxa < 2) {
            throw new Exception("At least two taxa are required in a taxon set");
        }
        if (!m_bOnlyUseTips && m_taxonset.get() == null) {
            throw new Exception("Taxonset must be specified OR tipsonly be set to true");
        }

        // determine which taxa are in the set
        m_iTaxa = new int[m_nNrOfTaxa];
        if ( set != null )  {  // m_taxonset.get() != null) {
            m_bTaxaSet = new boolean[sTaxaNames.size()];
            int k = 0;
            for (final String sTaxon : set) {
                final int iTaxon = sTaxaNames.indexOf(sTaxon);
                if (iTaxon < 0) {
                    throw new Exception("Cannot find taxon " + sTaxon + " in data");
                }
                if (m_bTaxaSet[iTaxon]) {
                    throw new Exception("Taxon " + sTaxon + " is defined multiple times, while they should be unique");
                }
                m_bTaxaSet[iTaxon] = true;
                m_iTaxa[k++] = iTaxon;
            }
        } else {
            for (int i = 0; i < m_nNrOfTaxa; i++) {
                m_iTaxa[i] = i;
            }
        }
    }

    @Override
    public double calculateLogP() throws Exception {
        logP = 0;
        if (m_bOnlyUseTips) {
            // tip date
            for (final int i : m_iTaxa) {
                m_fMRCATime = m_tree.getNode(i).getDate();
                logP += m_dist.logDensity(m_fMRCATime);
            }
            return logP;
        } else {
            // internal node
            calcMRCAtime(m_tree.getRoot(), new int[1]);
        }
        if (m_bIsMonophyleticInput.get() && !m_bIsMonophyletic) {
            logP = Double.NEGATIVE_INFINITY;
            return Double.NEGATIVE_INFINITY;
        }
        if (m_dist != null) {
            logP = m_dist.logDensity(m_fMRCATime - m_dist.m_offset.get());
        }
        return logP;
    }

    /**
     * Recursively visit all leaf nodes, and collect number of taxa in the taxon
     * set. When all taxa in the set are visited, record the time.
     * *
     * @param node
     * @param nTaxonCount
     */
    int calcMRCAtime(final Node node, final int[] nTaxonCount) {
        if (node.isLeaf()) {
            nTaxonCount[0]++;
            if (m_bTaxaSet[node.getNr()]) {
                return 1;
            } else {
                return 0;
            }
        } else {
            int iTaxons = calcMRCAtime(node.getLeft(), nTaxonCount);
            final int nLeftTaxa = nTaxonCount[0];
            nTaxonCount[0] = 0;
            if (node.getRight() != null) {
                iTaxons += calcMRCAtime(node.getRight(), nTaxonCount);
                final int nRightTaxa = nTaxonCount[0];
                nTaxonCount[0] = nLeftTaxa + nRightTaxa;
                if (iTaxons == m_nNrOfTaxa) {
                    // we are at the MRCA, so record the height
                    m_fMRCATime = node.getDate();
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


    /**
     * Loggable interface implementation follows *
     */
    @Override
    public void init(final PrintStream out) throws Exception {
        if (m_bOnlyUseTips) {
            if (m_dist != null) {
                out.print("logP(mrca(" + getID() + "))\t");
            }
            for (final int i : m_iTaxa) {
                out.print("height(" + m_tree.getTaxaNames()[i] + ")\t");
            }
        } else {
            if (m_dist != null || m_bIsMonophyleticInput.get()) {
                out.print("logP(mrca(" + m_taxonset.get().getID() + "))\t");
            }
            out.print("mrcatime(" + m_taxonset.get().getID() + ")\t");
        }
    }

    @Override
    public void log(final int nSample, final PrintStream out) {
        if (m_bOnlyUseTips) {
            if (m_dist != null) {
                out.print(getCurrentLogP() + "\t");
            }
            for (final int i : m_iTaxa) {
                out.print(m_tree.getNode(i).getDate() + "\t");
            }
        } else {
            if (m_dist != null || m_bIsMonophyleticInput.get()) {
                out.print(getCurrentLogP() + "\t");
            } else {
                calcMRCAtime(m_tree.getRoot(), new int[1]);
            }
            out.print(m_fMRCATime + "\t");
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
        return logP;
    }

    @Override
    public double getArrayValue(final int iDim) {
        switch (iDim) {
            case 0:
                return logP;
            case 1:
                return m_fMRCATime;
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