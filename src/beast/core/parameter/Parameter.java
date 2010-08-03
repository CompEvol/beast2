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
package beast.core.parameter;

import beast.core.*;

import java.io.PrintStream;
import java.util.Arrays;


@Description("A parameter represents a value in the state space that can be changed " +
        "by operators.")
public abstract class Parameter<T> extends StateNode implements Loggable {
    public Input<java.lang.Integer> m_nDimension = new Input<java.lang.Integer>("dimension", "dimension of the paperameter(default 1)", new java.lang.Integer(1));


    /**
     * constructors *
     */
    public Parameter() {
    }

    @Override
    public void initAndValidate() throws Exception {
        m_bIsDirty = new boolean[m_nDimension.get()];
    }


    /**
     * upper & lower bound *
     */
    protected T m_fUpper;
    protected T m_fLower;
    /**
     * the actual values of this parameter
     */
    protected T[] values;
    /**
     * isDirty flags for individual elements in high dimensional parameters
     */
    protected boolean[] m_bIsDirty;

    public boolean isDirty(int iParam) {
        return m_bIsDirty[iParam];
    }
//    @Override
//    public void setDirty(final boolean dirty) {
//    	super.setDirty(dirty);
//    	Arrays.fill(m_bIsDirty, dirty);
//    }

    /** number of the id, to find it quickly in the list of parameters of the State **/
    /**
     * initialised by State.initAndValidate *
     */

    /**
     * various setters & getters *
     */
    public int getDimension() {
        return values.length;
    }

    public T getValue() {
        return values[0];
    }

    public T getLower() {
        return m_fLower;
    }

    public void setLower(T fLower) {
        m_fLower = fLower;
    }

    public T getUpper() {
        return m_fUpper;
    }

    public void setUpper(T fUpper) {
        m_fUpper = fUpper;
    }

    public T getValue(int iParam) {
        return values[iParam];
    }

    public T[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    public void setBounds(T fLower, T fUpper) {
        m_fLower = fLower;
        m_fUpper = fUpper;
    }

    public void setValue(T fValue) throws Exception {
        if (isStochastic()) {
            values[0] = fValue;
            m_bIsDirty[0] = true;
            setDirty(true);
        } else throw new Exception("Can't set the value of a fixed parameter.");
    }

    public void setValue(int iParam, T fValue) throws Exception {
        if (isStochastic()) {
            values[iParam] = fValue;
            m_bIsDirty[iParam] = true;
            setDirty(true);
        } else throw new Exception("Can't set the value of a fixed parameter.");
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

    /**
     * Loggable interface implementation follows (partly) *
     */

    //public void init(State state, PrintStream out) throws Exception {
    public void init(PrintStream out) throws Exception {
        int nValues = getDimension();
        if (nValues == 1) {
            out.print(getID() + "\t");
        } else {
            for (int iValue = 0; iValue < nValues; iValue++) {
                out.print(getID() + iValue + "\t");
            }
        }
    }

    public void close(PrintStream out) {
        // nothing to do
    }


} // class Parameter
