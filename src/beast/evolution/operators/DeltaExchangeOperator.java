package beast.evolution.operators;

import beast.core.Input.Validate;
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
    //public Input<Tree> m_pTree = new Input<Tree>("tree", "if specified, all beast.tree branch length are scaled");

    public Input<RealParameter> parameterInput = new Input<RealParameter>("parameter", "if specified, this parameter is operated on");
    public Input<IntegerParameter> intparameterInput = new Input<IntegerParameter>("intparameter", "if specified, this parameter is operated on", Validate.XOR, parameterInput);

    public Input<Double> input_delta = new Input<Double>("delta", "Magnitude of change for two randomly picked values.", 1.0);
    public Input<Boolean> input_autoOptimize =
            new Input<Boolean>("autoOptimize", "if true, window size will be adjusted during the MCMC run to improve mixing.", true);
    public Input<Boolean> input_isIntegerOperator = new Input<Boolean>("integer", "if true, changes are all integers.", false);
    public Input<IntegerParameter> input_parameterWeights = new Input<IntegerParameter>("weightvector", "weights on a vector parameter");
    
    private boolean autoOptimize;
    private double delta;
    private boolean isIntegerOperator;
    private int[] parameterWeights;

    public void initAndValidate() {

        autoOptimize = input_autoOptimize.get();
        delta = input_delta.get();
        isIntegerOperator = input_isIntegerOperator.get();

        parameterWeights = new int[parameterInput.get().getDimension()];

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

    @Override
    public final double proposal() {
        double logq = 0.0;
        // get two dimensions
        RealParameter realparameter = parameterInput.get();
        IntegerParameter intparameter = intparameterInput.get();

        final int dim = realparameter.getDimension();
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
	
	
	        if (scalar1 < realparameter.getLower() ||
	                scalar1 > realparameter.getUpper() ||
	                scalar2 < realparameter.getLower() ||
	                scalar2 > realparameter.getUpper()) {
	            logq = Double.NEGATIVE_INFINITY;
	        }else{
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
	
	
	        if (scalar1 < intparameter.getLower() ||
	                scalar1 > intparameter.getUpper() ||
	                scalar2 < intparameter.getLower() ||
	                scalar2 > intparameter.getUpper()) {
	            logq = Double.NEGATIVE_INFINITY;
	        } else {
	        	intparameter.setValue(dim1, scalar1);
	        	intparameter.setValue(dim2, scalar2);
	        }
        	
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
    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
        if(autoOptimize){
            double fDelta = calcDelta(logAlpha);
            fDelta += Math.log(delta);
            delta = Math.exp(fDelta);
        }

    }
}
