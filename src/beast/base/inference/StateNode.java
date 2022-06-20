package beast.base.inference;


import java.io.PrintStream;
import java.util.List;

import org.w3c.dom.Node;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Loggable;

/**
 * This class represents a node of the state. Concrete classes include Parameters and Trees.
 * StateNodes differ from CalculationNodes in that they
 * 1. Do not calculate anything, with the exception of initialisation time
 * 2. can be changed by Operators
 *
 * @author Alexei Drummond
 */
@Description("A node that can be part of the state.")
public abstract class StateNode extends CalculationNode implements Loggable, Cloneable, Function {
    /**
     * Flag to indicate the StateNode is not constant.
     * This is particularly useful for Beauti *
     */
    final public Input<Boolean> isEstimatedInput = new Input<>("estimate", "whether to estimate this item or keep constant to its initial value", true);

    /**
     * @return this StateNode if it is not in the State.
     *         If it is in the State, return the version that is currently valid
     *         (i.e. not the stored one).
     */
    public StateNode getCurrent() {
        if (state == null) {
            return this;
        }
        return state.getStateNode(index);
    }

    /**
     * @param operator explain here why operator is useful
     * @return StateNode for an operation to do its magic on.
     *         The State will make a copy first, if there is not already
     *         one available.
     */
    public StateNode getCurrentEditable(final Operator operator) {
        startEditing(operator);
        return this;
    }

    /**
     * Getting/setting global dirtiness state for this StateNode.
     * Every StateNode has a flag (somethingIsDirty) that represents whether anything
     * in the state has changed. StateNode implementations like Parameters and Trees
     * have their own internal flag to represent which part of a StateNode (e.g.
     * an element in an array, or a node in a tree) has changed.
     * *
     */
    public boolean somethingIsDirty() {
        return this.hasStartedEditing;
    }

    public void setSomethingIsDirty(final boolean isDirty) {
        this.hasStartedEditing = isDirty;
    }

    /**
     * mark every internal element of a StateNode as isDirty.
     * So both the global flag for this StateNode (somethingIsDirty) should be set as
     * well as all the local flags.
     *
     * @param isDirty
     */
    abstract public void setEverythingDirty(final boolean isDirty);

    /**
     * @return a deep copy of this node in the state.
     *         This will generally be called only for stochastic nodes.
     */
    public abstract StateNode copy();

    /**
     * other := this
     * Assign all values of this to other
     * NB: Should only be used for initialisation!
     */
    public abstract void assignTo(StateNode other);

    /**
     * this := other
     * Assign all values of other to this
     * NB: Should only be used for initialisation!
     */
    public abstract void assignFrom(StateNode other);

    /**
     * As assignFrom, but without copying the ID
     * NB: Should only be used for initialisation!
     */
    public void assignFromWithoutID(StateNode other) {
        final String id = getID();
        assignFrom(other);
        setID(id);
    }

    /**
     * As assignFrom, but only those parts are assigned that
     * are variable, for instance for parameters bounds and dimension
     * do not need to be copied.
     */
    public abstract void assignFromFragile(StateNode other);

    /**
     * for storing a state *
     */
    final public void toXML(PrintStream out) {
        out.print("<statenode id='" + normalise(getID()) + "'>");
        out.print(normalise(toString()));
        out.print("</statenode>\n");
    }

    /**
     * stores a state node in XML format, to be restored by fromXML() *
     */
    final public String toXML() {
        return "<statenode id='" + normalise(getID()) + "'>" +
                normalise(toString()) +
                "</statenode>\n";
    }

    /** ensure XML identifiers get proper escape sequences **/
    private String normalise(String str) {
    	if (str == null) {
    		return null;
    	}
    	str = str.replaceAll("&", "&amp;");    	
    	str = str.replaceAll("'", "&apos;");
    	str = str.replaceAll("\"", "&quot;");
    	str = str.replaceAll("<", "&lt;");
    	str = str.replaceAll(">", "&gt;");
    	return str;
    }

    /**
     * for restoring a state that was stored using toXML() above
     * from a DOM Node. *
     */
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

    /**
     * Scale StateNode with amount scale and
     *
     * @param scale scaling factor
     * @return the number of degrees of freedom used in this operation. This number varies
     *         for the different types of StateNodes. For example, for real
     *         valued n-dimensional parameters, it is n, for a tree it is the
     *         number of internal nodes being scaled.
     * @throws IllegalArgumentException when StateNode become not valid, e.g. has
     *                   values outside bounds or negative branch lengths.
     */
    abstract public int scale(double scale);

    /**
     * Pointer to state, null if not part of a State.
     */
    protected State state = null;

    public State getState() {
        return state;
    }

    /**
     * flag to indicate some value has changed after operation is performed on state
     * For multidimensional parameters, there is an internal flag to indicate which
     * dimension is dirty
     */
    protected boolean hasStartedEditing = false;

    /**
     * The index of the parameter for identifying this StateNode
     * in the State.
     */
    public int index = -1;

    public int getIndex() {
        return index;
    }

    /**
     * should be called before an Operator proposes a new State *
     *
     * @param operator
     */
    public void startEditing(final Operator operator) {
        assert (isCalledFromOperator(4));
        if (hasStartedEditing) {
            // we are already editing
            return;
        }
        hasStartedEditing = true;
        // notify the state

        if (state != null) state.getEditableStateNode(this.index, operator);

        store();
    }

    private boolean isCalledFromOperator(int level) {
        // TODO: sun.reflect.Reflection.getCallerClass is not available in JDK7
        // and alternative methods are really slow according to
        // http://stackoverflow.com/questions/421280/in-java-how-do-i-find-the-caller-of-a-method-using-stacktrace-or-reflection

//    	Class<?> caller = sun.reflect.Reflection.getCallerClass(level);
//    	while (caller != null) {
//    		if (Operator.class.isAssignableFrom(caller)) {
//    			return true;
//    		}
//    		caller = sun.reflect.Reflection.getCallerClass(++level);
//    	}
//    	return false;

        return true;
    }

    @Override
	abstract protected void store();

    /**
     * This is the method actually called by State to restore the StateNode.
     * It ensures the hasStartedEditing flag is reset to false, then calls
     * restore() method that is provided by StateNode implementations.
     */
    public final void restoreStateNode() {
        hasStartedEditing = false;
        restore();
    }

    /**
     * This method (implemented by all StateNode classes) is called by
     * restoreStateNode() to restore the state when a proposal is rejected.
     */
    @Override
	abstract public void restore();

    
    
    /** 
     * Check whether this StateNode is estimated 
     * assuming it is part of an MCMC analysis.
     * @return true iff the "estimate" input is true AND 
     * an operator can be found that operates on this StateNode
     */
    public boolean isEstimated() {
    	if (!isEstimatedInput.get()) {
    		return false;
    	}
    	
    	// is there an operator in the outputs that operates on this StateNode?
    	OperatorSchedule schedule = null;
    	for (BEASTInterface o : getOutputs()) {
    		if (o instanceof Operator) {
    			Operator operator = (Operator) o;
    			schedule = operator.operatorSchedule;
    			List<StateNode> stateNodes = operator.listStateNodes();
    			if (stateNodes.contains(this)) {
    				return true;
    			}
    		}
    	}
    	
    	// is there another operator in the schedule that operates on this StateNode?
    	if (schedule != null) {
    		for (Operator operator : schedule.operators) {
    			List<StateNode> stateNodes = operator.listStateNodes();
    			if (stateNodes.contains(this)) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
} // class StateNode
