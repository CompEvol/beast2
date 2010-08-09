package beast.core;


/**
 * This base class is for Plugins that perform calculations based on the State.
 *
 * @author Andrew Rambaut
 */
public abstract class CalculationNode extends Plugin {

    // Package private because it shouldn't be called outside of the core package -
    // To check whether it is dirty, call isDirty on the specific Input.
    final boolean isDirty() {
        if (!hasCheckedDirtiness) {
            checkDirtiness();
        }
        return isDirty;
    }

    final void checkDirtiness() {
//        if (!_STATE_IN_DIRTINESS_CHECKING_MODE) {
//            throw Exception("Dirtyness should have been checked by now");
//        }

        if (hasCheckedDirtiness) return;
        if (hasDirtyInputs()) {
            isDirty = requiresRecalculation();
        }

        hasCheckedDirtiness = true;
    }

    private final boolean hasDirtyInputs() {
        try {
            for (Input<?> input : listInputs()) {
                if (input.isDirty()) return true;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return false;
    }

    final void resetDirtiness() {
        hasCheckedDirtiness = false;    
    }

// RRB: these methods confused me quite a lot, so I burried them and
//      renamed (re)storeCalculations => (re)store  
//    void store() {
//        storeCalculations();
//    }
//
//    void restore() {
//        restoreCalculations();
//    }

    //=================================================================
    // The API of CalculationNode. These 3 functions can be overridden
    // to increase efficiency by cacheing internal calculations.
    //=================================================================

    /** Store internal calculations
     *
     * This is called prior to the proposal of a new state
     **/
    protected void store() {
    	isDirty = false;
    }

    /** reverse of storeCalculations
     *
     * This is called when a proposal is rejected
     **/
    protected void restore() {
    	isDirty = false;
    }

    protected boolean requiresRecalculation() {
        return true;
    }

    /**
     * flag to indicate whether this node will be updating its calculations
     */
    private boolean isDirty = true;

    /**
     * flag to indicate whether dirtiness has been checked
     */
    private boolean hasCheckedDirtiness = false;

}