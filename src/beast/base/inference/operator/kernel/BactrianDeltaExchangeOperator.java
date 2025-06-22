package beast.base.inference.operator.kernel;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.operator.CompoundParameterHelper;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;

@Description("Delta exchange operator that proposes through a Bactrian distribution for real valued parameters")
public class BactrianDeltaExchangeOperator extends KernelOperator {

    public final Input<List<RealParameter>> parameterInput = new Input<>("parameter",
            "if specified, this parameter is operated on", new ArrayList<>());
    public final Input<List<IntegerParameter>> intparameterInput = new Input<>("intparameter",
            "if specified, this parameter is operated on", new ArrayList<>());

    public final Input<Double> deltaInput = new Input<>("delta", "Magnitude of change for two randomly picked values.", 1.0);
    public final Input<Boolean> autoOptimizeiInput =
            new Input<>("autoOptimize", "if true, window size will be adjusted during the MCMC run to improve mixing.", true);
    public final Input<Boolean> integerOperatorInput = new Input<>("integer", "if true, changes are all integers.", false);
    public final Input<IntegerParameter> parameterWeightsInput = new Input<>("weightvector", "weights on a vector parameter");

    

    private boolean autoOptimize;
    private double delta;
    private boolean isIntegerOperator;
    private CompoundParameterHelper compoundParameter = null;
    
    // because CompoundParameter cannot derive from parameter due to framework, the code complexity is doubled

	private int[] weights() {
		int[] weights;
		if (compoundParameter == null) { // one parameter case
			if (parameterInput.get().isEmpty()) {
				if (intparameterInput.get().size() > 0) {
					weights = new int[intparameterInput.get().get(0)
							.getDimension()];
				} else {
					// happens when BEAUti is setting things up
					weights = new int[0];
				}
			} else {
				weights = new int[parameterInput.get().get(0).getDimension()];
			}
		} else {
			if (compoundParameter.getDimension() < 1)
				throw new IllegalArgumentException(
						"Compound parameter is not created properly, dimension = "
								+ compoundParameter.getDimension());

			weights = new int[compoundParameter.getDimension()];
		}

		if (parameterWeightsInput.get() != null) {
			if (weights.length != parameterWeightsInput.get().getDimension())
				throw new IllegalArgumentException(
						"Weights vector should have the same length as parameter dimension");

			for (int i = 0; i < weights.length; i++) {
				weights[i] = parameterWeightsInput.get().getValue(i);
			}
		} else {
			for (int i = 0; i < weights.length; i++) {
				weights[i] = 1;
			}
		}
		return weights;
	}

    public void initAndValidate() {
    	super.initAndValidate();

        autoOptimize = autoOptimizeiInput.get();
        delta = deltaInput.get();
        isIntegerOperator = integerOperatorInput.get();

        if (parameterInput.get().isEmpty()) {
            if (intparameterInput.get().size() > 1) {
            	// sanity check
            	for (int i = 0; i < intparameterInput.get().size(); i++) {
            		for (int j = i + 1; j < intparameterInput.get().size(); j++) {
            			if (intparameterInput.get().get(i) == intparameterInput.get().get(j)) {
            				throw new RuntimeException("Duplicate intparameter (" + intparameterInput.get().get(j).getID() + ") found in operator " + getID());
            			}
            		}
            	}
            	// create single parameter from the list of int-parameters
                compoundParameter = new CompoundParameterHelper((intparameterInput.get()));
            }
        } else {
            if (parameterInput.get().size() > 1) {
            	// sanity check
            	for (int i = 0; i < parameterInput.get().size(); i++) {
            		for (int j = i + 1; j < parameterInput.get().size(); j++) {
            			if (parameterInput.get().get(i) == parameterInput.get().get(j)) {
            				throw new RuntimeException("Duplicate intparameter (" + parameterInput.get().get(j).getID() + ") found in operator " + getID());
            			}
            		}
            	}
            	// create single parameter from the list of parameters
                compoundParameter = new CompoundParameterHelper(parameterInput.get());
            }
        }

        if (isIntegerOperator && delta != Math.round(delta)) {
            throw new IllegalArgumentException("Can't be an integer operator if delta is not integer");
        }

        // dimension sanity check
        int dim = -1;
        if (compoundParameter == null) { // one parameter case
        	dim = (parameterInput.get().size() > 0 ? parameterInput.get().get(0).getDimension() : 
        		   intparameterInput.get().size() > 0 ? intparameterInput.get().get(0).getDimension() : 0);
        } else {
            dim = compoundParameter.getDimension();
        }
    	if (dim <= 1) {
    		Log.warning.println("WARNING: the dimension of the parameter is " + dim + " at the start of the run.\n"
    				+ "         The operator " + getID() + " has no effect (if this does not change).");
    	}

    }

