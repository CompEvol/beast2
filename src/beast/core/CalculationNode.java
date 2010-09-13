package beast.core;

/**
 * A CalculationNode is a Plugin that perform calculations based on the State.
 * CalculationNodes differ from  StateNodes in that they
 * 1. Calculate something
 * 2. can not be changed by Operators 
 *
 * @author Andrew Rambaut
 */
@Description("Plugin that perform calculations based on the State.")
public class CalculationNode extends Plugin {

    //=================================================================
    // The API of CalculationNode. These 3 functions (store/restore/requireCalculation)
    // can be overridden to increase efficiency by caching internal calculations.
	// General default implementations are provided.
    //=================================================================

    /** Store internal calculations
     *
     * This is called prior to the proposal of a new state.
     *
     **/
    protected void store() {
    	isDirty = false;
    }


    /** Check whether internal calculations need to be updated
     * 
     * This is called after a proposal of a new state.
     * A CalculationNode that needs a custom implementation should 
     * override requiresRecalculation()
     */
    final void checkDirtiness() {
        isDirty = requiresRecalculation();
    }

    /**
     * @return whether the API for the particular Plugin returns different
     * answers than before the operation was applied.
     *
     * This method is called before the CalculationNode do their calculations.
     * Called in order of the partial order defined by Input-Plugin relations.
     * Called only on those CalculationNodes potentially affected by a
     * StateNode change.
     *
     * Default implementation inspects all input plugins
     * and checks if there is any dirt anywhere.
     * Derived classes can provide a more efficient implementation
     * by checking which part of any input StateNode or Plugin has changed.
     *
     * Note this default implementation is relative expensive since it uses 
     * introspection, so overrides should be preferred.
     * After the operation has changed the state.
     */
    protected boolean requiresRecalculation() {
        try {
            for (Plugin plugin : listActivePlugins()) {
                if (plugin instanceof StateNode && ((StateNode)plugin).somethingIsDirty()) {
                	return true;
                }

                if (plugin instanceof CalculationNode && ((CalculationNode)plugin).isDirtyCalculation()) {
                    return true;
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return false;
    }

    /** Restore internal calculations
     *
     * This is called when a proposal is rejected
     **/
    protected void restore() {
    	isDirty = false;
    }

    /** Accept internal state and mark internal calculations as current
    *
    * This is called when a proposal is accepted
    **/
    protected void accept() {
    	isDirty = false;
    }

    /**
     * @return true if the node became dirty - that is needs to recalculate due to
     * changes in the inputes.
     *
     * CalcalationNodes typically know whether an input is a CalculationNode or StateNode
     * and also know whether the input is Validate.REQUIRED, hence cannot be null.
     * Further, for CalculationNodes, a shadow parameter can be kept so that a
     * call to Input.get() can be saved.
     * Made public to squeeze out a few cycles and save a few seconds in
     * calculation time by calling this directly instead of calling isDirty()
     * on the associated input.
     */
    final public boolean isDirtyCalculation() {
        return isDirty;
    }

    /**
     * flag to indicate whether this node will be updating its calculations
     */
    private boolean isDirty = true;

} // class CalculationNode