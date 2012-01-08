package beast.evolution.operators;


import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Operator;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.util.Randomizer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A generic operator for use with a sum-constrained (possibly weighted) vector parameter.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 *         <p/>
 *         Migrated to BEAST 2 by CHW and Walter
 */
@Description("A generic operator for use with a sum-constrained (possibly weighted) vector parameter.")
public class DeltaExchangeOperator extends Operator {
    //public Input<Tree> m_pTree = new Input<Tree>("tree", "if specified, all beast.tree branch length are scaled");

    public Input<List<RealParameter>> parameterInput = new Input<List<RealParameter>>("parameter",
            "if specified, this parameter is operated on", new ArrayList<RealParameter>());
    public Input<List<IntegerParameter>> intparameterInput = new Input<List<IntegerParameter>>("intparameter",
            "if specified, this parameter is operated on", new ArrayList<IntegerParameter>());

    public Input<Double> input_delta = new Input<Double>("delta", "Magnitude of change for two randomly picked values.", 1.0);
    public Input<Boolean> input_autoOptimize =
            new Input<Boolean>("autoOptimize", "if true, window size will be adjusted during the MCMC run to improve mixing.", true);
    public Input<Boolean> input_isIntegerOperator = new Input<Boolean>("integer", "if true, changes are all integers.", false);
    public Input<IntegerParameter> input_parameterWeights = new Input<IntegerParameter>("weightvector", "weights on a vector parameter");

    private boolean autoOptimize;
    private double delta;
    private boolean isIntegerOperator;
    private int[] parameterWeights;
    private CompoundParameterHelper compoundParameter = null;
    // because CompoundParameter cannot derive from parameter due to framework, the code complexity is doubled

    public void initAndValidate() {

        autoOptimize = input_autoOptimize.get();
        delta = input_delta.get();
        isIntegerOperator = input_isIntegerOperator.get();

        if (parameterInput.get().isEmpty()) {
            if (intparameterInput.get().size() > 1)
                compoundParameter = new CompoundParameterHelper(intparameterInput.get());
        } else {
            if (parameterInput.get().size() > 1)
                compoundParameter = new CompoundParameterHelper(parameterInput.get());
        }

        if (compoundParameter == null) { // one parameter case
            if (parameterInput.get().isEmpty()) {
                parameterWeights = new int[intparameterInput.get().get(0).getDimension()];
            } else {
                parameterWeights = new int[parameterInput.get().get(0).getDimension()];
            }
        } else {
            if (compoundParameter.getDimension() < 1)
                throw new IllegalArgumentException("Compound parameter is not created properly, dimension = " + compoundParameter.getDimension());

            parameterWeights = new int[compoundParameter.getDimension()];
        }

        if (input_parameterWeights.get() != null) {
            if (parameterWeights.length != input_parameterWeights.get().getDimension())
                throw new IllegalArgumentException("Weights vector should have the same length as parameter dimension");

            for (int i = 0; i < parameterWeights.length; i++) {
                parameterWeights[i] = input_parameterWeights.get().getValue(i);
            }
        } else {
            for (int i = 0; i < parameterWeights.length; i++) {
                parameterWeights[i] = 1;
            }
        }

        if (isIntegerOperator && delta != Math.round(delta)) {
            throw new IllegalArgumentException("Can't be an integer operator if delta is not integer");
        }
    }

