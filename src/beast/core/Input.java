/*
* File Input.java
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


import java.io.File;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;

import beast.core.parameter.RealParameter;




/**
 * Represents input of a BEASTObject class.
 * Inputs connect BEASTObjects with outputs of other BEASTObjects,
 * e.g. a Logger can get the result it needs to log from a
 * BEASTObject that actually performs a calculation.
 */
public class Input<T> {
    /**
     * input name, used for identification when getting/setting values of a plug-in *
     */
    String name = "";

    /**
     * short description of the function of this particular input *
     */
    String tipText = "";

    /**
     * value represented by this input
     */
    T value;

    /**
     * Type of T, automatically determined when setting a new value.
     * Used for type checking.
     */
    protected Class<?> theClass;

    /**
     * validation rules *
     */
    public enum Validate {
        OPTIONAL, REQUIRED, XOR, FORBIDDEN
    }

    // (Q2R) I am surprised the default is not required ....

    Validate rule = Validate.OPTIONAL;
    /**
     * used only if validation rule is XOR *
     */
    Input<?> other;
    public T defaultValue;
    /**
     * Possible values for enumerations, e.g. if
     * an input can be any of "constant", "linear", "quadratic"
     * this array contains these values. Used for validation and user interfaces.
     */
    public T[] possibleValues;

    /**
     * constructors *
     */
    public Input() {
    }

    /**
     * simple constructor, requiring only the input name and tiptext
     */
    public Input(String sName, String sTipText) {
        name = sName;
        tipText = sTipText;
        value = null;
        checkName();
    } // c'tor

    /**
     * simple constructor as above but with type pre-specified.
     * This allows inputs of types that cannot be determined through
     * introspection, such as template class inputs, e.g. Input<Parameter<?>>
     */
    public Input(String sName, String sTipText, Class<?> theClass) {
        this(sName, sTipText);
        this.theClass = theClass;
    } // c'tor

    /**
     * constructor for List<>
     */
    public Input(String sName, String sTipText, T startValue) {
        this(sName, sTipText);
        value = startValue;
        defaultValue = startValue;
    } // c'tor

    /**
     * constructor for List<> with type specified
     */
    public Input(String sName, String sTipText, T startValue, Class<?> theClass) {
        this(sName, sTipText, startValue);
        this.theClass = theClass;
    } // c'tor

    /**
     * constructor for List<> with XOR rules
     */
    public Input(String sName, String sTipText, T startValue, Validate rule, Input<?> other) {
        this(sName, sTipText, startValue);
        if (rule != Validate.XOR) {
            System.err.println("Programmer error: input rule should be XOR for this Input constructor");
        }
        this.rule = rule;
        this.other = other;
        this.other.other = this;
        this.other.rule = rule;
        checkName();
    } // c'tor

    /**
     * constructor for List<> with XOR rules with type specified
     */
    public Input(String sName, String sTipText, T startValue, Validate rule, Input<?> other, Class<?> theClass) {
        this(sName, sTipText, startValue, rule, other);
        this.theClass = theClass;
    } // c'tor


    /**
     * Constructor for REQUIRED rules for List-inputs, i.e. lists that require
     * at least one value to be specified.
     * If optional (i.e. no value need to be specified), leave the rule out
     */
    public Input(String sName, String sTipText, T startValue, Validate rule) {
        this(sName, sTipText, startValue);
        if (rule != Validate.REQUIRED) {
            System.err.println("Programmer error: input rule should be REQUIRED for this Input constructor"
                    + " (" + sName + ")");
        }
        this.rule = rule;
    } // c'tor

    /**
     * constructor for REQUIRED rules for List-inputs, with type pre-specified
     */
    public Input(String sName, String sTipText, T startValue, Validate rule, Class<?> type) {
        this(sName, sTipText, startValue, rule);
        theClass = type;
    } // c'tor

