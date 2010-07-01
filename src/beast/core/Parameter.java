/*
* File Parameter.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
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

import java.util.Arrays;


@Description("A parameter represents a value in the state space that can be changed " +
        "by operators.")
public class Parameter extends StateNode {
    public Input<Double> m_pValues = new Input<Double>("value", "start value for this parameter");
    public Input<Double> m_pUpper = new Input<Double>("upper", "upper value allowed for this parameter (default +INFINITY)", new Double(Double.POSITIVE_INFINITY));
    public Input<Double> m_pLower = new Input<Double>("lower", "lower value allowed for this parameter (default -INFINITY)", new Double(Double.NEGATIVE_INFINITY));
    public Input<Integer> m_nDimension = new Input<Integer>("dimension", "dimension (default 1)", new Integer(1));

    /**
     * constructors *
     */
    public Parameter() {
    }

    /**
     * upper & lower bound *
     */
    protected double m_fUpper;
    protected double m_fLower;
    /**
     * the actual values of this parameter
     */
    protected double[] values;

    /** number of the id, to find it quickly in the list of parameters of the State **/
    /**
     * initialised by State.initAndValidate *
     */

    @Override
    public void initAndValidate(State state) throws Exception {
        m_fUpper = m_pUpper.get();
        m_fLower = m_pLower.get();
        values = new double[m_nDimension.get()];
        for (int i = 0; i < values.length; i++) {
            values[i] = m_pValues.get();
        }
    }

    /**
     * defaults used if the user does not provide them in the XML *
     */
    public static final double DEFAULT_LOWER = Double.NEGATIVE_INFINITY;
    public static final double DEFAULT_UPPER = Double.POSITIVE_INFINITY;

    /**
     * various setters & getters *
     */
    public int getDimension() {
        return values.length;
    }

    public double getValue() {
        return values[0];
    }

    public int intValue() {
        return (int) values[0];
    }

    public boolean booleanValue() {
        return (values[0] != 0.0);
    }

    public double getLower() {
        return m_fLower;
    }

    public void setLower(double fLower) {
        m_fLower = fLower;
    }

    public double getUpper() {
        return m_fUpper;
    }

    public void setUpper(double fUpper) {
        m_fUpper = fUpper;
    }

    public double getValue(int iParam) {
        return values[iParam];
    }

    public double[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    public void setBounds(double fLower, double fUpper) {
        m_fLower = fLower;
        m_fUpper = fUpper;
    }

    public void setValue(double fValue) throws Exception {
        if (isStochastic) {
            values[0] = fValue;
            setDirty(true);
        } else throw new Exception("Can't set the value of a fixed parameter.");
    }

    public void setIntValue(int nValue) throws Exception {
        setValue(nValue);
    }

    public void setBooleanValue(boolean bValue) throws Exception {
        setValue(bValue ? 1.0 : 0.0);
    }

    public void setValue(int iParam, double fValue) throws Exception {
        if (isStochastic) {
            values[iParam] = fValue;
            setDirty(true);
        } else throw new Exception("Can't set the value of a fixed parameter.");
    }

    public void setIntValue(int iParam, int nValue) throws Exception {
        setValue(iParam, nValue);
    }

    public void setBooleanValue(int iParam, boolean bValue) throws Exception {
        setValue(iParam, bValue ? 1.0 : 0.0);
    }

    /**
     * deep copy *
     */
    public Parameter copy() {
        Parameter copy = new Parameter();
        copy.m_sID = m_sID;
        copy.values = new double[values.length];
        System.arraycopy(values, 0, copy.values, 0, values.length);
        copy.m_fLower = m_fLower;
        copy.m_fUpper = m_fUpper;
        return copy;
    }

    public void prepare() throws Exception {
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(m_sID);
        buf.append(": ");
        for (int i = 0; i < values.length; i++) {
            buf.append(values[i] + " ");
        }
        return buf.toString();
    }
} // class Parameter