    @Override
    public final double proposal() {
        double logq = 0.0;

        if (compoundParameter == null) { // one parameter case
            // get two dimensions
            RealParameter realparameter = null;
            IntegerParameter intparameter = null;

            if (parameterInput.get().isEmpty()) {
                intparameter = intparameterInput.get().get(0);
            } else {
                realparameter = parameterInput.get().get(0);
            }

            final int dim = (realparameter != null ? realparameter.getDimension() : intparameter.getDimension());

            final int dim1 = Randomizer.nextInt(dim);
            int dim2 = dim1;
            while (dim1 == dim2) {
                dim2 = Randomizer.nextInt(dim);
            }

            if (intparameter == null) {
                // operate on real parameter
                double scalar1 = realparameter.getValue(dim1);
                double scalar2 = realparameter.getValue(dim2);

                if (isIntegerOperator) {
                    int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

                    if (parameterWeights[dim1] != parameterWeights[dim2]) throw new RuntimeException();
                    scalar1 = Math.round(scalar1 - d);
                    scalar2 = Math.round(scalar2 + d);
                } else {

                    // exchange a random delta
                    final double d = Randomizer.nextDouble() * delta;
                    scalar1 -= d;
                    if (parameterWeights[dim1] != parameterWeights[dim2]) {
                        scalar2 += d * (double) parameterWeights[dim1] / (double) parameterWeights[dim2];
                    } else {
                        scalar2 += d;
                    }

                }

                if (scalar1 < realparameter.getLower() || scalar1 > realparameter.getUpper() ||
                        scalar2 < realparameter.getLower() || scalar2 > realparameter.getUpper()) {
                    logq = Double.NEGATIVE_INFINITY;
                } else {
                    realparameter.setValue(dim1, scalar1);
                    realparameter.setValue(dim2, scalar2);
                }
            } else {
                // operate on int parameter
                int scalar1 = intparameter.getValue(dim1);
                int scalar2 = intparameter.getValue(dim2);

                int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

                if (parameterWeights[dim1] != parameterWeights[dim2]) throw new RuntimeException();
                scalar1 = Math.round(scalar1 - d);
                scalar2 = Math.round(scalar2 + d);


                if (scalar1 < intparameter.getLower() || scalar1 > intparameter.getUpper() ||
                        scalar2 < intparameter.getLower() || scalar2 > intparameter.getUpper()) {
                    logq = Double.NEGATIVE_INFINITY;
                } else {
                    intparameter.setValue(dim1, scalar1);
                    intparameter.setValue(dim2, scalar2);
                }

            }

        } else { // compound parameter case

            // get two dimensions
            final int dim = compoundParameter.getDimension();

            final int dim1 = Randomizer.nextInt(dim);
            int dim2 = dim1;
            while (dim1 == dim2) {
                dim2 = Randomizer.nextInt(dim);
            }

            if (intparameterInput.get().isEmpty()) {
                // operate on real parameter
                double scalar1 = (Double) compoundParameter.getValue(dim1);
                double scalar2 = (Double) compoundParameter.getValue(dim2);

                if (isIntegerOperator) {
                    int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

                    if (parameterWeights[dim1] != parameterWeights[dim2]) throw new RuntimeException();
                    scalar1 = Math.round(scalar1 - d);
                    scalar2 = Math.round(scalar2 + d);
                } else {

                    // exchange a random delta
                    final double d = Randomizer.nextDouble() * delta;
                    scalar1 -= d;
                    if (parameterWeights[dim1] != parameterWeights[dim2]) {
                        scalar2 += d * (double) parameterWeights[dim1] / (double) parameterWeights[dim2];
                    } else {
                        scalar2 += d;
                    }

                }

                if (scalar1 < (Double) compoundParameter.getLower(dim1) ||
                        scalar1 > (Double) compoundParameter.getUpper(dim1) ||
                        scalar2 < (Double) compoundParameter.getLower(dim2) ||
                        scalar2 > (Double) compoundParameter.getUpper(dim2)) {
                    logq = Double.NEGATIVE_INFINITY;
                } else {
                    compoundParameter.setValue(dim1, scalar1);
                    compoundParameter.setValue(dim2, scalar2);
                }
            } else {
                // operate on int parameter
                int scalar1 = (Integer) compoundParameter.getValue(dim1);
                int scalar2 = (Integer) compoundParameter.getValue(dim2);

                int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

                if (parameterWeights[dim1] != parameterWeights[dim2]) throw new RuntimeException();
                scalar1 = Math.round(scalar1 - d);
                scalar2 = Math.round(scalar2 + d);


                if (scalar1 < (Integer) compoundParameter.getLower(dim1) ||
                        scalar1 > (Integer) compoundParameter.getUpper(dim1) ||
                        scalar2 < (Integer) compoundParameter.getLower(dim2) ||
                        scalar2 > (Integer) compoundParameter.getUpper(dim2)) {
                    logq = Double.NEGATIVE_INFINITY;
                } else {
                    compoundParameter.setValue(dim1, scalar1);
                    compoundParameter.setValue(dim2, scalar2);
                }

            }

        }

        //System.err.println("apply deltaEx");
        // symmetrical move so return a zero hasting ratio
        return logq;
    }

    @Override
    public double getCoercableParameterValue() {
        return delta;
    }

    @Override
    public void setCoercableParameterValue(double fValue) {
        delta = fValue;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
        if (autoOptimize) {
            double fDelta = calcDelta(logAlpha);
            fDelta += Math.log(delta);
            delta = Math.exp(fDelta);
        }

    }

    @Override
    public final String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newDelta = delta * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting delta to about " + formatter.format(newDelta);
        } else if (prob > 0.40) {
            return "Try setting delta to about " + formatter.format(newDelta);
        } else return "";
    }
}
