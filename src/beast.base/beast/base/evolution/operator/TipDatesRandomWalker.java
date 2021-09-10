package beast.base.evolution.operator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.Node;
import beast.base.util.Randomizer;



@Description("Randomly moves tip dates on a tree by randomly selecting one from (a subset of) taxa")
public class TipDatesRandomWalker extends TreeOperator {
    // perhaps multiple trees may be necessary if they share the same taxon?
    // public Input<List<Tree>> m_treesInput = new Input<>("tree" ,"tree to operate on", new ArrayList<>(), Validate.REQUIRED);

    final public Input<Double> windowSizeInput =
            new Input<>("windowSize", "the size of the window both up and down when using uniform interval OR standard deviation when using Gaussian", Input.Validate.REQUIRED);
    final public Input<TaxonSet> m_taxonsetInput = new Input<>("taxonset", "limit scaling to a subset of taxa. By default all tips are scaled.");
    final public Input<Boolean> useGaussianInput =
            new Input<>("useGaussian", "Use Gaussian to move instead of uniform interval. Default false.", false);

    /**
     * node indices of taxa to choose from *
     */
    protected int[] taxonIndices;

    protected double windowSize = 1;
    protected boolean useGaussian;

    /**
     * whether to reflect random values from boundaries or absorb *
     */
    protected boolean reflectValue = true;

    @Override
    public void initAndValidate() {
        windowSize = windowSizeInput.get();
        useGaussian = useGaussianInput.get();

        // determine taxon set to choose from
        if (m_taxonsetInput.get() != null) {
            List<String> taxaNames = new ArrayList<>();
            for (String taxon : treeInput.get().getTaxaNames()) {
                taxaNames.add(taxon);
            }

            List<String> set = m_taxonsetInput.get().asStringList();
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
        // randomly select leaf node
        int i = Randomizer.nextInt(taxonIndices.length);
        Node node = treeInput.get().getNode(taxonIndices[i]);

        double value = node.getHeight();
        double newValue = value;
        if (useGaussian) {
            newValue += Randomizer.nextGaussian() * windowSize;
        } else {
            newValue += Randomizer.nextDouble() * 2 * windowSize - windowSize;
        }


        if (newValue > node.getParent().getHeight()) { // || newValue < 0.0) {
            if (reflectValue) {
            	double h = node.getParent().getHeight();
            	// tips heights may become less than 0, so their parents can have height < 0 as well
            	// https://github.com/CompEvol/beast2/issues/995
            	// so the range (0, h) may become empty
            	// therefore, set lower bound on range to min(0.0, node height) 
                newValue = reflectValue(newValue, Math.min(0.0, node.getHeight()), h);
            } else {
                return Double.NEGATIVE_INFINITY;
            }
        }
        if (newValue == value) {
            // this saves calculating the posterior
            return Double.NEGATIVE_INFINITY;
        }
        node.setHeight(newValue);

        return 0.0;
    }


    public double reflectValue(double value, double lower, double upper) {

        double newValue = value;

        if (value < lower) {
            if (Double.isInfinite(upper)) {
                // we are only going to reflect once as the upper bound is at infinity...
                newValue = lower + (lower - value);
            } else {
                double remainder = lower - value;

                int widths = (int) Math.floor(remainder / (upper - lower));
                remainder -= (upper - lower) * widths;

                // even reflections
                if (widths % 2 == 0) {
                    newValue = lower + remainder;
                    // odd reflections
                } else {
                    newValue = upper - remainder;
                }
            }
        } else if (value > upper) {
            if (Double.isInfinite(lower)) {
                // we are only going to reflect once as the lower bound is at -infinity...
                newValue = upper - (newValue - upper);
            } else {

                double remainder = value - upper;

                int widths = (int) Math.floor(remainder / (upper - lower));
                remainder -= (upper - lower) * widths;

                // even reflections
                if (widths % 2 == 0) {
                    newValue = upper - remainder;
                    // odd reflections
                } else {
                    newValue = lower + remainder;
                }
            }
        }

        return newValue;
    }


    @Override
    public double getCoercableParameterValue() {
        return windowSize;
    }

    @Override
    public void setCoercableParameterValue(double value) {
        windowSize = value;
    }

    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
        double delta = calcDelta(logAlpha);
        delta += Math.log(windowSize);
        windowSize = Math.exp(delta);
    }

    @Override
    public final String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = windowSize * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else if (prob > 0.40) {
            return "Try setting window size to about " + formatter.format(newWindowSize);
        } else return "";
    }
}
