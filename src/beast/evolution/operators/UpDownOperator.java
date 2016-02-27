package beast.evolution.operators;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Operator;
import beast.core.StateNode;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.util.Randomizer;


@Description("This element represents an operator that scales two parameters in different directions. " +
        "Each operation involves selecting a scale uniformly at random between scaleFactor and 1/scaleFactor. " +
        "The up parameter is multiplied by this scale and the down parameter is divided by this scale.")
public class UpDownOperator extends Operator {

    final public Input<Double> scaleFactorInput = new Input<>("scaleFactor",
            "magnitude factor used for scaling", Validate.REQUIRED);
    final public Input<List<StateNode>> upInput = new Input<>("up",
            "zero or more items to scale upwards", new ArrayList<>());
    final public Input<List<StateNode>> downInput = new Input<>("down",
            "zero or more items to scale downwards", new ArrayList<>());
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to acheive a good acceptance rate (default true)", true);
    final public Input<Boolean> elementWiseInput = new Input<>("elementWise", "flag to indicate that the scaling is applied to a random index in multivariate parameters (default false)", false);

    final public Input<Double> scaleUpperLimit = new Input<>("upper", "Upper Limit of scale factor", 1.0);
    final public Input<Double> scaleLowerLimit = new Input<>("lower", "Lower limit of scale factor", 0.0);

    double scaleFactor;
    private double upper,lower;

    @Override
    public void initAndValidate() {
        scaleFactor = scaleFactorInput.get();
        // sanity checks
        if (upInput.get().size() + downInput.get().size() == 0) {
        	Log.warning.println("WARNING: At least one up or down item must be specified");
        }
        if (upInput.get().size() == 0 || downInput.get().size() == 0) {
        	Log.warning.println("WARNING: no " + (upInput.get().size() == 0 ? "up" : "down") + " item specified in UpDownOperator");
        }
        upper = scaleUpperLimit.get();
        lower = scaleLowerLimit.get();
    }

    /**
     * override this for proposals,
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal
     *         should not be accepted
     */
    @Override
    public final double proposal() {

        final double scale = (scaleFactor + (Randomizer.nextDouble() * ((1.0 / scaleFactor) - scaleFactor)));
        int goingUp = 0, goingDown = 0;


        if (elementWiseInput.get()) {
            int size = 0;
            for (StateNode up : upInput.get()) {
                if (size == 0) size = up.getDimension();
                if (size > 0 && up.getDimension() != size) {
                    throw new RuntimeException("elementWise=true but parameters of differing lengths!");
                }
                goingUp += 1;
            }

            for (StateNode down : downInput.get()) {
                if (size == 0) size = down.getDimension();
                if (size > 0 && down.getDimension() != size) {
                    throw new RuntimeException("elementWise=true but parameters of differing lengths!");
                }
                goingDown += 1;
            }

            int index = Randomizer.nextInt(size);

            for (StateNode up : upInput.get()) {
                if (up instanceof RealParameter) {
                    RealParameter p = (RealParameter) up;
                    p.setValue(p.getValue(index) * scale);
                }
                if (outsideBounds(up)) {
                    return Double.NEGATIVE_INFINITY;
                }
            }

            for (StateNode down : downInput.get()) {
                if (down instanceof RealParameter) {
                    RealParameter p = (RealParameter) down;
                    p.setValue(p.getValue(index) / scale);
                }
                if (outsideBounds(down)) {
                    return Double.NEGATIVE_INFINITY;
                }
            }
        } else {

            try {
                for (StateNode up : upInput.get()) {
                    up = up.getCurrentEditable(this);
                    goingUp += up.scale(scale);
                }
                // separated this into second loop because the outsideBounds might return true transiently with
                // related variables which would be BAD. Note current implementation of outsideBounds isn't dynamic,
                // so not currently a problem, but this became a problem in BEAST1 so this is preemptive strike.
                // Same below for down
                for (StateNode up : upInput.get()) {
                    if (outsideBounds(up)) {
                        return Double.NEGATIVE_INFINITY;
                    }
                }

                for (StateNode down : downInput.get()) {
                    down = down.getCurrentEditable(this);
                    goingDown += down.scale(1.0 / scale);
                }
                for (StateNode down : downInput.get()) {
                    if (outsideBounds(down)) {
                        return Double.NEGATIVE_INFINITY;
                    }
                }
            } catch (Exception e) {
                // scale resulted in invalid StateNode, abort proposal
                return Double.NEGATIVE_INFINITY;
            }
        }
        return (goingUp - goingDown - 2) * Math.log(scale);
    }

    private boolean outsideBounds(final StateNode node) {
        if (node instanceof Parameter<?>) {
            final Parameter<?> p = (Parameter<?>) node;
            final Double lower = (Double) p.getLower();
            final Double upper = (Double) p.getUpper();
            final Double value = (Double) p.getValue();
            if (value < lower || value > upper) {
                return true;
            }
        }
        return false;
    }

    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(final double logAlpha) {
        if (optimiseInput.get()) {
            double delta = calcDelta(logAlpha);
            delta += Math.log(1.0 / scaleFactor - 1.0);
            setCoercableParameterValue(1.0 / (Math.exp(delta) + 1.0) );
        }
    }

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        scaleFactor = Math.max(Math.min(value,upper),lower);
    }

    @Override
    public String getPerformanceSuggestion() {
        final double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        final double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        final double sf = Math.pow(scaleFactor, ratio);

        final DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else if (prob > 0.40) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else return "";
    }
} // class UpDownOperator
