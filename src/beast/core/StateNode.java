package beast.core;

import java.io.PrintStream;

import org.w3c.dom.Node;

/**
 * This class represents a node of the state. Concrete classes include Parameters and Trees.
 * StateNodes differ from CalculationNodes in that they
 * 1. Do not calculate anything, with the exception of initialisation time
 * 2. can be changed by Operators 
 *
 * @author Alexei Drummond
 */
@Description("A node that can be part of the state.")
public abstract class StateNode extends Plugin  implements Loggable, Cloneable, Valuable {
	/** Flag to indicate the StateNode is not constant.
	 * This is particularly useful for Beauti **/
	public Input<Boolean> m_bIsEstimated = new Input<Boolean>("estimate", "whether to estimate this item or keep constant to its initial value", true); 
	
	/** Returns this StateNode if it is not in the State.
     * If it is in the State, return the version that is currently valid 
     * (i.e. not the stored one). 
     */
    public StateNode getCurrent() {
    	if (m_state == null) {
    		return this;
    	}
    	return m_state.getStateNode(index);
    }
    /** Return StateNode for an operation to do its magic on.
     * The State will make a copy first, if there is not already
     * one available.
     * @param operator explain here why operator is useful
     */
    public StateNode getCurrentEditable(Operator operator) {
    	return m_state.getEditableStateNode(this.index, operator);
    }

    /** Getting/setting global dirtiness state for this StateNode.
     * Every StateNode has a flag (somethingIsDirty) that represents whether anything
     * in the state has changed. StateNode implementations like Parameters and Trees
     * have their own internal flag to represent which part of a StateNode (e.g.
     * an element in an array, or a node in a tree) has changed.
     * **/
    public boolean somethingIsDirty() {
        return this.somethingIsDirty;
    }
    
    public void setSomethingIsDirty(final boolean isDirty) {
    	this.somethingIsDirty = isDirty;
    }
    /** mark every internal element of a StateNode as isDirty.
     * So both the global flag for this StateNode (somethingIsDirty) should be set as
     * well as all the local flags.
     * @param isDirty
     */
    abstract public void setEverythingDirty(final boolean isDirty);

    /**
     * @return a deep copy of this node in the state. 
     * This will generally be called only for stochastic nodes.
     */
    public abstract StateNode copy();
    
    /** other := this 
     * Assign all values of this to other **/
    public abstract void assignTo(StateNode other);
    
    /** this := other 
    * Assign all values of other to this **/
    public abstract void assignFrom(StateNode other);
    
    /** As assignFrom, but only those parts are assigned that 
     * are variable, for instance for parameters bounds and dimension
     * do not need to be copied.
     */
    public abstract void assignFromFragile(StateNode other);

    /** for storing a state **/
    final public void toXML(PrintStream out) {
    	out.print("<statenode id='" + getID() +"'>");
    	out.print(toString());
    	out.print("</statenode>\n");
    }
    
    /** for restoring a state that was stored using toXML() above. **/
    public abstract void fromXML(Node node);

    

//    /**
//     * @return true if this node is acting as a random variable, false if this node is fixed and effectively data.
//     */
//    public final boolean isStochastic() {
//        return this.isStochastic;
//    }
//
//    /**
//     * @param isStochastic true if this need should be treated as stochastic, false if this node should be fixed
//     *                     and treated as data
//     */
//    final void setStochastic(boolean isStochastic) {
//        this.isStochastic = isStochastic;
//    }
//
//    boolean isStochastic = true;

    /** Scale StateNode with amount fScale and
     * @return the number of degrees of freedom used in this operation. This number varies
     * for the different types of StateNodes. For example, for real
     * valued n-dimensional parameters, it is n, for a tree it is the
     * number of internal nodes being scaled.
     * 
     * @throws Exception when StateNode become not valid, e.g. has
     * values outside bounds or negative branch lengths.
     *
     * @param fScale scaling factor
     */
    abstract public int scale(double fScale) throws Exception;
    
    
//    abstract public int getDimension();
//    abstract public double getArrayValue();
//    abstract public double getArrayValue(int iValue);

    /** 
     * Pointer to state, null if not part of a State.
     */
    protected State m_state = null;
    
    /**
     * flag to indicate some value has changed after operation is performed on state *
     * For multidimensional parameters, there is an internal flag to indicate
     */
    boolean somethingIsDirty = false;

    /**
     * The index of the parameter for identifying this StateNode 
     * in the State.
     */
    public int index = -1;

} // class StateNode