    @Override
    public final double proposal() {
    	int[] parameterWeights = weights();
    	final int dim = parameterWeights.length;
    	
    	// Find the number of weights that are nonzero
    	int nonZeroWeights = 0;
    	for (int i: parameterWeights) {
    		if (i != 0) {
    			++nonZeroWeights;
    		}
    	}
    	
        if (nonZeroWeights <= 1) {
        	// it is impossible to select two distinct entries in this case, so there is nothing to propose 
        	return 0.0;
        }
    	
        // Generate indices for the values to be modified
        int dim1 = Randomizer.nextInt(nonZeroWeights);
        int dim2 = Randomizer.nextInt(nonZeroWeights-1);
        if (dim2 >= dim1) {
        	++dim2;
        }
        if (nonZeroWeights<dim) {
        	// There are zero weights, so we need to increase dim1 and dim2 accordingly.
        	int nonZerosBeforeDim1 = dim1;
        	int nonZerosBeforeDim2 = dim2;
        	dim1 = 0;
        	dim2 = 0;
        	while (nonZerosBeforeDim1 > 0 | parameterWeights[dim1] == 0 ) {
        		if (parameterWeights[dim1] != 0) {
        			--nonZerosBeforeDim1;
        		}
        		++dim1;
        	}
        	while (nonZerosBeforeDim2 > 0 | parameterWeights[dim2] == 0 ) {
        		if (parameterWeights[dim2] != 0) {
        			--nonZerosBeforeDim2;
        		}
        		++dim2;
        	}
        }

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

            if (intparameter == null) {
                // operate on real parameter
                double scalar1 = realparameter.getValue(dim1);
                double scalar2 = realparameter.getValue(dim2);

                if (isIntegerOperator) {
                    final int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

                    if (parameterWeights[dim1] != parameterWeights[dim2]) throw new RuntimeException();
                    scalar1 = Math.round(scalar1 - d);
                    scalar2 = Math.round(scalar2 + d);
                } else {

                    // exchange a random delta
                    final double d = getNextDouble(0);
//                    scalar1 -= d;
//                    if (parameterWeights[dim1] != parameterWeights[dim2]) {
//                        scalar2 += d * parameterWeights[dim1] / parameterWeights[dim2];
//                    } else {
//                        scalar2 += d;
//                    }

                    if (parameterWeights[dim1] != parameterWeights[dim2]) {
                        final double sumW = parameterWeights[dim1] + parameterWeights[dim2];
                        scalar1 -= d * parameterWeights[dim2] / sumW;
                        scalar2 += d * parameterWeights[dim1] / sumW;
                    } else {
                        scalar1 -= d / 2; // for equal weights
                        scalar2 += d / 2;
                    }

//                    double w1 = parameterWeights[dim1];
//                    double w2 = parameterWeights[dim2];
//                    double wsum = w1 + w2;
//                    scalar1 = scalar1 + d*w2/wsum;
//                    scalar2 = scalar2 - d*w1/wsum;
                    


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

                final int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

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

            if (intparameterInput.get().isEmpty()) {
                // operate on real parameter
                double scalar1 = (Double) compoundParameter.getValue(dim1);
                double scalar2 = (Double) compoundParameter.getValue(dim2);

                if (isIntegerOperator) {
                    final int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

                    if (parameterWeights[dim1] != parameterWeights[dim2]) throw new RuntimeException();
                    scalar1 = Math.round(scalar1 - d);
                    scalar2 = Math.round(scalar2 + d);
                } else {

                    // exchange a random delta
                    final double d = getNextDouble(0);
//                    scalar1 -= d;
//                    if (parameterWeights[dim1] != parameterWeights[dim2]) {
//                        scalar2 += d * parameterWeights[dim1] / parameterWeights[dim2];
//                    } else {
//                        scalar2 += d;
//                    }

                    if (parameterWeights[dim1] != parameterWeights[dim2]) {
                        final double sumW = parameterWeights[dim1] + parameterWeights[dim2];
                        scalar1 -= d * parameterWeights[dim2] / sumW;
                        scalar2 += d * parameterWeights[dim1] / sumW;
                    } else {
                        scalar1 -= d / 2; // for equal weights
                        scalar2 += d / 2;
                    }

//                    double w1 = parameterWeights[dim1];
//                    double w2 = parameterWeights[dim2];
//                    double wsum = w1 + w2;
//                    scalar1 = scalar1 + d*w2/wsum;
//                    scalar2 = scalar2 - d*w1/wsum;

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

                final int d = Randomizer.nextInt((int) Math.round(delta)) + 1;

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

    private double getNextDouble(int i) {
    	return kernelDistribution.getRandomDelta(i, Double.NaN, delta);
	}

	@Override
    public double getCoercableParameterValue() {
        return delta;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        delta = value;
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    @Override
    public void optimize(final double logAlpha) {
        // must be overridden by operator implementation to have an effect
        if (autoOptimize) {
            double _delta = calcDelta(logAlpha);
            _delta += Math.log(delta);
            delta = Math.exp(_delta);
            if (isIntegerOperator) {
            	// when delta < 0.5
            	// Randomizer.nextInt((int) Math.round(delta)) becomes
            	// Randomizer.nextInt(0) which results in an exception
            	delta = Math.max(0.5000000001, delta);
            }
        }

    }

    @Override
    public final String getPerformanceSuggestion() {
        final double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        final double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        final double newDelta = delta * ratio;

        final DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting delta to about " + formatter.format(newDelta);
        } else if (prob > 0.40) {
            return "Try setting delta to about " + formatter.format(newDelta);
        } else return "";
    }
    
	
    @Override
    public double getTargetAcceptanceProbability() {
    	return 0.3;
    }

}
