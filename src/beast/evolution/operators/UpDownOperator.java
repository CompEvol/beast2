package beast.evolution.operators;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Operator;
import beast.core.StateNode;
import beast.util.Randomizer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Description("This element represents an operator that scales two parameters in different directions. " +
        "Each operation involves selecting a scale uniformly at random between scaleFactor and 1/scaleFactor. " +
        "The up parameter is multiplied by this scale and the down parameter is divided by this scale.")
public class UpDownOperator extends Operator {

    public Input<Double> m_scaleFactor = new Input<Double>("scaleFactor",
            "magnitude factor used for scaling", Validate.REQUIRED);
    public Input<List<StateNode>> m_up = new Input<List<StateNode>>("up",
            "zero or more items to scale upwards", new ArrayList<StateNode>());
    public Input<List<StateNode>> m_down = new Input<List<StateNode>>("down",
            "zero or more items to scale downwards", new ArrayList<StateNode>());
    public Input<Boolean> m_bOptimise = new Input<Boolean>("optimise", "flag to indicate that the scale factor is automatically changed in order to acheive a good acceptance rate (default true)", true);

    double m_fScaleFactor;

    @Override
    public void initAndValidate() throws Exception {
        m_fScaleFactor = m_scaleFactor.get();
        // sanity checks
        if (m_up.get().size() + m_down.get().size() == 0) {
            throw new Exception("At least one up or down item must be specified");
        }
        if (m_up.get().size() == 0 || m_down.get().size() == 0) {
            System.err.println("WARNING: no " + (m_up.get().size() == 0 ? "up" : "down") + " item specified in UpDownOperator");
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

        final double scale = (m_fScaleFactor + (Randomizer.nextDouble() * ((1.0 / m_fScaleFactor) - m_fScaleFactor)));
        int goingUp = 0, goingDown = 0;

        try {
            for (StateNode up : m_up.get()) {
                up = up.getCurrentEditable(this);
                goingUp += up.scale(scale);
            }

            for (StateNode down : m_down.get()) {
                down = down.getCurrentEditable(this);
                goingDown += down.scale(1.0 / scale);
            }
        } catch (Exception e) {
            // scale resulted in invalid StateNode, abort proposal
            return Double.NEGATIVE_INFINITY;
        }
        return (goingUp - goingDown - 2) * Math.log(scale);
    }

    /**
     * automatic parameter tuning *
     */
    @Override
    public void optimize(double logAlpha) {
        if (m_bOptimise.get()) {
            double fDelta = calcDelta(logAlpha);
            fDelta += Math.log(1.0 / m_fScaleFactor - 1.0);
            m_fScaleFactor = 1.0 / (Math.exp(fDelta) + 1.0);
        }
    }

    @Override
    public double getCoercableParameterValue() {
        return m_fScaleFactor;
    }

    @Override
    public void setCoercableParameterValue(double fValue) {
        m_fScaleFactor = fValue;
    }

    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double sf = Math.pow(m_fScaleFactor, ratio);

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else if (prob > 0.40) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else return "";
    }
} // class UpDownOperator
