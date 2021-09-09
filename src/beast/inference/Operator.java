/*
* File Operator.java
*
* Copyright (C) 2011 BEAST2 Core Team
*
* This file is part of BEAST2.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/
package beast.inference;


import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
//import org.json.JSONWriter;
import org.json.JSONStringer;

import beast.base.BEASTInterface;
import beast.base.BEASTObject;
import beast.base.Description;
import beast.base.Input;
import beast.base.Input.Validate;

@Description("Proposes a move in state space.")
public abstract class Operator extends BEASTObject {
    final public Input<Double> m_pWeight = new Input<>("weight", "weight with which this operator is selected", Validate.REQUIRED);

    private final String STANDARD_OPERATOR_PACKAGE = "beast.evolution.operators";

    /**
     * the schedule used for auto optimisation *
     */
    OperatorSchedule operatorSchedule;

    public void setOperatorSchedule(final OperatorSchedule operatorSchedule) {
        this.operatorSchedule = operatorSchedule;
    }

    /**
     * Implement this for proposing new states based on evaluations of
     * a distribution. By default it returns null but can be overridden
     * to implement more complex proposals.
     *
     * @return a distribution or null if not required
     */
    public Distribution getEvaluatorDistribution() {
        return null;
    }

    /**
     * Implement this for proposing a new State.
     * The proposal is responsible for keeping the State valid,
     * and if the State becomes invalid (e.g. a parameter goes out
     * of its range) Double.NEGATIVE_INFINITY should be returned.
     * <p>
     * If the operator is a Gibbs operator, hence the proposal should
     * always be accepted, the method should return Double.POSITIVE_INFINITY.
     *
     * @param evaluator An evaluator object that can be use to repetitively
     *                  used to evaluate the distribution returned by getEvaluatorDistribution().
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal
     * should not be accepted (because the proposal is invalid) or
     * Double.POSITIVE_INFINITY if the proposal should always be accepted
     * (for Gibbs operators).
     */
    public double proposal(final Evaluator evaluator) {
        return proposal();
    }

    /**
     * Implement this for proposing a new State.
     * The proposal is responsible for keeping the State valid,
     * and if the State becomes invalid (e.g. a parameter goes out
     * of its range) Double.NEGATIVE_INFINITY should be returned.
     * <p>
     * If the operator is a Gibbs operator, hence the proposal should
     * always be accepted, the method should return Double.POSITIVE_INFINITY.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal
     * should not be accepted (because the proposal is invalid) or
     * Double.POSITIVE_INFINITY if the proposal should always be accepted
     * (for Gibbs operators).
     */
    abstract public double proposal();

    /**
     * @return the relative weight which determines the probability this proposal is chosen
     * from among all the available proposals
     */
    public double getWeight() {
        return m_pWeight.get();
    }

    public String getName() {

        String className = this.getClass().getName();
        if (className.startsWith(STANDARD_OPERATOR_PACKAGE)) {
            className = className.substring(STANDARD_OPERATOR_PACKAGE.length() + 1);
        }
        return className + "(" + (getID() != null ? getID() : "") + ")";
    }

    /**
     * keep statistics of how often this operator was used, accepted or rejected *
     */
    protected int m_nNrRejected = 0;
    protected int m_nNrAccepted = 0;
    protected int m_nNrRejectedForCorrection = 0;
    protected int m_nNrAcceptedForCorrection = 0;

    private final boolean detailedRejection = false;
    // rejected because likelihood is infinite
    protected int m_nNrRejectedInvalid = 0;
    // rejected because operator failed (sub-group of above)
    protected int m_nNrRejectedOperator = 0;

    public void accept() {
        m_nNrAccepted++;
        if (operatorSchedule.autoOptimizeDelayCount >= operatorSchedule.autoOptimizeDelay) {
            m_nNrAcceptedForCorrection++;
        }
    }

    public void reject() {
        reject(0); // silly hack
    }

    // 0 like finite  -1 like -inf -2 operator failed
    public void reject(final int reason) {
        m_nNrRejected++;
        if (reason < 0) {
            ++m_nNrRejectedInvalid;
            if (reason == -2) {
                ++m_nNrRejectedOperator;
            }
        }
        if (operatorSchedule.autoOptimizeDelayCount >= operatorSchedule.autoOptimizeDelay) {
            m_nNrRejectedForCorrection++;
        }
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    public void optimize(final double logAlpha) {
        // must be overridden by operator implementation to have an effect
    }

    /**
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     * @return change of value of a parameter for MCMC chain optimisation
     */
    protected double calcDelta(final double logAlpha) {
        return operatorSchedule.calcDelta(this, logAlpha);
    } // calcDelta

    /**
     * @return target for automatic operator optimisation
     */
    public double getTargetAcceptanceProbability() {
        return 0.234;
    }

    /**
     * @return value changed through automatic operator optimisation
     */
    public double getCoercableParameterValue() {
        return Double.NaN;
    }

    /**
     * set value that changed through automatic operator optimisation
     *
     * @param value
     */
    public void setCoercableParameterValue(final double value) {
    }

    /**
     * return directions on how to set operator parameters, if any *
     *
     * @return
     */
    public String getPerformanceSuggestion() {
        return "";
    }

    /**
     * return list of state nodes that this operator operates on.
     * state nodes that are input to the operator but are never changed
     * in a proposal should not be listed
     */
    public List<StateNode> listStateNodes() {
        // pick up all inputs that are stateNodes that are estimated
        final List<StateNode> list = new ArrayList<>();
        for (BEASTInterface o : listActiveBEASTObjects()) {
            if (o instanceof StateNode) {
                final StateNode stateNode = (StateNode) o;
                if (stateNode.isEstimatedInput.get()) {
                    list.add(stateNode);
                }
            }
        }
        return list;
    }

    @Override
	public String toString() {
        return OperatorSchedule.prettyPrintOperator(this, 70, 10, 5,
                operatorSchedule != null ? operatorSchedule.getNormalizedWeight(this) : 0.0, detailedRejection);
    }

    /**
     * Store to state file, so on resume the parameter tuning is restored.
     * By default, it stores information in JSON for example
     * <p>
     * {"id":"kappaScaler", "p":0.5, "accept":39, "reject":35, "acceptFC":0, "rejectFC":0}
     * <p>
     * Meta-operators (operators that have one or more operators as inputs)
     * need to override this method to store the tuning information associated
     * with their sub-operators by generating nested JSON, for example
     * <p>
     * {"id":"metaOperator", "p":0.5, "accept":396, "reject":355, "acceptFC":50, "rejectFC":45,
     * operators [
     * {"id":"kappaScaler1", "p":0.5, "accept":39, "reject":35, "acceptFC":0, "rejectFC":0}
     * {"id":"kappaScaler2", "p":0.5, "accept":39, "reject":35, "acceptFC":0, "rejectFC":0}
     * ]
     * }
     * *
     */
    public void storeToFile(final PrintWriter out) {
    	try {
	        JSONStringer json = new JSONStringer();
	        json.object();
	
	        if (getID()==null)
	           setID("unknown");
	
	        json.key("id").value(getID());
	
	        double p = getCoercableParameterValue();
	        if (Double.isNaN(p)) {
	            json.key("p").value("NaN");
	        } else if (Double.isInfinite(p)) {
	        	if (p > 0) {
	        		json.key("p").value("Infinity");
	        	} else {
	        		json.key("p").value("-Infinity");
	        	}
	        } else {
	            json.key("p").value(p);
	        }
	        json.key("accept").value(m_nNrAccepted);
	        json.key("reject").value(m_nNrRejected);
	        json.key("acceptFC").value(m_nNrAcceptedForCorrection);
	        json.key("rejectFC").value(m_nNrRejectedForCorrection);
	        json.key("rejectIv").value(m_nNrRejectedInvalid);
	        json.key("rejectOp").value(m_nNrRejectedOperator);
	        json.endObject();
	        out.print(json.toString());
    	} catch (JSONException e) {
    		// failed to log operator in state file
    		// report and continue
    		e.printStackTrace();
    	}
    }

    /**
     * Restore tuning information from file
     * Override this method for meta-operators (see also storeToFile).
     */
    public void restoreFromFile(JSONObject o) {
    	try {
	        if (!Double.isNaN(o.getDouble("p"))) {
	            setCoercableParameterValue(o.getDouble("p"));
	        }
	        m_nNrAccepted = o.getInt("accept");
	        m_nNrRejected = o.getInt("reject");
	        m_nNrAcceptedForCorrection = o.getInt("acceptFC");
	        m_nNrRejectedForCorrection = o.getInt("rejectFC");
	
	        m_nNrRejectedInvalid = o.has("rejectIv") ? o.getInt("rejectIv") : 0;
	        m_nNrRejectedOperator = o.has("rejectOp") ? o.getInt("rejectOp") : 0;
    	} catch (JSONException e) {
    		// failed to restore from state file
    		// report and continue
    		e.printStackTrace();
    	}
    }


    /**
     * indicates that the state needs to be initialised so that
     * BEASTObjects can be identified that need updating. This
     * almost always needs to happen, except for cases where the
     * operator already initialised the state, e.g. for delayed
     * acceptance operators.
     */
    public boolean requiresStateInitialisation() {
        return true;
    }
    
    // Added for coupled MCMC
    public int get_m_nNrAccepted(){
    	return m_nNrAccepted;
    }
    public int get_m_nNrRejected(){
    	return m_nNrRejected;
    }
    public int get_m_nNrAcceptedForCorrection(){
    	return m_nNrAcceptedForCorrection;
    }
    public int get_m_nNrRejectedForCorrection(){
    	return m_nNrRejectedForCorrection;
    }
    
    // Added for coupled MCMC
    public void setAcceptedRejected(int m_nNrAccepted, int m_nNrRejected, int m_nNrAcceptedForCorrection, int m_nNrRejectedForCorrection){
    	this.m_nNrAccepted = m_nNrAccepted;
    	this.m_nNrRejected = m_nNrRejected;
    	this.m_nNrAcceptedForCorrection = m_nNrAcceptedForCorrection;
    	this.m_nNrRejectedForCorrection = m_nNrRejectedForCorrection;
    }
    
} // class Operator
