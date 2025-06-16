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
package beast.base.core;



import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import beast.pkgmgmt.BEASTClassLoader;

public interface BEASTInterface extends Citable {
	final static String DEFEAULT_DESCRIPTION = "Not documented!!!";
	
	/**
	 * initAndValidate is supposed to check validity of values of inputs, and initialise. 
	 * If for some reason this fails, the most appropriate exception to throw is 
	 * IllegalArgumentException (if the combination of input values is not correct)
	 * or otherwise a RuntimeException.
	 */	
    public void initAndValidate();

	/** identifiable **/
	public String getID();
	public void setID(String ID);
	
	/** return set of Outputs, that is Objects for which this object is an Input **/
	public Set<BEASTInterface> getOutputs();

	/** return Map of Inputs containing both Inputs and InptForAnnotatedConstructors 
	 * indexed by Input name **/
	public Map<String, Input<?>> getInputs();
	
	
    /* Utility for testing purposes only.
     * This cannot be done in a constructor, since the
     * inputs will not exist yet at that point in time
     * and listInputs returns a list of nulls!
     * Assigns objects to inputs in order in which the
     * inputs are declared in the class, then calls
     * initAndValidate().
     */
   default public void init(final Object... objects) {
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
  default public void initByName(final Object... objects) {
      if (objects.length % 2 == 1) {
          throw new RuntimeException("Expected even number of arguments, name-value pairs");
      }
      for (int i = 0; i < objects.length; i += 2) {
          if (objects[i] instanceof String) {
              final String name = (String) objects[i];
              setInputValue(name, objects[i + 1]);
          } else {
              throw new RuntimeException("Expected a String in " + i + "th argument ");
          }
      }
      try {
          validateInputs();
      } catch (IllegalArgumentException ex) {
          ex.printStackTrace();
          throw new RuntimeException("validateInputs() failed! " + ex.getMessage());
      }
      try {
          initAndValidate();
      } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("initAndValidate() failed! " + e.getMessage());
      }
  } // initByName

	
	@SuppressWarnings({"unchecked", "rawtypes" }) 
	static Set<BEASTInterface> getOutputs(Object object) {
    	try {
            Method method = object.getClass().getMethod("getOutputs");
            Object outputs = method.invoke(object);
            if (outputs instanceof Set<?>) {
            	return (Set) outputs;
            }
    		throw new RuntimeException("call to getOutputs() on object did not return a java.util.Set");
    	} catch (Exception e) {
    		throw new RuntimeException("could not call getOutputs() on object: " + e.getMessage());
    	}
	}

