package beast.core;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author
 * @author Andrew Rambaut
 * @version $Id$
 */
public abstract class Operator extends BaseOperator {
    @Override
    public final Distribution getEvaluatorDistribution() {
        return null;
    }

    @Override
    public final double proposal(final Evaluator evaluator) {
        return proposal();
    }

    /** Implement this for proposing a new State.
     * The proposal is responsible for keeping the State valid,
     * and if the State becomes invalid (e.g. a parameter goes out
     * of its range) Double.NEGATIVE_INFINITY should be returned.
     *
     * If the operator is a Gibbs operator, hence the proposal should
     * always be accepted, the method should return Double.POSITIVE_INFINITY.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal
     * should not be accepted (because the proposal is invalid) or
     * Double.POSITIVE_INFINITY if the proposal should always be accepted
     * (for Gibbs operators).
     **/
    abstract public double proposal();
}