    /**
     * constructor for REQUIRED rules
     */
    public Input(String sName, String sTipText, Validate rule) {
        this(sName, sTipText);
        if (rule != Validate.REQUIRED) {
            System.err.println("Programmer error: input rule should be REQUIRED for this Input constructor"
                    + " (" + sName + ")");
        }
        this.rule = rule;
    } // c'tor

    /**
     * constructor for REQUIRED rules, with type pre-specified
     */
    public Input(String sName, String sTipText, Validate rule, Class<?> type) {
        this(sName, sTipText, rule);
        this.theClass = type;
    }

    /**
     * constructor for XOR rules *
     */
    public Input(String sName, String sTipText, Validate rule, Input<?> other) {
        this(sName, sTipText);
        if (rule != Validate.XOR) {
            System.err.println("Programmer error: input rule should be XOR for this Input constructor");
        }
        this.rule = rule;
        this.other = other;
        this.other.other = this;
        this.other.rule = rule;
    } // c'tor

    /**
     * constructor for XOR rules, with type pre-specified
     */
    public Input(String sName, String sTipText, Validate rule, Input<?> other, Class<?> type) {
        this(sName, sTipText, rule, other);
        this.theClass = type;
    }

    /**
     * constructor for enumeration.
     * Typical usage is with an array of possible String values, say ["constant","exponential","lognormal"]
     * Furthermore, a default value is required (should we have another constructor that could leave
     * the value optional? When providing a 'no-input' entry in the list and setting that as the default,
     * that should cover that situation.)
     */
    public Input(String sName, String sTipText, T startValue, T[] sPossibleValues) {
        name = sName;
        tipText = sTipText;
        value = startValue;
        defaultValue = startValue;
        possibleValues = sPossibleValues;
        checkName();
    } // c'tor

    /**
     * check name is not one of the reserved ones *
     */
    private void checkName() {
        if (name.toLowerCase().equals("id") ||
                name.toLowerCase().equals("idref") ||
                name.toLowerCase().equals("spec") ||
                name.toLowerCase().equals("name")) {
            System.err.println("Found an input with invalid name: " + name);
            System.err.println("'id', 'idref', 'spec' and 'name' are reserved and cannot be used");
            System.exit(0);
        }
    }

    /**
     * various setters and getters
     */
    public String getName() {
        return name;
    }

    public String getTipText() {
        return tipText;
    }
    
	public String getValueTipText() {
		if (theClass == Boolean.class) {
			return ("[true|false]");
		}
		if (theClass == Integer.class) {
			return ("<integer>");
		}
		if (theClass == Long.class) {
			return ("<long>");
		}
		if (theClass == Double.class) {
			return ("<double>");
		}
		if (theClass == String.class) {
			return "<string>";
		}
		if (theClass == File.class) {
			return "<filename>";
		}
		if (theClass.isEnum()) {
			return Arrays.toString(possibleValues).replaceAll(",","|");
		}
		return "";
	}
    
    public Class<?> getType() {
        return theClass;
    }
    
    public void setType(Class<?> theClass) {
        this.theClass = theClass;
    }

    public Validate getRule() {
        return rule;
    }

    public void setRule(final Validate rule) {
        this.rule = rule;
    }

    public Input<?> getOther() {
        return other;
    }

    /**
     * Get the value of this input -- not to be called from operators!!!
     * If this is a StateNode input, instead of returning
     * the actual value, the current value of the StateNode
     * is returned. This is defined as the current StateNode
     * in the State, or itself if it is not part of the state.
     *
     * @return value of this input
     */
    public T get() {
        return value;
    }

    /**
     * As get() but with this difference that the State can manage
     * whether to make a copy and register the operator.
     * <p/>
     * Only Operators should call this method.
     * Also Operators should never call Input.get(), always Input.get(operator).
     *
     * @param operator
     * @return
     */
    @SuppressWarnings("unchecked")
    public T get(final Operator operator) {
        return (T) ((StateNode) value).getCurrentEditable(operator);
    }

