package beast.base.inference;

import beast.base.core.BEASTObject;
import beast.base.core.Description;

/**
 * A CalculationNode is a BEAST Object that perform calculations based on the State.
 * CalculationNodes differ from  StateNodes in that they
 * 1. Calculate something
 * 2. can not be changed by Operators.
 *
 * Calculations are functions,  StateNodes are variables.
 *
 * @author Andrew Rambaut
 */
@Description("BEASTObject that performs calculations based on the State.")
public abstract class CalculationNode extends BEASTObject {

    //=================================================================
    // The API of CalculationNode. These 3 functions (store/restore/requireCalculation)
    // can be overridden to increase efficiency by caching internal calculations.
    // General default implementations are provided.
    //=================================================================

    /**
     * Store internal calculations. Called before a calculation node
     * is asked to perform any calculations, but after some part of the
     * state has changed through a operator proposal.
     * <p/>
     * This is not meant to be used to calculate anything, just store
     * intermediate results of calculations. Input values should not
     * be accessed because some StateNodes may have been changed.
     */
    protected void store() {
        isDirty = false;
    }


    /**
     * Check whether internal calculations need to be updated
     * <p/>
     * This is called after a proposal of a new state.
     * A CalculationNode that needs a custom implementation should
     * override requiresRecalculation()
     */
    final void checkDirtiness() {
        isDirty = requiresRecalculation();
    }

    /**
     * @return whether the API for the particular BEASTObject returns different
     *         answers than before the operation was applied.
     *         <p/>
     *         This method is called before the CalculationNode do their calculations.
     *         Called in order of the partial order defined by Input-BEASTObject relations.
     *         Called only on those CalculationNodes potentially affected by a
     *         StateNode change.
     *         <p/>
     *         Default implementation return 'true', since requiresRecalculation is
     *         called for a node only if one of its arguments has changed.
     */
    protected boolean requiresRecalculation() {
        return true;


//        *         <p/>
//        *         Default implementation inspects all input beastObjects
//        *         and checks if there is any dirt anywhere.
//        *         Derived classes can provide a more efficient implementation
//        *         by checking which part of any input StateNode or BEASTObject has changed.
//        *         <p/>
//        *         Note this default implementation is relative expensive since it uses
//        *         introspection, so overrides should be preferred.
//        *         After the operation has changed the state.state
        // this is a prototypical implementation of requiresRecalculation()
//        try {
//            for (BEASTObject beastObject : listActivePlugins()) {
//                if (beastObject instanceof StateNode && ((StateNode)beastObject).somethingIsDirty()) {
//                	return true;
//                }
//
//                if (beastObject instanceof CalculationNode && ((CalculationNode)beastObject).isDirtyCalculation()) {
//                    return true;
//                }
//            }
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//
//        return false;
    }

    /**
     * Restore internal calculations
     * <p/>
     * This is called when a proposal is rejected
     */
    protected void restore() {
        isDirty = false;
    }

    /**
     * Accept internal state and mark internal calculations as current
     * <p/>
     * This is called when a proposal is accepted
     */
    protected void accept() {
        isDirty = false;
    }

    /**
     * @return true if the node became dirty - that is needs to recalculate due to
     *         changes in the inputs.
     *         <p/>
     *         CalcalationNodes typically know whether an input is a CalculationNode or StateNode
     *         and also know whether the input is Validate.REQUIRED, hence cannot be null.
     *         Further, for CalculationNodes, a shadow parameter can be kept so that a
     *         call to Input.get() can be saved.
     *         Made public to squeeze out a few cycles and save a few seconds in
     *         calculation time by calling this directly instead of calling isDirty()
     *         on the associated input.
     */
    final public boolean isDirtyCalculation() {
        return isDirty;
    }

    /**
     * flag to indicate whether this node will be updating its calculations
     */
    private boolean isDirty = false;

    /**
     * Compute a checksum of this calculation node. Checksums are used for validity checks, to ensure
     * that state nodes and fat calculation nodes are correctly restored. The base implementation will
     * never trigger the validity checks and must be overwritten to make use of them. Lean calculation
     * nodes should NOT overwrite this method, as they do not need to be restored and hence checksum
     * comparisons could lead to false alarms.
     *
     * @return checksum of the calculation node
     */
    protected int getChecksum() {
        return 0;
    }

    /**
     * The checksum of the calculation node, evaluated and stored before an operator proposes an MCMC step.
     */
    protected int preOperatorChecksum;

    /**
     * Store the current checksum as the ´preOperatorChecksum´.
     */
    public void storeChecksum() {
        preOperatorChecksum = getChecksum();
    }

    /**
     * Check whether the current checksum matches the ´preOperatorChecksum´.
     * @return true iff the checksums match
     */
    public boolean matchesOldChecksum() {
        return preOperatorChecksum == getChecksum();
    }

} // class CalculationNode
