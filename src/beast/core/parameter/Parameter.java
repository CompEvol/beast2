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
import beast.core.Input.Validate;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


@Description("A parameter represents a value in the state space that can be changed " +
        "by operators.")
public abstract class Parameter<T> extends StateNode {
    /**
     * value is a required input since it is very hard to ensure any internal consistency
     * when no value is specified. When	another class wants to set the dimension, say,
     * this will make it the responsibility of the other class to maintain internal consistency of
     * the parameter.
     */
    public Input<String> m_pValues = new Input<String>("value", "start value(s) for this parameter. If multiple values are specified, they should be separated by whitespace.", Validate.REQUIRED);
    public Input<java.lang.Integer> m_nDimension =
            new Input<java.lang.Integer>("dimension", "dimension of the parameter (default 1)", 1);
    public Input<Integer> minorDimensionInput = new Input<Integer>("minordimension", "minro-dimension when the parameter is interpreted as a matrix (default 1)", 1);


    /**
     * constructors *
     */
    public Parameter() {
    }

    public Parameter(T[] values) {
        this.values = values.clone();
        this.storedValues = values.clone();
        m_fUpper = getMax();
        m_fLower = getMin();
        m_bIsDirty = new boolean[values.length];
    }

    @Override
    public void initAndValidate() throws Exception {
        m_bIsDirty = new boolean[m_nDimension.get()];

        minorDimension = minorDimensionInput.get();
        if (minorDimension > 0 && m_nDimension.get() % minorDimension > 0) {
            throw new Exception("Dimension must be divisble by stride");
        }
    }


    /**
     * upper & lower bound *
     */
    protected T m_fUpper;
    protected T m_fLower;

    abstract T getMax();

    abstract T getMin();

    /**
     * the actual values of this parameter
     */
    protected T[] values;
    protected T[] storedValues;

    /**
     * sub-dimension when parameter is considered a matrix
     */
    protected int minorDimension = 1;

    /**
     * isDirty flags for individual elements in high dimensional parameters
     */
    protected boolean[] m_bIsDirty;
    /**
     * last element to be changed *
     */
    protected int m_nLastDirty;

    /**
     * @param iParam dimention to check
     * @return true if the iParam-th element has changed
     */
    public boolean isDirty(int iParam) {
        return m_bIsDirty[iParam];
    }

    /**
     * Returns index of entry that was changed last. Useful if it is known only a
     * single  value has changed in the array. *
     */
    public int getLastDirty() {
        return m_nLastDirty;
    }

    @Override
    public void setEverythingDirty(final boolean isDirty) {
        setSomethingIsDirty(isDirty);
        Arrays.fill(m_bIsDirty, isDirty);
    }

    /*
     * various setters & getters *
     */
    public int getDimension() {
        return values.length;
    }

    /**
     * Change the dimension of a parameter
     * <p/>
     * This should only be called from initAndValidate() when a parent
     * plugin can easily calculate the dimension of a parameter, but it
     * is awkward to do this by hand.
     * <p/>
     * Values are sourced from the original parameter values.
     */
    @SuppressWarnings("unchecked")
    public void setDimension(int nDimension) {
        if (getDimension() != nDimension) {
            T[] values2 = (T[]) Array.newInstance(m_fUpper.getClass(), nDimension);
            for (int i = 0; i < nDimension; i++) {
                values2[i] = values[i % getDimension()];
            }
            values = (T[]) values2;
            storedValues = (T[]) Array.newInstance(m_fUpper.getClass(), nDimension);
        }
        m_bIsDirty = new boolean[nDimension];
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

    public void setValue(T fValue) {
        startEditing(null);
//        if (isStochastic()) {
        values[0] = fValue;
        m_bIsDirty[0] = true;
        m_nLastDirty = 0;
//    	}
//        } else {
//        	System.err.println("Can't set the value of a parameter, unless startEditing() is called first.");
//        	System.exit(1);
//        }
    }

    public void setValue(int iParam, T fValue) {
        startEditing(null);
//        if (isStochastic()) {
        values[iParam] = fValue;
        m_bIsDirty[iParam] = true;
        m_nLastDirty = iParam;
//        } else {
//        	System.err.println("Can't set the value of a parameter, unless startEditing() is called first.");
//	        System.exit(1);
//	    }
    }

    public void swap(int iLeft, int iRight) {
        startEditing(null);
        T tmp = values[iLeft];
        values[iLeft] = values[iRight];
        values[iRight] = tmp;
    }

    /*public void setValueQuietly(int dim, T value){
        values[dim] = value;
        m_bIsDirty[dim] = true;
        m_nLastDirty = dim;
    }*/

    /**
     * Note that changing toString means fromXML needs to be changed as well,
     * since it parses the output of toString back into a parameter.
     */
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append(m_sID).append("[").append(values.length);
        if (minorDimension > 0) {
            buf.append(" ").append(minorDimension);
        }
        buf.append("] ");
        buf.append("(").append(m_fLower).append(",").append(m_fUpper).append("): ");
        for (T value : values) {
            buf.append(value).append(" ");
        }
        return buf.toString();
    }


