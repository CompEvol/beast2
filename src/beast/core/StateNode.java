package beast.core;

import java.io.PrintStream;

import org.w3c.dom.Node;

/**
 * This interface represents a node of the state. Concrete classes include Parameters and Trees.
 *
 * @author Alexei Drummond
 */
public abstract class StateNode extends Plugin  implements Loggable, Cloneable {
    /** 
     * Pointer to state, null if not part of a State.
     */
    protected State m_state = null;
    /** return this StateNode if it is not in the State
     * If it is in the State, return the version that is currently valid. 
     */
    public StateNode getCurrent() {
    	if (m_state == null) {
    		return this;
    	}
    	return m_state.getStateNode(index);
    }
    /** return StateNode for an operation to do its magic on.
     * The State will make a copy first, if there is not already
     * one available.
     */
    public StateNode getCurrentEditable(Operator operator) {
    	return m_state.getEditableStateNode(this.index, operator);
    }

    /**
     * @return a deep copy of this node in the state. 
     * This will generally be called only for stochastic nodes.
     */
    public abstract StateNode copy();
    /** other := this **/
    public abstract void assignTo(StateNode other);
    /** this := other **/
    public abstract void assignFrom(StateNode other);
    public abstract void toXML(PrintStream out);
    public abstract void fromXML(Node node);

    
    /** getting/setting dirtyness state **/
    boolean somethingIsDirty() {
        return this.somethingIsDirty;
    }
    
    public void setSomethingIsDirty(final boolean isDirty) {
    	this.somethingIsDirty = isDirty;
    }

    abstract public void setEverythingDirty(final boolean isDirty);

    /**
     * @return true if this node is acting as a random variable, false if this node is fixed and effectively data.
     */
    public final boolean isStochastic() {
        return this.isStochastic;
    }

    /**
     * @param isStochastic true if this need should be treated as stochastic, false if this node should be fixed
     *                     and treated as data
     */
    final void setStochastic(boolean isStochastic) {
        this.isStochastic = isStochastic;
    }

    /** Scale StateNode with amount fScale and return the number of
     * degrees of freedom used in this operation. This number varies
     * for the different types of StateNodes. For example, for real
     * valued n-dimensional parameters, it is n, for a tree it is the
     * number of internal nodes being scaled.
     * 
     * throws Exception when StateNode become not valid, e.g. has
     * values outside bounds or negative branch lengths
     */
    abstract public int scale(double fScale) throws Exception;

    boolean isStochastic = true;

    /**
     * flag to indicate some value has changed after operation is performed on state *
     * For multidimensional parameters, there is
     */
    boolean somethingIsDirty = false;

    /**
     * The index of the parameter for logging et cetera
     */
    public int index = -1;

}