    /**
     * Return the dirtiness state for this input.
     * For a StateNode or list of StateNodes, report whether for any something is dirty,
     * for a CalcationNode or list of CalculationNodes, report whether any is dirty.
     * Otherwise, return false.
     * *
     */
    public boolean isDirty() {
        final T value = get();

        if (value == null) {
            return false;
        }

        if (value instanceof StateNode) {
            return ((StateNode) value).somethingIsDirty();
        }

        if (value instanceof CalculationNode) {
            return ((CalculationNode) value).isDirtyCalculation();
        }

        if (value instanceof List<?>) {
            for (final Object obj : (List<?>) value) {
                if (obj instanceof CalculationNode && ((CalculationNode) obj).isDirtyCalculation()) {
                    return true;
                } else if (obj instanceof StateNode && ((StateNode) obj).somethingIsDirty()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Sets value to this input.
     * If class is not determined yet, first determine class of declaration of
     * this input so that we can do type checking.
     * If value is of type String, try to parse the value if this input is
     * Integer, Double or Boolean.
     * If this input is a List, instead of setting this value, the value is
     * added to the vector.
     * Otherwise, m_value is assigned to value.
     *
     * @param value
     * @param plugin
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void setValue(final Object value, final BEASTObject plugin) throws Exception {
        if (value == null) {
            if (this.value != null) {
                if (this.value instanceof BEASTObject) {
                    ((BEASTObject) this.value).outputs.remove(plugin);
                }
            }
            this.value = null;
            return;
        }
        if (theClass == null) {
            determineClass(plugin);
        }
        if (value instanceof String) {
            setStringValue((String) value, plugin);
        } else if (this.value != null && this.value instanceof List<?>) {
            if (theClass.isAssignableFrom(value.getClass())) {
                @SuppressWarnings("rawtypes") final
                List vector = (List) this.value;
//              // don't insert duplicates
                // RRB: DO insert duplicates: this way CompoundValuable can be set up to 
                // contain rate matrices with dependent variables/parameters.
                // There does not seem to be an example where a duplicate insertion is a problem...
//                for (Object o : vector) {
//                    if (o.equals(value)) {
//                        return;
//                    }
//                }
                vector.add(value);
                if (value instanceof BEASTObject) {
                    ((BEASTObject) value).outputs.add(plugin);
                }
            }

            else if (value instanceof List<?> && theClass.isAssignableFrom(((List<?>)value).get(0).getClass())) {
                // add all elements in given list to input list.
                @SuppressWarnings("rawtypes")
				final List<Object> vector = (List) this.value;
                for (Object v : ((List<?>)value)) {
                    vector.add(v);
                    if (v instanceof BEASTObject) {
                        ((BEASTObject) v).outputs.add(plugin);
                    }
                }
            }
            else {
                throw new Exception("Input 101: type mismatch for input " + getName() +
                        ". " + theClass.getName() +".isAssignableFrom(" + value.getClass() + ")=false");
            }

        } else {
            if (theClass.isAssignableFrom(value.getClass())) {
                if (value instanceof BEASTObject) {
                    if (this.value != null) {
                        ((BEASTObject) this.value).outputs.remove(plugin);
                    }
                    ((BEASTObject) value).outputs.add(plugin);
                }
                this.value = (T) value;
            } else {
                throw new Exception("Input 102: type mismatch for input " + getName());
            }
        }
    }

    /**
     * Call custom input validation.
     * For an input with name "name", the method canSetName will be invoked,
     * that is, 'canSet' + the name of the input with first letter capitalised.
     * The canSetName(Object o) method should have one argument of type Object.
     * <p/>
     * It is best for Beauti to throw an Exception from canSetName() with some
     * diagnostic info when the value cannot be set.
     */
    public boolean canSetValue(Object value, BEASTObject plugin) throws Exception {
        String inputName = new String(name.charAt(0) + "").toUpperCase() + name.substring(1);
        try {
            Method method = plugin.getClass().getMethod("canSet" + inputName, Object.class);
            //System.err.println("Calling method " + plugin.getClass().getName() +"."+ method.getName());
            Object o = method.invoke(plugin, value);
            return (Boolean) o;
        } catch (java.lang.NoSuchMethodException e) {
            return true;
        } catch (java.lang.reflect.InvocationTargetException e) {
        	System.err.println(plugin.getClass().getName() + "." + getName() + ": " + e.getCause());
        	
            if (e.getCause() != null) {
                throw new Exception(e.getCause().getMessage());
            }
            return false;
        }
    }

    /**
     * Determine class through introspection,
     * This sets the theClass member of Input<T> to the actual value of T.
     * If T is a vector, i.e. Input<List<S>>, the actual value of S
     * is assigned instead
     *
     * @param plugin whose type is to be determined
     * @throws Exception
     */
    public void determineClass(final Object plugin) throws Exception {
        try {
            final Field[] fields = plugin.getClass().getFields();
            // find this input in the plugin
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getType().isAssignableFrom(Input.class)) {
                    Input<?> input = (Input<?>) fields[i].get(plugin);
                    if (input == this) {
                        // found the input, now determine the type of the input
                        Type t = fields[i].getGenericType();
                        Type[] genericTypes = ((ParameterizedType) t).getActualTypeArguments();
                        // check if it is a List
                        // NB: if the List is not initialised, there is no way 
                        // to determine the type (that I know of...)
                        if (value != null && value instanceof List<?>) {
                            Type[] genericTypes2 = ((ParameterizedType) genericTypes[0]).getActualTypeArguments();
                            theClass = (Class<?>) genericTypes2[0];
                        // gettting type of map is not possible?!?
                        //} else if (value != null && value instanceof Map<?,?>) {
                        //    Type[] genericTypes2 = ((ParameterizedType) genericTypes[0]).getActualTypeArguments();
                        //    theClass = (Class<?>) genericTypes2[0];
                        } else {
                            // it is not a list (or if it is, this will fail)
                            try {
                                theClass = (Class<?>) genericTypes[0];
                            } catch (Exception e) {
                            	// resolve ID
                            	String id = "";
                                Method method = plugin.getClass().getMethod("getID");
                                if (method != null) {
                                	id = method.invoke(plugin).toString();
                                }
                                // assemble error message
                                System.err.println(plugin.getClass().getName() + " " + id + " failed. " +
                                        "Possibly template or abstract Plugin used " +
                                        "or if it is a list, the list was not initilised???");
                                System.err.println("class is " + plugin.getClass());
                                e.printStackTrace(System.err);
                                System.exit(0);
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // determineClass

    /**
     * Try to parse value of string into Integer, Double or Boolean,
     * or it this types differs, just assign as string.
     *
     * @param sValue value representation
     * @throws Exception when all conversions fail
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setStringValue(final String sValue, final BEASTObject plugin) throws Exception {
        // figure out the type of T and create object based on T=Integer, T=Double, T=Boolean, T=Valuable
        if (value instanceof List<?>) {
        	List list = (List) value;
        	list.clear();
			// remove start and end spaces
			String sValue2 = sValue.replaceAll("^\\s+", "");
			sValue2 = sValue2.replaceAll("\\s+$", "");
			// split into space-separated bits
			String[] sValues = sValue2.split("\\s+");
			for (int i = 0; i < sValues.length; i++) {
	            if (theClass.equals(Integer.class)) {
	            	list.add(new Integer(sValues[i % sValues.length]));
	            }
	            else if (theClass.equals(Double.class)) {
	            	list.add(new Double(sValues[i % sValues.length]));
	            }
	            else if (theClass.equals(Boolean.class)) {
	            	String str = sValues[i % sValues.length].toLowerCase();
	            	list.add(str.equals("1") || str.equals("true") || str.equals("yes"));
	            }
	            else if (theClass.equals(String.class)) {
	            	list.add(new String(sValues[i % sValues.length]));
	            }
			}
			return;
        }
    	
        if (theClass.equals(Integer.class)) {
            value = (T) new Integer(sValue);
            return;
        }
        if (theClass.equals(Double.class)) {
            value = (T) new Double(sValue);
            return;
        }
        if (theClass.equals(Boolean.class)) {
            final String sValue2 = sValue.toLowerCase();
            if (sValue2.equals("yes") || sValue2.equals("true")) {
                value = (T) Boolean.TRUE;
                return;
            } else if (sValue2.equals("no") || sValue2.equals("false")) {
                value = (T) Boolean.FALSE;
                return;
            }
        }
        if (theClass.equals(Function.class)) {
            final RealParameter param = new RealParameter();
            param.initByName("value", sValue, "upper", 0.0, "lower", 0.0, "dimension", 1);
            param.initAndValidate();
            if (value != null && value instanceof List) {
                ((List) value).add(param);
            } else {
                value = (T) param;
            }
            param.outputs.add(plugin);
            return;
        }

        if (theClass.isEnum()) {
            for (final T t : possibleValues) {
                if (sValue.equals(t.toString())) {
                    value = t;
                    return;
                }
            }
            throw new Exception("Input 104: value " + sValue + " not found. Select one of " + Arrays.toString(possibleValues));
        }

        // call a string constructor of theClass
        try {
            final Constructor ctor = theClass.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            final Object o = ctor.newInstance(sValue);
            if (value != null && value instanceof List) {
                ((List) value).add(o);
            } else {
                value = (T) o;
            }
            if (o instanceof BEASTObject) {
                ((BEASTObject) o).outputs.add(plugin);
            }
        } catch (Exception e) {
            throw new Exception("Input 103: type mismatch, cannot initialize input '" + getName() +
                    "' with value '" + sValue + "'.\nExpected something of type " + getType().getName() +
                    ". " + (e.getMessage() != null ? e.getMessage() : ""));
        }
    } // setStringValue

    /**
     * validate input according to validation rule *
     *
     * @throws Exception when validation fails. why not return a string?
     */
    public void validate() throws Exception {
        if (possibleValues != null) {
            // it is an enumeration, check the value is in the list
            boolean bFound = false;
            for (final T value : possibleValues) {
                if (value.equals(this.value)) {
                    bFound = true;
                }
            }
            if (!bFound) {
                throw new Exception("Expected one of " + Arrays.toString(possibleValues) + " but got " + this.value);
            }
        }

        switch (rule) {
            case OPTIONAL:
                // noting to do
                break;
            case REQUIRED:
                if (get() == null) {
                    throw new Exception("Input '" + getName() + "' must be specified.");
                }
                if (get() instanceof List<?>) {
                    if (((List<?>) get()).size() == 0) {
                        throw new Exception("At least one input of name '" + getName() + "' must be specified.");
                    }
                }
                break;
            case XOR:
                if (get() == null) {
                    if (other.get() == null) {
                        throw new Exception("Either input '" + getName() + "' or '" + other.getName() + "' needs to be specified");
                    }
                } else {
                    if (other.get() != null) {
                        throw new Exception("Only one of input '" + getName() + "' and '" + other.getName() + "' must be specified (not both)");
                    }
                }
                // noting to do
                break;
            case FORBIDDEN:
                if (get() instanceof List<?>) {
                    if (((List<?>) get()).size() > 0) {
                        throw new Exception("No input of name '" + getName() + "' must be specified.");
                    }
                } else if (get() == null) {
                    throw new Exception("Input '" + getName() + "' must not be specified.");
                }
                break;
        }
    } // validate

} // class Input