    @Override
    public Parameter<T> copy() {
        try {
            @SuppressWarnings("unchecked")
            Parameter<T> copy = (Parameter<T>) this.clone();
            copy.values = values.clone();//new Boolean[values.length];
            copy.m_bIsDirty = new boolean[values.length];
            return copy;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void assignTo(StateNode other) {
        @SuppressWarnings("unchecked")
        Parameter<T> copy = (Parameter<T>) other;
        copy.setID(getID());
        copy.index = index;
        copy.values = values.clone();
        //System.arraycopy(values, 0, copy.values, 0, values.length);
        copy.m_fLower = m_fLower;
        copy.m_fUpper = m_fUpper;
        copy.m_bIsDirty = new boolean[values.length];
    }

    @Override
    public void assignFrom(StateNode other) {
        @SuppressWarnings("unchecked")
        Parameter<T> source = (Parameter<T>) other;
        setID(source.getID());
        values = source.values.clone();
        storedValues = source.storedValues.clone();
        System.arraycopy(source.values, 0, values, 0, values.length);
        m_fLower = source.m_fLower;
        m_fUpper = source.m_fUpper;
        m_bIsDirty = new boolean[source.values.length];
    }

    @Override
    public void assignFromFragile(StateNode other) {
        @SuppressWarnings("unchecked")
        Parameter<T> source = (Parameter<T>) other;
        System.arraycopy(source.values, 0, values, 0, values.length);
        Arrays.fill(m_bIsDirty, false);
    }


    /**
     * Loggable interface implementation follows (partly, the actual
     * logging of values happens in derived classes) *
     */
    @Override
    public void init(PrintStream out) throws Exception {
        final int nValues = getDimension();
        if (nValues == 1) {
            out.print(getID() + "\t");
        } else {
            for (int iValue = 0; iValue < nValues; iValue++) {
                out.print(getID() + (iValue + 1) + "\t");
            }
        }
    }

    @Override
    public void close(PrintStream out) {
        // nothing to do
    }

    /**
     * StateNode implementation *
     */
    @Override
    public void fromXML(Node node) {
        NamedNodeMap atts = node.getAttributes();
        setID(atts.getNamedItem("id").getNodeValue());
        String sStr = node.getTextContent();
        Pattern pattern = Pattern.compile(".*\\[(.*) (.*)\\].*\\((.*),(.*)\\): (.*) ");
        Matcher matcher = pattern.matcher(sStr);
        if (matcher.matches()) {
            String sDimension = matcher.group(1);
            String sStride = matcher.group(2);
            String sLower = matcher.group(3);
            String sUpper = matcher.group(4);
            String sValuesAsString = matcher.group(5);
            String[] sValues = sValuesAsString.split(" ");
            minorDimension = Integer.parseInt(sStride);
            fromXML(Integer.parseInt(sDimension), sLower, sUpper, sValues);
        } else {
            pattern = Pattern.compile(".*\\[(.*)\\].*\\((.*),(.*)\\): (.*) ");
            matcher = pattern.matcher(sStr);
            if (matcher.matches()) {
                String sDimension = matcher.group(1);
                String sLower = matcher.group(2);
                String sUpper = matcher.group(3);
                String sValuesAsString = matcher.group(4);
                String[] sValues = sValuesAsString.split(" ");
                minorDimension = 0;
                fromXML(Integer.parseInt(sDimension), sLower, sUpper, sValues);
            } else {
                throw new RuntimeException("parameter could not be parsed");
            }
        }
    }

    /**
     * Restore a saved parameter from string representation.
     * This cannot be a template method since it requires
     * creation of an array of T...
     *
     * @param nDimension parameter dimention
     * @param sLower     lower bound
     * @param sUpper     upper bound
     * @param sValues    values
     */
    abstract void fromXML(int nDimension, String sLower, String sUpper, String[] sValues);

    /**
     * matrix implementation *
     */
    public int getMinorDimension1() {
        return minorDimension;
    }

    public int getMinorDimension2() {
        return getDimension() / minorDimension;
    }

    public T getMatrixValue(int i, int j) {
        return values[i * minorDimension + j];
    }

    public void setMatrixValue(int i, int j, T value) {
        setValue(i * minorDimension + j, value);
    }

    public void getMatrixValues1(int i, T[] row) {
        assert (row.length == minorDimension);
        System.arraycopy(values, i * minorDimension, row, 0, minorDimension);
    }

    public void getMatrixValues1(int i, double[] row) {
        assert (row.length == minorDimension);
        for (int j = 0; j < minorDimension; j++) {
            row[j] = getArrayValue(i * minorDimension + j);
        }
    }

    public void getMatrixValues2(int j, T[] col) {
        assert (col.length == getMinorDimension2());
        for (int i = 0; i < getMinorDimension2(); i++) {
            col[i] = values[i * minorDimension + j];
        }
    }

    public void getMatrixValues2(int j, double[] col) {
        assert (col.length == getMinorDimension2());
        for (int i = 0; i < getMinorDimension2(); i++) {
            col[i] = getArrayValue(i * minorDimension + j);
        }
    }

    @Override
    protected void store() {
        System.arraycopy(values, 0, storedValues, 0, values.length);
    }

    @Override
    public void restore() {
        T[] tmp = storedValues;
        storedValues = values;
        values = tmp;
        m_bHasStartedEditing = false;
    }
} // class Parameter