    /**
     * @return description from @Description annotation
     */
	default String getDescription() {
        final Annotation[] classAnnotations = this.getClass().getAnnotations();
        for (final Annotation annotation : classAnnotations) {
            if (annotation instanceof Description) {
                final Description description = (Description) annotation;
                return description.value();
            }
        }
        return "Not documented!!!";
    }

	
	@Override
	default String getCitations() {
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
        // only add citation if it is not already processed
    	for (Citation citation : getCitationList()) {
            if (!citations.contains(citation.value())) {
                // and there is actually a citation to add
                buf.append("\n");
                buf.append(citation.value());
                buf.append("\n");
                citations.add(citation.value());
            }
        }
        try {
            for (final BEASTInterface beastObject : listActiveBEASTObjects()) {
                buf.append(beastObject.getCitations(citations, IDs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buf.toString();
    } // getCitations


    /**
     * create list of inputs to this plug-in *
     */
    default List<Input<?>> listInputs() {
        final List<Input<?>> inputs = new ArrayList<>();
        
        Map<String, Input> inputNames = new LinkedHashMap<>();
        
        // First, collect all Inputs
        final Field[] fields = getClass().getFields();
        for (final Field field : fields) {
            if (field.getType().isAssignableFrom(Input.class)) {
            	try {
            		final Input<?> input = (Input<?>) field.get(this);
            		if (inputNames.keySet().contains(input.getName())) {
            			throw new RuntimeException("Programmer error in class " + this.getClass().getName() + ": multiple inputs with name " + input.getName() + " found (perhaps in sub and super classes)\n"
            					+ "Classes should have unique input names");
            		}
            		inputNames.put(input.getName(), input);
            		inputs.add(input);
            	} catch (IllegalAccessException e) {
            		// not a publicly accessible input, ignore
            	}
            }
        }
        
        listAnnotatedInputs(this, inputs, inputNames);
    
	    return inputs;
    } // listInputs
    
    default void listAnnotatedInputs(Object o, List<Input<?>> inputs, Map<String, Input> inputNames) {
        // Second, collect InputForAnnotatedConstructors of annotated constructor (if any)
	    Constructor<?>[] allConstructors = o.getClass().getDeclaredConstructors();
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
	    			Class<?> type = types[i + offset];
	    			Class<?> clazz = type;
					if (type.isArray()) {
						// nothing to do
					} else if (type.isPrimitive()) {
						// nothing to do
						
						// it's probably a primitive
//						if (type.equals(Integer.TYPE)) {
//							clazz = int.class;
//						} else if (type.equals(Long.TYPE)) {
//							clazz = long.class;
//						} else if (type.equals(Short.TYPE)) {
//							clazz = short.class;
//						} else if (type.equals(Float.TYPE)) {
//							clazz = float.class;
//						} else if (type.equals(Double.TYPE)) {
//							clazz = double.class;
//						} else if (type.equals(Boolean.TYPE)) {
//							clazz = boolean.class;
//						} else if (type.equals(Byte.TYPE)) {
//							clazz = byte.class;							
//						} else if (type.equals(Character.TYPE)) {
//							clazz = char.class;							
//						}
					} else {
						try {
							clazz = BEASTClassLoader.forName(type.getTypeName());
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
							throw new RuntimeException("Cannot find type " + e.getMessage());
						}
					}
	    			if (clazz.isAssignableFrom(List.class)) {
                        Type[] genericTypes2 = ((ParameterizedType) gtypes[i + offset]).getActualTypeArguments();
                        Class<?> theClass = (Class<?>) genericTypes2[0];
	    				InputForAnnotatedConstructor<?> t = null;
						try {
							t = new InputForAnnotatedConstructor<>(o, theClass, param);
						} catch (NoSuchMethodException | SecurityException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	            		if (inputNames.keySet().contains(t.getName())) {
	            			Input input = inputNames.get(t.getName());
	            			if (!(input instanceof InputForAnnotatedConstructor)) {
	                			throw new RuntimeException("Programmer error in class " + this.getClass().getName() + ": multiple inputs with name " + input.getName() + " found (perhaps in sub and super classes)\n"
	                					+ "Classes should have unique input names");	            				
	            			}
	            			if (!input.equals(t)) {
	            				throw new RuntimeException("Programmer error: @Param inputs with same name ("+ input.getName() +") should be equal to previously used annotations");
	            			}
	            		} else {
	            			inputNames.put(t.getName(), t);
	            			inputs.add(t);
	            		}
	    			} else {
	    				InputForAnnotatedConstructor<?> t = null;
						try {
							t = new InputForAnnotatedConstructor<>(o, types[i + offset], param);
						} catch (NoSuchMethodException | SecurityException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	            		if (inputNames.keySet().contains(t.getName())) {
	            			Input input = inputNames.get(t.getName());
	            			if (!(input instanceof InputForAnnotatedConstructor)) {
	                			throw new RuntimeException("Programmer error in class " + this.getClass().getName() + ": multiple inputs with name " + input.getName() + " found (perhaps in sub and super classes)\n"
	                					+ "Classes should have unique input names");	            				
	            			}
	            			if ((input.defaultValue != null && !input.defaultValue.toString().equals(t.defaultValue.toString())) ||
	            				!input.getTipText().equals(t.getTipText())) {
	            				throw new RuntimeException("Programmer error: @Param inputs with same name should be equal to previously used annotations");
	            			}
	            		} else {
	            			inputNames.put(t.getName(), t);
	            			inputs.add(t);
	            		}
	    			}
	    		}
	    	}
		}

    }
    
 
    /**
     * create array of all plug-ins in the inputs that are instantiated.
     * If the input is a List of plug-ins, these individual plug-ins are
     * added to the list.
     *
     * @return list of all active plug-ins
     * @throws IllegalAccessException
     */
    default List<BEASTInterface> listActiveBEASTObjects() {
        final List<BEASTInterface> beastObjects = new ArrayList<>();

        for (Input<?> input : getInputs().values()) {
        	if (input.get() != null) {
        		if (input.get() instanceof List<?>) {
        			final List<?> list = (List<?>) input.get();
        			for (final Object o : list) {
        				if (o instanceof BEASTInterface) {
        					beastObjects.add((BEASTInterface) o);
        				}
        			}
        		} else if (input.get() != null && input.get() instanceof BEASTInterface) {
        			beastObjects.add((BEASTInterface) input.get());
        		}
        	}
        }
        return beastObjects;
    }

    @Deprecated /** use listActiveBEASTObjects instead **/
    default List<BEASTInterface> listActivePlugins() throws IllegalArgumentException, IllegalAccessException {
    	return listActiveBEASTObjects();
    } // listActivePlugins

    /**
     * get description of an input
     *
     * @param name of the input
     * @return description of input
     */
    default String getTipText(final String name) throws IllegalArgumentException, IllegalAccessException {
		try {
	    	Input<?> input = getInput(name);
	    	if (input != null) {
	    		return input.getTipText();
	    	}
		} catch (Exception e) {
			// whatever happened, getting a tip text is no reason to interrupt anything,
			// so ignore and return null
		}
        return null;
    } // getTipText


    /**
     * check whether the input is an Integer, Double, Boolean or String *
     */
    default boolean isPrimitive(final String name) {
        final Input<?> input = getInput(name);
        final Class<?> inputType = input.getType();

        if (inputType == null) {
            input.determineClass(this);
        }

        assert inputType != null;
        for (final Class<?> c : new Class[]{Integer.class, Long.class, Double.class, Float.class, Boolean.class, String.class}) {
            if (inputType.isAssignableFrom(c)) {
                return true;
            }
        }
        return false;
    } // isPrimitive

    /**
     * get value of an input by input name *
     */
    default Object getInputValue(final String name) {
        final Input<?> input = getInput(name);
        return input.get();
    } // getInputValue

    /**
     * set value of an input by input name *
     */
    default void setInputValue(final String name, final Object value) {
        final Input<?> input = getInput(name);
        if (!input.canSetValue(value, this)) {
            throw new RuntimeException("Cannot set input value of " + name);
        }
        input.setValue(value, this);
    } // setInputValue

    /**
     * get input by input name *
     */
    default Input<?> getInput(final String name) {
    	
    	Map<String, Input<?>> inputs = getInputs();
    	if (inputs.containsKey(name)) {
    		return inputs.get(name);
    	}

        String inputNames = " "; // <- space here to prevent error in .substring below
        for (final Input<?> input : listInputs()) {
            inputNames += input.getName() + ",";
        }
        throw new IllegalArgumentException("This BEASTInterface (" + (this.getID() == null ? this.getClass().getName() : this.getID()) + ") has no input with name " + name + ". " +
                "Choose one of these inputs:" + inputNames.substring(0, inputNames.length() - 1));
    } // getInput


    /**
     * check validation rules for all its inputs
     * 
     * @throws IllegalArgumentException when validation fails
     */
    default void validateInputs() {
        try {
            for (final Input<?> input : listInputs()) {
                input.validate();
            }
        } catch (IllegalArgumentException ex) {
            Log.err.println("Validation error when initializing object " +
                    this.getClass().getName() + " (id " + getID() + "):");
            throw ex;
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

    default void getPredecessors(final List<BEASTInterface> predecessors) {
        predecessors.add(this);
        for (final BEASTInterface beastObject2 : listActiveBEASTObjects()) {
            if (!predecessors.contains(beastObject2)) {
                beastObject2.getPredecessors(predecessors);
            }
        }
    }

    
    /** Determine class of all of the inputs of this object 
     * if that has not already happened
     */
    default void determindClassOfInputs() {
    	for (Input<?> input : listInputs()) {
    		if (input.getType() == null) {
    			input.determineClass(this);
    		}
    	}
    }

    /**
     * Returns true if the object implementing this interface should
     * NOT be duplicated by BeautiDoc.deepCopyPlugin().  Used to avoid
     * unwanted duplication of shared objects by the partition cloning
     * operation.
     */
    default boolean notCloneable() {
        return false;
    }
    
    
}


