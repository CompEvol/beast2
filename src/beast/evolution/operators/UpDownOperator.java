package beast.evolution.operators;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.StateNode;
import beast.core.Input.Validate;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.util.Randomizer;


@Description("This element represents an operator that scales two parameters in different directions. " +
        "Each operation involves selecting a scale uniformly at random between scaleFactor and 1/scaleFactor. " +
        "The up parameter is multiplied by this scale and the down parameter is divided by this scale.")
public class UpDownOperator extends Operator {

    public Input<Double> scaleFactorInput = new Input<Double>("scaleFactor",
            "magnitude factor used for scaling", Validate.REQUIRED);
    public Input<List<StateNode>> upInput = new Input<List<StateNode>>("up",
            "zero or more items to scale upwards", new ArrayList<StateNode>());
    public Input<List<StateNode>> downInput = new Input<List<StateNode>>("down",
            "zero or more items to scale downwards", new ArrayList<StateNode>());
    public Input<Boolean> optimiseInput = new Input<Boolean>("optimise", "flag to indicate that the scale factor is automatically changed in order to acheive a good acceptance rate (default true)", true);
    public Input<Boolean> elementWiseInput = new Input<Boolean>("elementWise", "flag to indicate that the scaling is applied to a random index in multivariate parameters (default false)", false);

    double scaleFactor;

    @Override
    public void initAndValidate() throws Exception {
        scaleFactor = scaleFactorInput.get();
        // sanity checks
        if (upInput.get().size() + downInput.get().size() == 0) {
            System.err.println("WARNING: At least one up or down item must be specified");
        }
        if (upInput.get().size() == 0 || downInput.get().size() == 0) {
            System.err.println("WARNING: no " + (upInput.get().size() == 0 ? "up" : "down") + " item specified in UpDownOperator");
        }
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
                    if (outsideBounds(up)) {
                        return Double.NEGATIVE_INFINITY;
                    }
                }

                for (StateNode down : downInput.get()) {
                    down = down.getCurrentEditable(this);
                    goingDown += down.scale(1.0 / scale);
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
            final Parameter<?> p = (Parameter) node;
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
            double fDelta = calcDelta(logAlpha);
            fDelta += Math.log(1.0 / scaleFactor - 1.0);
            scaleFactor = 1.0 / (Math.exp(fDelta) + 1.0);
        }
    }

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(final double fValue) {
        scaleFactor = fValue;
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
