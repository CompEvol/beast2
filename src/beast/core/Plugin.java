/*
* File Plugin.java
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Description(
        value = "Base class for all plug-ins, which is pretty much every class " +
                "you want to incorporate in a model.",
        isInheritable = false
)
abstract public class Plugin {
	
	/* default constructor */
	public Plugin() {}
	
	/* Utility for testing purposes only.
	 * This cannot be done in a constructor, since the 
	 * inputs will not exist yet at that point in time
	 * and listInputs returns a list of nulls!
	 * Assigns objects to inputs in order in which the
	 * inputs are declared in the class, then calls
	 * initAndValidate().
	 */
	public void init(Object...objects) throws Exception {
		List<Input<?>> inputs = listInputs();
		int i = 0;
		for(Object object : objects) {
			inputs.get(i++).setValue(object, this);
		}
		initAndValidate();
	} // c'tor
	
	
    // identifiable
    protected String m_sID;

    public String getID() {
        return m_sID;
    }

    public void setID(String sID) {
        m_sID = sID;
    }


    /**
     * @return  description from @Description annotation
     */
    public String getDescription() {
        Annotation[] classAnnotations = this.getClass().getAnnotations();
        for (Annotation annotation : classAnnotations) {
            if (annotation instanceof Description) {
                Description description = (Description) annotation;
                return description.value();
            }
        }
        return "Not documented!!!";
    }

    /**
     * @return  citation from @Citation annotation *
     */
    public final Citation getCitation() {
        Annotation[] classAnnotations = this.getClass().getAnnotations();
        for (Annotation annotation : classAnnotations) {
            if (annotation instanceof Citation) {
                return (Citation) annotation;
            }
        }
        return null;
    }

    /**
     * @return  references for this plug in and all its inputs *
     */
    public final String getCitations() {
        return getCitations(new HashSet<String>());
    }

    private String getCitations(HashSet<String> bDone) {
        StringBuffer buf = new StringBuffer();
        if (!bDone.contains(getID())) {
            // only add citation if it is not already processed
            if (getCitation() != null) {
                // and there is actually a citation to add
                buf.append(getCitation().value());
                buf.append("\n\n");
            }
            bDone.add(getID());
        }
        try {
            for (Plugin plugin : listActivePlugins()) {
                buf.append(plugin.getCitations(bDone));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buf.toString();
    } // getCitations

    public List<Input<?>> listInputs() throws IllegalArgumentException, IllegalAccessException {
        List<Input<?>> inputs = new ArrayList<Input<?>>();
        Field[] fields = getClass().getFields();
        for(Field field : fields) {
            if( field.getType().isAssignableFrom(Input.class) ) {
                Input<?> input = (Input<?>) field.get(this);
                inputs.add(input);
            }
        }
        return inputs;
    } // listInputs

    /**
     * create array of all plug-ins in the inputs that are instantiated.
     * If the input is a List of plug-ins, these individual plug-ins are
     * added to the list.
     * @return list of all active plugins
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public List<Plugin> listActivePlugins() throws IllegalArgumentException, IllegalAccessException {
        List<Plugin> sPlugins = new ArrayList<Plugin>();
        Field[] fields = getClass().getFields();
        for(Field field : fields) {
            if( field.getType().isAssignableFrom(Input.class) ) {
                Input<?> input = (Input<?>) field.get(this);
                if( input.get() != null ) {
                    if( input.get() instanceof List<?> ) {
                        List<?> vector = (List<?>) input.get();
                        for(Object o : vector) {
                            if( o instanceof Plugin ) {
                                sPlugins.add((Plugin) o);
                            }
                        }
                    } else if( input.get() != null && input.get() instanceof Plugin ) {
                        sPlugins.add((Plugin) input.get());
                    }
                }
            }
        }
        return sPlugins;
    } // listActivePlugins

    public String getTipText(String sName) throws IllegalArgumentException, IllegalAccessException {
        Field[] fields = getClass().getDeclaredFields();
        for(Field field : fields) {
            if( field.getType().isAssignableFrom(Input.class) ) {
                Input<?> input = (Input<?>) field.get(this);
                if( input.getName().equals(sName) ) {
                    return input.getTipText();
                }
            }
        }
        return null;
    } // getTipText


    public boolean isPrimitive(String sName) throws Exception {
        Input<?> input = getInput(sName);
        if (input.getType() == null) {
            input.determineClass(this);
        }
        if (input.getType().isAssignableFrom(Integer.class)) {
            return true;
        }
        if (input.getType().isAssignableFrom(Double.class)) {
            return true;
        }
        if (input.getType().isAssignableFrom(Boolean.class)) {
            return true;
        }
        if (input.getType().isAssignableFrom(String.class)) {
            return true;
        }
        return false;
    } // isPrimitive

    public Object getInputValue(String sName) throws Exception {
        Input<?> input = getInput(sName);
        return input.get();
    } // getInputValue

    public void setInputValue(String sName, Object value) throws Exception {
        Input<?> input = getInput(sName);
        input.setValue(value, this);
    } // setInputValue

    public Input<?> getInput(String sName) throws Exception {
        Field[] fields = getClass().getFields();
        for(Field field : fields) {
            if( field.getType().isAssignableFrom(Input.class) ) {
                Input<?> input = (Input<?>) field.get(this);
                if( input.getName().equals(sName) ) {
                    return input;
                }
            }
        }
        throw new Exception("This plugin (" + this.getID() + ") has no input with name " + sName);
    } // getInput

    /**
     * @throws Exception when plugin does not implement this method
     */
    //abstract public void initAndValidate() throws Exception;
    public void initAndValidate() throws Exception {
    // TODO: AR - Why is this not an abstract method? Does Plugin need to be concrete?
    // RRB: can be abstract, but this breaks some of the DocMaker stuff.
    // It only produces pages for Plugins that are not abstract. 
    // This means the MCMC page does not point to Operator page any more since the latter does not exist.
    // As a result, there is no place that lists all Operators, which is a bit of a shame.
    // Perhaps DocMaker can be fixed to work around this, otherwise I see no issues making this abstract.
    
        throw new Exception("Plugin.initAndValidate(): Every plugin should implement this method to assure the class behaves, " +
                "even when inputs are not specified");
    }

    /**
     * check validation rules for all its inputs *
     * @throws Exception when validation fails
     */
    public void validateInputs() throws Exception {
        for (Input<?> input : listInputs()) {
            input.validate();
        }
    }


    public String toString() {
    	return getID();
    } // toString
    
} // class Plugin
