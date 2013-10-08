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
package beast.core;


import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import beast.core.Input.Validate;
import beast.core.util.Evaluator;



@Description("Proposes a move in state space.")
public abstract class Operator extends BEASTObject {
    public Input<Double> m_pWeight = new Input<Double>("weight", "weight with which this operator is selected", Validate.REQUIRED);

    /**
     * the schedule used for auto optimisation *
     */
    OperatorSchedule operatorSchedule;

    public void setOperatorSchedule(OperatorSchedule operatorSchedule) {
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
     * <p/>
     * If the operator is a Gibbs operator, hence the proposal should
     * always be accepted, the method should return Double.POSITIVE_INFINITY.
     *
     * @param evaluator An evaluator object that can be use to repetitively
     *                  used to evaluate the distributribution returned by getEvaluatorDistribution().
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal
     *         should not be accepted (because the proposal is invalid) or
     *         Double.POSITIVE_INFINITY if the proposal should always be accepted
     *         (for Gibbs operators).
     */
    public double proposal(final Evaluator evaluator) {
        return proposal();
    }

    /**
     * Implement this for proposing a new State.
     * The proposal is responsible for keeping the State valid,
     * and if the State becomes invalid (e.g. a parameter goes out
     * of its range) Double.NEGATIVE_INFINITY should be returned.
     * <p/>
     * If the operator is a Gibbs operator, hence the proposal should
     * always be accepted, the method should return Double.POSITIVE_INFINITY.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal
     *         should not be accepted (because the proposal is invalid) or
     *         Double.POSITIVE_INFINITY if the proposal should always be accepted
     *         (for Gibbs operators).
     */
    abstract public double proposal();

    /**
     * @return the relative weight which determines the probability this proposal is chosen
     *         from among all the available proposals
     */
    public double getWeight() {
        return m_pWeight.get();
    }

    public String getName() {
        return this.getClass().getName() + (getID() != null ? "_" + getID() : "");
    }

    /**
     * keep statistics of how often this operator was used, accepted or rejected *
     */
    protected int m_nNrRejected = 0;
    protected int m_nRrAccepted = 0;
    protected int m_nRrRejectedForCorrection = 0;
    protected int m_nRrAcceptedForCorrection = 0;

    public void accept() {
        m_nRrAccepted++;
        if (operatorSchedule.autoOptimizeDelayCount >= operatorSchedule.autoOptimizeDelay) {
            m_nRrAcceptedForCorrection++;
        }
    }

    public void reject() {
        m_nNrRejected++;
        if (operatorSchedule.autoOptimizeDelayCount >= operatorSchedule.autoOptimizeDelay) {
            m_nRrRejectedForCorrection++;
        }
    }

    /**
     * called after every invocation of this operator to see whether
     * a parameter can be optimised for better acceptance hence faster
     * mixing
     *
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     */
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
    }

    /**
     * @param logAlpha difference in posterior between previous state & proposed state + hasting ratio
     * @return change of value of a parameter for MCMC chain optimisation
     */
    protected double calcDelta(double logAlpha) {
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
     */
    public void setCoercableParameterValue(double fValue) {
    }

    /**
     * return directions on how to set operator parameters, if any *
     */
    public String getPerformanceSuggestion() {
        return "";
    }

    /**
     * return list of state nodes that this operator operates on.
     * state nodes that are input to the operator but are never changed
     * in a proposal should not be listed
     */
    public List<StateNode> listStateNodes() throws Exception {
        // pick up all inputs that are stateNodes that are estimated
        List<StateNode> list = new ArrayList<StateNode>();
        for (BEASTObject o : listActivePlugins()) {
            if (o instanceof StateNode) {
                StateNode stateNode = (StateNode) o;
                if (stateNode.isEstimatedInput.get()) {
                    list.add(stateNode);
                }
            }
        }
        return list;
    }

    public String toString() {
        String sName = getName();
        if (sName.length() < 70) {
            sName += "                                                                      ".substring(sName.length(), 70);
        }
        DecimalFormat format = new DecimalFormat("#.###");
        if (!Double.isNaN(getCoercableParameterValue())) {
            String sStr = getCoercableParameterValue() + "";
            sName += sStr.substring(0, Math.min(sStr.length(), 5));
        } else {
            sName += "     ";
        }
        sName += " ";
        return sName + "\t" + m_nRrAccepted + "\t" + m_nNrRejected + "\t" +
                (m_nRrAccepted + m_nNrRejected) + "\t" +
                format.format(((m_nRrAccepted + 0.0) / (m_nRrAccepted + m_nNrRejected))) +
                " " + getPerformanceSuggestion();
    }

    /** store to state file, so on resume the parameter tuning is restored **/
	public void storeToFile(PrintStream out) {
        out.print("{id:\"" + getID() + '"' +
        		", p:" + getCoercableParameterValue() +
        		", accept:" + m_nRrAccepted + 
        		", reject:" + m_nNrRejected + 
        		", acceptFC:" + m_nRrAcceptedForCorrection +  
        		", rejectFC:" + m_nRrRejectedForCorrection +  
        		"}"
                );
	}

	public void restoreFromFile(JSONObject o) {
        if (!Double.isNaN(o.getDouble("p"))) {
            setCoercableParameterValue(o.getDouble("p"));
        }
        m_nRrAccepted = o.getInt("accept");
        m_nNrRejected = o.getInt("reject");
        m_nRrAcceptedForCorrection = o.getInt("acceptFC");
        m_nRrRejectedForCorrection = o.getInt("rejectFC");
	}

} // class Operator
