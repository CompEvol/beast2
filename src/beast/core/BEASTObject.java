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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    public Set<BEASTInterface> outputs = new HashSet<>();
	
	/** 
	 * cache collecting all Inputs and InputForAnnotatedConstrutors 
	 * indexed through input name
	 */
    private Map<String,Input<?>> inputcache;
	
    /**
     * @return set of Objects that have this Object in one of its Inputs
     */
	@Override
	public Set<BEASTInterface> getOutputs() {
		return outputs;
	};

	@Override
	public Map<String, Input<?>> getInputs() {
		if (inputcache == null) {
			inputcache = new HashMap<>();
			try {
				for (Input<?> input : listInputs()) {
					inputcache.put(input.getName(), input);
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
				throw new RuntimeException("Problem getting inputs " + e.getClass().getName() + e.getMessage());
			}
		}
		return inputcache;
	};

	// identifiable
    protected String ID;

	@Override
    public String getID() {
        return ID;
    }

	@Override
    public void setID(final String ID) {
        this.ID = ID;
    }

    // A default method in BEASTInterface cannot override
    // a method in Object, so it needs to be in BEASTObject
	@Override
    public String toString() {
    	return getID();
    }
    
    
    
} // class BEASTObject
