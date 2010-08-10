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

import java.io.PrintStream;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

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
     */
    public BooleanParameter(Integer value, Integer dimension) throws Exception {
    	init(value, dimension);
    }

    @Override
    public void initAndValidate() throws Exception {
        values = new Boolean[m_nDimension.get()];
        for (int i = 0; i < values.length; i++) {
            values[i] = m_pValues.get();
        }
        super.initAndValidate();
    }


    /** we need this here, because the base implementation (public T getValue()) fails
     * for some reason
     */
    @Override
    public Boolean getValue() {
        return values[0];
    }

    // RRB: if you remove next line, please document properly!
    @Override
    public void log(int nSample, PrintStream out) {
        BooleanParameter var = (BooleanParameter) getCurrent();
        int nValues = var.getDimension();
        for (int iValue = 0; iValue < nValues; iValue++) {
            out.print(var.getValue(iValue) + "\t");
        }
    }

	@Override
	public int scale(double fScale) {
		// nothing to do
		return 0;
	}

	@Override
    public void fromXML(Node node) {
    	NamedNodeMap atts = node.getAttributes();
    	setID(atts.getNamedItem("id").getNodeValue());
//    	setLower(Boolean.parseBoolean(atts.getNamedItem("lower").getNodeValue()));
//    	setUpper(Boolean.parseBoolean(atts.getNamedItem("upper").getNodeValue()));
    	int nDimension = Integer.parseInt(atts.getNamedItem("dimension").getNodeValue());
    	values = new Boolean[nDimension];
    	String sValue = node.getTextContent();
    	String [] sValues = sValue.split(",");
    	for (int i = 0; i < sValues.length; i++) {
    		values[i] = Boolean.parseBoolean(sValues[i]);
    	}
    }
}
