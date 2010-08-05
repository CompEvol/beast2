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
import beast.core.Input;
import beast.core.StateNode;

import java.io.PrintStream;

/**
 * @author Joseph Heled
 *
 */
@Description("A Boolean-valued parameter represents a value (or array of values if the dimension is larger than one) " +
        "in the state space that can be changed by operators.")
public class BooleanParameter extends Parameter<java.lang.Boolean> {

    public Input<Boolean> m_pValues = new Input<Boolean>("value", "start value for this parameter");

    public BooleanParameter() {}

    /**
     * Constructor for testing.
     *
     * @param value
     * @param dimension
     * @throws Exception
     */
    public BooleanParameter(Integer value, Integer dimension) throws Exception {

        m_pValues.setValue(value, this);
        m_nDimension.setValue(dimension, this);
        initAndValidate();
    }


//    public Boolean getValue() {
//        return values[0];
//    }

    @Override
    public void initAndValidate() throws Exception {

        values = new Boolean[m_nDimension.get()];
        for (int i = 0; i < values.length; i++) {
            values[i] = m_pValues.get();
        }
        super.initAndValidate();
    }

    /**
     * deep copy *
     */
    @Override
    public Parameter<?> copy() {
        Parameter<Boolean> copy = new BooleanParameter();
        copy.setID(getID());
        copy.index = index;
        copy.values = new Boolean[values.length];
        System.arraycopy(values, 0, copy.values, 0, values.length);
        copy.m_bIsDirty = new boolean[values.length];
        return copy;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void assignTo(StateNode other) {
        Parameter<Boolean> copy = (Parameter<Boolean>) other;
        copy.setID(getID());
        copy.index = index;
        copy.values = new Boolean[values.length];
        System.arraycopy(values, 0, copy.values, 0, values.length);
        copy.m_fLower = m_fLower;
        copy.m_fUpper = m_fUpper;
        copy.m_bIsDirty = new boolean[values.length];
    }

    public void log(int nSample, PrintStream out) {
        BooleanParameter var = (BooleanParameter) getCurrent();//state.getStateNode(m_sID);
        int nValues = var.getDimension();
        for (int iValue = 0; iValue < nValues; iValue++) {
            out.print(var.getValue(iValue) + "\t");
        }
    }
}
