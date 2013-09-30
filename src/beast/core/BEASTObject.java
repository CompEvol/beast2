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
abstract public class BEASTObject {
    /**
     * set of YObjects that have this YObject in one of its Inputs *
     */
    public Set<BEASTObject> outputs = new HashSet<BEASTObject>();

    /* default constructor */
    public BEASTObject() {
    }

//	protected void setInputTypes() {
//		try {
//			for (Input<?> input : listInputs()) {
//				if (input != null) {
//					input.determineClass(this);
//				}
//			}
//		} catch (Exception e) {
//			// ignore
//			e.printStackTrace();
//		}
//	}

    /* Utility for testing purposes only.
      * This cannot be done in a constructor, since the
      * inputs will not exist yet at that point in time
      * and listInputs returns a list of nulls!
      * Assigns objects to inputs in order in which the
      * inputs are declared in the class, then calls
      * initAndValidate().
      */
    public void init(Object... objects) throws Exception {
        List<Input<?>> inputs = listInputs();
        int i = 0;
        for (Object object : objects) {
            inputs.get(i++).setValue(object, this);
        }
        initAndValidate();
    } // init

    /* Utility for testing purposes
      * The arguments are alternating input names and values,
      * and values are assigned to the input with the particular name.
      * For example initByName("kappa", 2.0, "lambda", true)
      * assigns 2 to input kappa and true to input lambda.
      * After assigning inputs, initAndValidate() is called.
      */
    public void initByName(Object... objects) throws Exception {
        if (objects.length % 2 == 1) {
            throw new Exception("Expected even number of arguments, name-value pairs");
        }
        for (int i = 0; i < objects.length; i += 2) {
            if (objects[i] instanceof String) {
                String sName = (String) objects[i];
                setInputValue(sName, objects[i + 1]);
            } else {
                throw new Exception("Expected a String in " + i + "th argument ");
            }
        }
        initAndValidate();
    } // initByName

    // identifiable
    protected String ID;

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }


    /**
     * @return description from @Description annotation
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
     * @return citation from @Citation annotation *
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
     * @return references for this plug in and all its inputs *
     */
    public final String getCitations() {
        return getCitations(new HashSet<String>(), new HashSet<String>());
    }

    private String getCitations(HashSet<String> citations, HashSet<String> IDs) {
    	if (getID() != null) {
    		if (IDs.contains(getID())) {
    			return "";
    		}
    		IDs.add(getID());
    	}
        StringBuffer buf = new StringBuffer();
        if (getCitation() != null) {
           // only add citation if it is not already processed
           if (!citations.contains(getCitation().value())) {
                // and there is actually a citation to add
                buf.append(getCitation().value());
                buf.append("\n\n");
                citations.add(getCitation().value());
            }
            //return buf.toString();
        }
        try {
            for (BEASTObject plugin : listActivePlugins()) {
                buf.append(plugin.getCitations(citations, IDs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buf.toString();
    } // getCitations


    /**
     * create list of inputs to this plug-in *
     */
    public List<Input<?>> listInputs() throws IllegalArgumentException, IllegalAccessException {
        List<Input<?>> inputs = new ArrayList<Input<?>>();
        Field[] fields = getClass().getFields();
        for (Field field : fields) {
            if (field.getType().isAssignableFrom(Input.class)) {
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
     *
     * @return list of all active plug-ins
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public List<BEASTObject> listActivePlugins() throws IllegalArgumentException, IllegalAccessException {
        List<BEASTObject> plugins = new ArrayList<BEASTObject>();
        Field[] fields = getClass().getFields();
        for (Field field : fields) {
            if (field.getType().isAssignableFrom(Input.class)) {
                Input<?> input = (Input<?>) field.get(this);
                if (input.get() != null) {
                    if (input.get() instanceof List<?>) {
                        List<?> vector = (List<?>) input.get();
                        for (Object o : vector) {
                            if (o instanceof BEASTObject) {
                                plugins.add((BEASTObject) o);
                            }
                        }
                    } else if (input.get() != null && input.get() instanceof BEASTObject) {
                        plugins.add((BEASTObject) input.get());
                    }
                }
            }
        }
        return plugins;
    } // listActivePlugins

    /**
     * get description of an input
     *
     * @param name of the input
     * @return list of inputs
     */
    public String getTipText(String name) throws IllegalArgumentException, IllegalAccessException {
        Field[] fields = getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().isAssignableFrom(Input.class)) {
                Input<?> input = (Input<?>) field.get(this);
                if (input.getName().equals(name)) {
                    return input.getTipText();
                }
            }
        }
        return null;
    } // getTipText


    /**
     * check whether the input is an Integer, Double, Boolean or String *
     */
    public boolean isPrimitive(String name) throws Exception {
        Input<?> input = getInput(name);
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

    /**
     * get value of an input by input name *
     */
    public Object getInputValue(String name) throws Exception {
        Input<?> input = getInput(name);
        return input.get();
    } // getInputValue

    /**
     * set value of an input by input name *
     */
    public void setInputValue(String name, Object value) throws Exception {
        Input<?> input = getInput(name);
        if (!input.canSetValue(value, this)) {
            throw new Exception("Cannot set input value of " + name);
        }
        input.setValue(value, this);
    } // setInputValue

    /**
     * get input by input name *
     */
    public Input<?> getInput(String name) throws Exception {
        Field[] fields = getClass().getFields();
        for (Field field : fields) {
            if (field.getType().isAssignableFrom(Input.class)) {
                Input<?> input = (Input<?>) field.get(this);
                if (input.getName().equals(name)) {
                    return input;
                }
            }
        }


        String inputNames = " "; // <- space here to prevent error in .substring below
        for (Input<?> input : listInputs()) {
            inputNames += input.getName() + ",";
        }
        throw new Exception("This BEASTObject (" + (this.getID() == null ? this.getClass().getName() : this.getID()) + ") has no input with name " + name + ". " +
                "Choose one of these inputs:" + inputNames.substring(0, inputNames.length() - 1));
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
     *
     * @throws Exception when validation fails
     */
    public void validateInputs() throws Exception {
        for (Input<?> input : listInputs()) {
            input.validate();
        }
    }

    /**
     * Collect all predecessors in the graph where inputs
     * represent incoming edges and plug-ins nodes.
     *  
     * @param predecessors in partial order such that if
     * x is after y in the list then x is not an ancestor of y
     * (but x need not necessarily be a predecesor of y)
     */

    public void getPredecessors(List<BEASTObject> predecessors) {
		predecessors.add(this);
		try {
			for (BEASTObject plugin2 : listActivePlugins()) {
				if (!predecessors.contains(plugin2)) {
					plugin2.getPredecessors(predecessors);
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public String toString() {
        return getID();
    } // toString

	
	
} // class BEASTObject
