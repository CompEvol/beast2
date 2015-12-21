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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beast.util.InputType;

public interface BEASTInterface {
    public void initAndValidate() throws Exception;

	/** identifiable **/
	public String getID();
	public void setID(String ID);
	/** return set of Outputs, that is Objects for which this object is an Input **/
	public Set getOutputs();

	
	
    /* Utility for testing purposes only.
     * This cannot be done in a constructor, since the
     * inputs will not exist yet at that point in time
     * and listInputs returns a list of nulls!
     * Assigns objects to inputs in order in which the
     * inputs are declared in the class, then calls
     * initAndValidate().
     */
   default public void init(final Object... objects) throws Exception {
       final List<Input<?>> inputs = listInputs();
       int i = 0;
       for (final Object object : objects) {
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
  default public void initByName(final Object... objects) throws Exception {
      if (objects.length % 2 == 1) {
          throw new RuntimeException("Expected even number of arguments, name-value pairs");
      }
      for (int i = 0; i < objects.length; i += 2) {
          if (objects[i] instanceof String) {
              final String sName = (String) objects[i];
              setInputValue(sName, objects[i + 1]);
          } else {
              throw new RuntimeException("Expected a String in " + i + "th argument ");
          }
      }
      try {
          initAndValidate();
      } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("initAndValidate() failed! " + e.getMessage());
      }
  } // initByName

	
	//@SuppressWarnings("rawtypes") 
	static Set getOutputs(Object object) {
    	try {
            Method method = object.getClass().getMethod("getOutputs");
            Object outputs = method.invoke(object);
            return (Set) outputs;
    	} catch (Exception e) {
    		throw new RuntimeException("could not call getID() on object: " + e.getMessage());
    	}
	}

    /**
     * @return description from @Description annotation
     */
	default public String getDescription() {
        final Annotation[] classAnnotations = this.getClass().getAnnotations();
        for (final Annotation annotation : classAnnotations) {
            if (annotation instanceof Description) {
                final Description description = (Description) annotation;
                return description.value();
            }
        }
        return "Not documented!!!";
    }

    /**
     * @return citation from @Citation annotation *
     */
    default public Citation getCitation() {
        final Annotation[] classAnnotations = this.getClass().getAnnotations();
        for (final Annotation annotation : classAnnotations) {
            if (annotation instanceof Citation) {
                return (Citation) annotation;
            }
        }
        return null;
    }

    /**
     * @return references for this plug in and all its inputs *
     */
    default public String getCitations() {
        return getCitations(new HashSet<>(), new HashSet<>());
    }

    default String getCitations(final HashSet<String> citations, final HashSet<String> IDs) {
        if (getID() != null) {
            if (IDs.contains(getID())) {
                return "";
            }
            IDs.add(getID());
        }
        final StringBuilder buf = new StringBuilder();
        if (getCitation() != null) {
            // only add citation if it is not already processed
            if (!citations.contains(getCitation().value())) {
                // and there is actually a citation to add
                buf.append("\n");
                buf.append(getCitation().value());
                buf.append("\n");
                citations.add(getCitation().value());
            }
            //return buf.toString();
        }
        try {
            for (final BEASTInterface plugin : listActivePlugins()) {
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
    default public List<Input<?>> listInputs() throws IllegalArgumentException, IllegalAccessException {
        final List<Input<?>> inputs = new ArrayList<>();
        
        // First, collect all Inputs
        final Field[] fields = getClass().getFields();
        for (final Field field : fields) {
            if (field.getType().isAssignableFrom(Input.class)) {
                final Input<?> input = (Input<?>) field.get(this);
                inputs.add(input);
            }
        }
        
        // Second, collect InputForAnnotatedConstructors of annotated constructor (if any)
	    Constructor<?>[] allConstructors = this.getClass().getDeclaredConstructors();
	    for (Constructor<?> ctor : allConstructors) {
	    	Annotation[][] annotations = ctor.getParameterAnnotations();
	    	List<Param> paramAnnotations = new ArrayList<>();
	    	for (Annotation [] a0 : annotations) {
		    	for (Annotation a : a0) {
		    		if (a instanceof Param) {
		    			paramAnnotations.add((Param) a);
		    		}
		    	}
	    	}
	    	Class<?>[] types  = ctor.getParameterTypes();	    	
    		Type[] gtypes = ctor.getGenericParameterTypes();
	    	if (types.length > 0 && paramAnnotations.size() > 0) {
	    		int offset = 0;
	    		if (types.length == paramAnnotations.size() + 1) {
	    			offset = 1;
	    		}
	    		for (int i = 0; i < paramAnnotations.size(); i++) {
	    			Param param = paramAnnotations.get(i);
	    			Type type = types[i + offset];
	    			Class<?> clazz = null;
					try {
						clazz = Class.forName(type.getTypeName());
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    			if (clazz.isAssignableFrom(List.class)) {
                        Type[] genericTypes2 = ((ParameterizedType) gtypes[i + offset]).getActualTypeArguments();
                        Class<?> theClass = (Class<?>) genericTypes2[0];
	    				InputForAnnotatedConstructor<?> t = null;
						try {
							t = new InputForAnnotatedConstructor<>(this, theClass, param);
						} catch (NoSuchMethodException | SecurityException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	    				inputs.add(t);
	    			} else {
	    				InputForAnnotatedConstructor<?> t = null;
						try {
							t = new InputForAnnotatedConstructor<>(this, types[i + offset], param);
						} catch (NoSuchMethodException | SecurityException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	    				inputs.add(t);
	    			}
	    		}
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
    default public List<BEASTInterface> listActivePlugins() throws IllegalArgumentException, IllegalAccessException {
        final List<BEASTInterface> plugins = new ArrayList<>();
        final Field[] fields = getClass().getFields();
        for (final Field field : fields) {
            if (field.getType().isAssignableFrom(Input.class)) {
                final Input<?> input = (Input<?>) field.get(this);
                if (input.get() != null) {
                    if (input.get() instanceof List<?>) {
                        final List<?> vector = (List<?>) input.get();
                        for (final Object o : vector) {
                            if (o instanceof BEASTInterface) {
                                plugins.add((BEASTInterface) o);
                            }
                        }
                    } else if (input.get() != null && input.get() instanceof BEASTInterface) {
                        plugins.add((BEASTInterface) input.get());
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
    default public String getTipText(final String name) throws IllegalArgumentException, IllegalAccessException {
        final Field[] fields = getClass().getDeclaredFields();
        for (final Field field : fields) {
            if (field.getType().isAssignableFrom(Input.class)) {
                final Input<?> input = (Input<?>) field.get(this);
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
    default public boolean isPrimitive(final String name) throws Exception {
        final Input<?> input = getInput(name);
        final Class<?> inputType = input.getType();

        if (inputType == null) {
            input.determineClass(this);
        }

        assert inputType != null;
        for (final Class c : new Class[]{Integer.class, Double.class, Boolean.class, String.class}) {
            if (inputType.isAssignableFrom(c)) {
                return true;
            }
        }
//        if (inputType.isAssignableFrom(Integer.class)) {
//            return true;
//        }
//        if (inputType.isAssignableFrom(Double.class)) {
//            return true;
//        }
//        if (inputType.isAssignableFrom(Boolean.class)) {
//            return true;
//        }
//        if (inputType.isAssignableFrom(String.class)) {
//            return true;
//        }
        return false;
    } // isPrimitive

    /**
     * get value of an input by input name *
     */
    default public Object getInputValue(final String name) throws Exception {
        final Input<?> input = getInput(name);
        return input.get();
    } // getInputValue

    /**
     * set value of an input by input name *
     */
    default public void setInputValue(final String name, final Object value) throws Exception {
        final Input<?> input = getInput(name);
        if (!input.canSetValue(value, this)) {
            throw new RuntimeException("Cannot set input value of " + name);
        }
        input.setValue(value, this);
    } // setInputValue

    /**
     * get input by input name *
     */
    default public Input<?> getInput(final String name) throws Exception {
        final Field[] fields = getClass().getFields();
        for (final Field field : fields) {
            if (field.getType().isAssignableFrom(Input.class)) {
                final Input<?> input = (Input<?>) field.get(this);
                if (input.getName().equals(name)) {
                    return input;
                }
            }
        }


        String inputNames = " "; // <- space here to prevent error in .substring below
        for (final Input<?> input : listInputs()) {
            inputNames += input.getName() + ",";
        }
        throw new Exception("This BEASTInterface (" + (this.getID() == null ? this.getClass().getName() : this.getID()) + ") has no input with name " + name + ". " +
                "Choose one of these inputs:" + inputNames.substring(0, inputNames.length() - 1));
    } // getInput


    /**
     * check validation rules for all its inputs *
     *
     * @throws Exception when validation fails
     */
    default public void validateInputs() throws Exception {
        for (final Input<?> input : listInputs()) {
            input.validate();
        }
    }

    /**
     * Collect all predecessors in the graph where inputs
     * represent incoming edges and plug-ins nodes.
     *
     * @param predecessors in partial order such that if
     *                     x is after y in the list then x is not an ancestor of y
     *                     (but x need not necessarily be a predecesor of y)
     */

    default public void getPredecessors(final List<BEASTInterface> predecessors) {
        predecessors.add(this);
        try {
            for (final BEASTInterface plugin2 : listActivePlugins()) {
                if (!predecessors.contains(plugin2)) {
                    plugin2.getPredecessors(predecessors);
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


// // This class was formerly called 'Plugin'
//    @Description(
//            value = "Base class for all BEAST objects, which is pretty much every class " +
//                    "you want to incorporate in a model.",
//            isInheritable = false
//    )
//    abstract public class Core implements BEASTInterface {
//        /**
//         * set of Objects that have this Object in one of its Inputs *
//         * @deprecate use getOuputs() or BEASTInterface.getOuputs(object) instead
//         */
//    	@Deprecated
//        public Set<BEASTInterface> outputs = new HashSet<>();
//    	
//        /**
//         * @return set of Objects that have this Object in one of its Inputs
//         */
//    	@SuppressWarnings("rawtypes")
//    	public Set getOutputs() {
//    		return outputs;
//    	};
//
//        // identifiable
//        protected String ID;
//
//        public String getID() {
//            return ID;
//        }
//
//        public void setID(final String ID) {
//            this.ID = ID;
//        }
//    }
}


