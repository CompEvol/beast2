package beast.core;

/**
 * This interface represents a node of the state. Concrete classes include Parameters and Trees.
 *
 * @author Alexei Drummond
 */
public abstract class StateNode extends Plugin {

    /**
     * @return a deep copy of this node in the state. This will generally be called only for stochastic nodes.
     */
    public abstract StateNode copy();

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(final boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * @return true if this node is acting as a random variable, false if this node is fixed and effectively data.
     */
    final boolean isStochastic() {
        return isStochastic;
    }

    /**
     * @param isStochastic true if this need should be treated as stochastic, false if this node should be fixed
     *                     and treated as data
     */
    final void setStochastic(boolean isStochastic) {
        this.isStochastic = isStochastic;
    }

    public final int getIndex(State state) {
        return index;
    }

    boolean isStochastic = true;

    /**
     * flag to indicate value has changed after operation is performed on state *
     */
    boolean dirty = false;

    /**
     * The index of the parameter for logging et cetera
     */
    int index = -1;

}
