/*
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


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// This class was formerly called 'Plugin'
@Description(
        value = "Base class for all BEAST objects, which is pretty much every class " +
                "you want to incorporate in a model.",
        isInheritable = false
)
abstract public class BEASTObject implements BEASTInterface{
    /**
     * set of Objects that have this Object in one of its Inputs *
     * @deprecate use getOuputs() or BEASTObject.getOuputs(object) instead
     */
	@Deprecated
    public Set<BEASTObject> outputs = new HashSet<BEASTObject>();
	
    /**
     * @return set of Objects that have this Object in one of its Inputs
     */
	@SuppressWarnings("rawtypes")
	public Set getOutputs() {
		return outputs;
	};

    // identifiable
    protected String ID;

    public String getID() {
        return ID;
    }

    public void setID(final String ID) {
        this.ID = ID;
    }

    // A default method in BEASTInterface cannot override
    // a method in Object, so it needs to be in BEASTObject
    public String getString() {
    	return getID();
    }
} // class BEASTObject
