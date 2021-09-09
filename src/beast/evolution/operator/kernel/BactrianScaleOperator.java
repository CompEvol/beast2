package beast.evolution.operator.kernel;

import java.text.DecimalFormat;

import beast.base.Description;
import beast.base.Input;
import beast.evolution.operator.ScaleOperator;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.inference.operator.kernel.KernelDistribution;
import beast.inference.parameter.BooleanParameter;
import beast.inference.parameter.RealParameter;
import beast.inference.util.InputUtil;
import beast.util.Randomizer;

@Description("Scale operator that finds scale factor according to a Bactrian distribution (Yang & Rodriguez, 2013), "
		+ "which is a mixture of two Gaussians: p(x) = 1/2*N(x;-m,1-m^2) + 1/2*N(x;+m,1-m^2) and more efficient than RealRandomWalkOperator")
public class BactrianScaleOperator extends ScaleOperator {
    public final Input<KernelDistribution> kernelDistributionInput = new Input<>("kernelDistribution", "provides sample distribution for proposals", 
    		KernelDistribution.newDefaultKernelDistribution());

    protected KernelDistribution kernelDistribution;

	@Override
	public void initAndValidate() {
    	kernelDistribution = kernelDistributionInput.get();
        if (scaleUpperLimit.get() == 1 - 1e-8) {
        	scaleUpperLimit.setValue(10.0, this);
        }
		super.initAndValidate();
	}
    
	protected double getScaler(int i, double value) {
    	return kernelDistribution.getScaler(i, value, getCoercableParameterValue());
	}
    
//	protected double getScaler(int i) {
//    	return kernelDistribution.getScaler(i, getCoercableParameterValue());
//	}

    @Override
    public double proposal() {
        try {

            double hastingsRatio;            

            if (isTreeScaler()) {

                final Tree tree = (Tree)InputUtil.get(treeInput, this);
                if (rootOnlyInput.get()) {
                    final Node root = tree.getRoot();                    
                    final double scale = getScaler(root.getNr(), root.getHeight());
                    final double newHeight = root.getHeight() * scale;

                    if (newHeight < Math.max(root.getLeft().getHeight(), root.getRight().getHeight())) {
                        return Double.NEGATIVE_INFINITY;
                    }
                    root.setHeight(newHeight);
                    return Math.log(scale);
                } else {
                    // scale the beast.tree
                    final double scale = getScaler(0, Double.NaN);
                    final int scaledNodes = tree.scale(scale);
                    return Math.log(scale) * scaledNodes;
                }
            }

            // not a tree scaler, so scale a parameter
            final boolean scaleAll = scaleAllInput.get();
            final int specifiedDoF = degreesOfFreedomInput.get();
            final boolean scaleAllIndependently = scaleAllIndependentlyInput.get();

            final RealParameter param = (RealParameter)InputUtil.get(parameterInput, this);

            assert param.getLower() != null && param.getUpper() != null;

            final int dim = param.getDimension();

            if (scaleAllIndependently) {
                // update all dimensions independently.
                hastingsRatio = 0;
                final BooleanParameter indicators = indicatorInput.get();
                if (indicators != null) {
                    final int dimCount = indicators.getDimension();
                    final Boolean[] indicator = indicators.getValues();
                    final boolean impliedOne = dimCount == (dim - 1);
                    for (int i = 0; i < dim; i++) {
                        if( (impliedOne && (i == 0 || indicator[i-1])) || (!impliedOne && indicator[i]) )  {
                            final double scaleOne = getScaler(i, param.getValue(i));
                            final double newValue = scaleOne * param.getValue(i);

                            hastingsRatio += Math.log(scaleOne);

                            if (outsideBounds(newValue, param)) {
                                return Double.NEGATIVE_INFINITY;
                            }

                            param.setValue(i, newValue);
                        }
                    }
                }  else {

                    for (int i = 0; i < dim; i++) {

                        final double scaleOne = getScaler(i, param.getValue(i));
                        final double newValue = scaleOne * param.getValue(i);

                        hastingsRatio += Math.log(scaleOne);

                        if( outsideBounds(newValue, param) ) {
                            return Double.NEGATIVE_INFINITY;
                        }

                        param.setValue(i, newValue);
                    }
                }
            } else if (scaleAll) {
                // update all dimensions
                // hasting ratio is dim-2 times of 1dim case. would be nice to have a reference here
                // for the proof. It is supposed to be somewhere in an Alexei/Nicholes article.

                // all Values assumed independent!
            	final double scale = getScaler(0, param.getValue(0));
                final int computedDoF = param.scale(scale);
                final int usedDoF = (specifiedDoF > 0) ? specifiedDoF : computedDoF ;
                hastingsRatio = usedDoF * Math.log(scale);
            } else {

                // which position to scale
                final int index;
                final BooleanParameter indicators = indicatorInput.get();
                if (indicators != null) {
                    final int dimCount = indicators.getDimension();
                    final Boolean[] indicator = indicators.getValues();
                    final boolean impliedOne = dimCount == (dim - 1);

                    // available bit locations. there can be hundreds of them. scan list only once.
                    final int[] loc = new int[dimCount + 1];
                    int locIndex = 0;

                    if (impliedOne) {
                        loc[locIndex] = 0;
                        ++locIndex;
                    }
                    for (int i = 0; i < dimCount; i++) {
                        if (indicator[i]) {
                            loc[locIndex] = i + (impliedOne ? 1 : 0);
                            ++locIndex;
                        }
                    }

                    if (locIndex > 0) {
                        final int rand = Randomizer.nextInt(locIndex);
                        index = loc[rand];
                    } else {
                        return Double.NEGATIVE_INFINITY; // no active indicators
                    }

                } else {
                    // any is good
                    index = Randomizer.nextInt(dim);
                }

                final double oldValue = param.getValue(index);

                if (oldValue == 0) {
                    // Error: parameter has value 0 and cannot be scaled
                    return Double.NEGATIVE_INFINITY;
                }

            	final double scale = getScaler(index, oldValue);
                hastingsRatio = Math.log(scale);

                final double newValue = scale * oldValue;

                if (outsideBounds(newValue, param)) {
                    // reject out of bounds scales
                    return Double.NEGATIVE_INFINITY;
                }

                param.setValue(index, newValue);
                // provides a hook for subclasses
                //cleanupOperation(newValue, oldValue);
            }

            return hastingsRatio;

        } catch (Exception e) {
            // whatever went wrong, we want to abort this operation...
            return Double.NEGATIVE_INFINITY;
        }
    }

    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
    	if (optimiseInput.get()) {
	        double delta = calcDelta(logAlpha);
	        double scaleFactor = getCoercableParameterValue();
	        delta += Math.log(scaleFactor);
	        scaleFactor = Math.exp(delta);
	        setCoercableParameterValue(scaleFactor);
    	}
    }
    
    @Override
    public double getTargetAcceptanceProbability() {
    	return 0.3;
    }
    


    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = getCoercableParameterValue() * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Try setting scale factor to about " + formatter.format(newWindowSize);
        } else return "";
    }

}
