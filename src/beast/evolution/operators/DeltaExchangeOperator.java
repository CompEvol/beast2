package beast.evolution.operators;

import beast.core.Operator;
import beast.core.Input;
import beast.core.Description;
import beast.core.parameter.RealParameter;
import beast.core.parameter.IntegerParameter;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;

/**
 * A generic operator for use with a sum-constrained (possibly weighted) vector parameter.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 *
 * Migrated to BEAST 2 by CHW
 */
@Description("A generic operator for use with a sum-constrained (possibly weighted) vector parameter.")
public class DeltaExchangeOperator extends Operator {
    public Input<Tree> m_pTree = new Input<Tree>("tree", "if specified, all beast.tree branch length are scaled");

    public Input<RealParameter> parameter = new Input<RealParameter>("parameter", "if specified, this parameter is scaled", Input.Validate.REQUIRED);

    public Input<Double> input_delta = new Input<Double>("delta", "Magnitude of change for two randomly picked values.", 1.0);
    public Input<Boolean> input_autoOptimize =
            new Input<Boolean>("autoOptimize", "if true, window size will be adjusted during the MCMC run to improve mixing.", true);
    public Input<Boolean> input_isIntegerOperator = new Input<Boolean>("integer", "if true, changes are all integers.", false);
    public Input<IntegerParameter> input_parameterWeights = new Input<IntegerParameter>("weightvector", "weights on a vector parameter",
            Input.Validate.OPTIONAL);
    
    private boolean autoOptimize;
    private double delta;
    private boolean isIntegerOperator;
    private int[] parameterWeights;

    public void initAndValidate() {

        autoOptimize = input_autoOptimize.get();
        delta = input_delta.get();
        isIntegerOperator = input_isIntegerOperator.get();

        parameterWeights = new int[parameter.get().getDimension()];

        if(input_parameterWeights.get() != null){
            for (int i = 0; i < parameterWeights.length; i++) {
                parameterWeights[i] = input_parameterWeights.get().getValue(i);
            }
        }else{
            for (int i = 0; i < parameterWeights.length; i++) {
                parameterWeights[i] = 1;
            }
        }

        if (isIntegerOperator && delta != Math.round(delta)) {
            throw new IllegalArgumentException("Can't be an integer operator if delta is not integer");
        }
    }

    public final double proposal() {
        double logq = 0.0;
        // get two dimensions
        RealParameter parameter = this.parameter.get(this);
        
        final int dim = parameter.getDimension();
        final int dim1 = Randomizer.nextInt(dim);
        int dim2 = dim1;
        while (dim1 == dim2) {
            dim2 = Randomizer.nextInt(dim);
        }

        double scalar1 = parameter.getValue(dim1);
        double scalar2 = parameter.getValue(dim2);

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


        if (scalar1 < parameter.getLower() ||
                scalar1 > parameter.getUpper() ||
                scalar2 < parameter.getLower() ||
                scalar2 > parameter.getUpper()) {
            logq = Double.NEGATIVE_INFINITY;
        }else{

            parameter.setValue(dim1, scalar1);
            parameter.setValue(dim2, scalar2);
        }
        //System.err.println("apply deltaEx");
        // symmetrical move so return a zero hasting ratio
        return logq;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
        if(autoOptimize){
            double fDelta = calcDelta(logAlpha);
            fDelta += Math.log(delta);
            delta = Math.exp(fDelta);
        }

    }
}
