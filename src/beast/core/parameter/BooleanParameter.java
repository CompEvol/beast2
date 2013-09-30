/*
* File BooleanParameter.java
*
* Copyright (C) 2010 Joseph Heled jheled@gmail.com
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


import beast.core.Description;

import java.io.PrintStream;


/**
 * @author Joseph Heled
 */
@Description("A Boolean-valued parameter represents a value (or array of values if the dimension is larger than one) " +
        "in the state space that can be changed by operators.")
public class BooleanParameter extends Parameter.Base<java.lang.Boolean> {
    public BooleanParameter() {
        m_fUpper = true;
    }

    public BooleanParameter(Boolean[] fValues) {
        super(fValues);
        m_fUpper = true;
    }

    /**
     * Constructor used by Input.setValue(String) *
     */
    public BooleanParameter(String sValue) throws Exception {
        init(sValue, 1);
        m_fUpper = true;
    }


    @Override
    Boolean getMax() {
        return true;
    }

    @Override
    Boolean getMin() {
        return false;
    }

    /** Valuable implementation follows **/
    /**
     * we need this here, because the base implementation (public T getValue()) fails
     * for some reason
     */
    @Override
    public Boolean getValue() {
        return values[0];
    }

    @Override
    public double getArrayValue() {
        return (values[0] ? 1 : 0);
    }

    @Override
    public double getArrayValue(int iValue) {
        return (values[iValue] ? 1 : 0);
    }

    ;

    /**
     * Loggable implementation follows *
     */
    @Override
    public void log(int nSample, PrintStream out) {
        BooleanParameter var = (BooleanParameter) getCurrent();
        int nValues = var.getDimension();
        for (int iValue = 0; iValue < nValues; iValue++) {
            // Output 0/1 for tracer
            out.print((var.getValue(iValue) ? '1' : '0') + "\t");
        }
    }

    /**
     * StateNode methods *
     */
    @Override
    public int scale(double fScale) {
        // nothing to do
        System.err.println("Attempt to scale Boolean parameter " + getID() + "  has no effect");
        return 0;
    }

    @Override
    void fromXML(int nDimension, String sLower, String sUpper, String[] sValues) {
        values = new Boolean[nDimension];
        for (int i = 0; i < sValues.length; i++) {
            values[i] = Boolean.parseBoolean(sValues[i]);
        }
    }
}
