package beast.base.evolution.operator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;



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
    public void initAndValidate() {
        scaleFactor = scaleFactorInput.get();

        // determine taxon set to choose from
        if (taxonsetInput.get() != null) {
            List<String> taxaNames = new ArrayList<>();
            for (String taxon : treeInput.get().getTaxaNames()) {
                taxaNames.add(taxon);
            }

            List<String> set = taxonsetInput.get().asStringList();
            int nrOfTaxa = set.size();
            taxonIndices = new int[nrOfTaxa];
            int k = 0;
            for (String taxon : set) {
                int taxonIndex = taxaNames.indexOf(taxon);
                if (taxonIndex < 0) {
                    throw new IllegalArgumentException("Cannot find taxon " + taxon + " in tree");
                }
                taxonIndices[k++] = taxonIndex;
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
        Tree tree = (Tree) InputUtil.get(treeInput, this);

        // randomly select leaf node
        int i = Randomizer.nextInt(taxonIndices.length);
        Node node = tree.getNode(taxonIndices[i]);
        double upper = node.getParent().getHeight();
        //double lower = 0.0;
        //final double newValue = (Randomizer.nextDouble() * (upper -lower)) + lower;

        // scale node
        double scale = (scaleFactor + (Randomizer.nextDouble() * ((1.0 / scaleFactor) - scaleFactor)));
        final double newValue = node.getHeight() * scale;

        // check the tree does not get negative branch lengths
        if (newValue > upper) {
            return Double.NEGATIVE_INFINITY;
        }
        node.setHeight(newValue);

        return -Math.log(scale);
    }

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        scaleFactor = value;
    }


    @Override
    public void optimize(double logAlpha) {
        double delta = calcDelta(logAlpha);
        delta += Math.log(1.0 / scaleFactor - 1.0);
        scaleFactor = 1.0 / (Math.exp(delta) + 1.0);
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
