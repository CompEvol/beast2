package beast.evolution.operators;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;



@Description("Scales tip dates on a tree by randomly selecting one from (a subset of) taxa")
public class TipDatesScaler extends TreeOperator {
    // perhaps multiple trees may be necessary if they share the same taxon?
    // public Input<List<Tree>> m_treesInput = new Input<>("tree" ,"tree to operate on", new ArrayList<>(), Validate.REQUIRED);

    final public Input<Double> scaleFactorInput = new Input<>("scaleFactor", "scaling factor: larger means more bold proposals", 1.0);
    final public Input<TaxonSet> taxonsetInput = new Input<>("taxonset", "limit scaling to a subset of taxa. By default all tips are scaled.");

    /**
     * shadows input *
     */
    double scaleFactor;
    /**
     * node indices of taxa to choose from *
     */
    int[] taxonIndices;

    @Override
    public void initAndValidate() throws Exception {
        scaleFactor = scaleFactorInput.get();

        // determine taxon set to choose from
        if (taxonsetInput.get() != null) {
            List<String> sTaxaNames = new ArrayList<>();
            for (String sTaxon : treeInput.get().getTaxaNames()) {
                sTaxaNames.add(sTaxon);
            }

            List<String> set = taxonsetInput.get().asStringList();
            int nNrOfTaxa = set.size();
            taxonIndices = new int[nNrOfTaxa];
            int k = 0;
            for (String sTaxon : set) {
                int iTaxon = sTaxaNames.indexOf(sTaxon);
                if (iTaxon < 0) {
                    throw new IllegalArgumentException("Cannot find taxon " + sTaxon + " in tree");
                }
                taxonIndices[k++] = iTaxon;
            }
        } else {
            taxonIndices = new int[treeInput.get().getTaxaNames().length];
            for (int i = 0; i < taxonIndices.length; i++) {
                taxonIndices[i] = i;
            }
        }
    }

    @Override
    public double proposal() {
        Tree tree = treeInput.get(this);

        // randomly select leaf node
        int i = Randomizer.nextInt(taxonIndices.length);
        Node node = tree.getNode(taxonIndices[i]);
        double fUpper = node.getParent().getHeight();
        //double fLower = 0.0;
        //final double newValue = (Randomizer.nextDouble() * (fUpper -fLower)) + fLower;

        // scale node
        double fScale = (scaleFactor + (Randomizer.nextDouble() * ((1.0 / scaleFactor) - scaleFactor)));
        final double newValue = node.getHeight() * fScale;

        // check the tree does not get negative branch lengths
        if (newValue > fUpper) {
            return Double.NEGATIVE_INFINITY;
        }
        node.setHeight(newValue);

        return -Math.log(fScale);
    }

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(double fValue) {
        scaleFactor = fValue;
    }


    @Override
    public void optimize(double logAlpha) {
        double fDelta = calcDelta(logAlpha);
        fDelta += Math.log(1.0 / scaleFactor - 1.0);
        scaleFactor = 1.0 / (Math.exp(fDelta) + 1.0);
    }

    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double sf = Math.pow(scaleFactor, ratio);

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else if (prob > 0.40) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else return "";
    }

}
