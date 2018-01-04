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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import beast.core.parameter.RealParameter;
import beast.core.util.Log;


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
    public Input(String name, String tipText) {
        this.name = name;
        this.tipText = tipText;
        value = null;
        checkName();
    } // c'tor

    /**
     * simple constructor as above but with type pre-specified.
     * This allows inputs of types that cannot be determined through
     * introspection, such as template class inputs, e.g. Input<Parameter<?>>
     */
    public Input(String name, String tipText, Class<?> theClass) {
        this(name, tipText);
        this.theClass = theClass;
    } // c'tor

    /**
     * constructor for List<>
     */
    public Input(String name, String tipText, T startValue) {
        this(name, tipText);
        value = startValue;
        defaultValue = startValue;
    } // c'tor

    /**
     * constructor for List<> with type specified
     */
    public Input(String name, String tipText, T startValue, Class<?> theClass) {
        this(name, tipText, startValue);
        this.theClass = theClass;
    } // c'tor

    /**
     * constructor for List<> with XOR rules
     */
    public Input(String name, String tipText, T startValue, Validate rule, Input<?> other) {
        this(name, tipText, startValue);
        if (rule != Validate.XOR) {
            Log.err.println("Programmer error: input rule should be XOR for this Input constructor");
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
    public Input(String name, String tipText, T startValue, Validate rule, Input<?> other, Class<?> theClass) {
        this(name, tipText, startValue, rule, other);
        this.theClass = theClass;
    } // c'tor


    /**
     * Constructor for REQUIRED rules for List-inputs, i.e. lists that require
     * at least one value to be specified.
     * If optional (i.e. no value need to be specified), leave the rule out
     */
    public Input(String name, String tipText, T startValue, Validate rule) {
        this(name, tipText, startValue);
        /*if (rule != Validate.REQUIRED) {
            Log.err.println("Programmer error: input rule should be REQUIRED for this Input constructor"
                    + " (" + name + ")");
        }*/
        this.rule = rule;
    } // c'tor

    /**
     * constructor for REQUIRED rules for List-inputs, with type pre-specified
     */
    public Input(String name, String tipText, T startValue, Validate rule, Class<?> type) {
        this(name, tipText, startValue, rule);
        theClass = type;
    } // c'tor

    /**
     * constructor for REQUIRED rules
     */
    public Input(String name, String tipText, Validate rule) {
        this(name, tipText);
        if (rule != Validate.REQUIRED) {
            Log.err.println("Programmer error: input rule should be REQUIRED for this Input constructor"
                    + " (" + name + ")");
        }
        this.rule = rule;
    } // c'tor

    /**
     * constructor for REQUIRED rules, with type pre-specified
     */
    public Input(String name, String tipText, Validate rule, Class<?> type) {
        this(name, tipText, rule);
        this.theClass = type;
    }

    /**
     * constructor for XOR rules *
     */
    public Input(String name, String tipText, Validate rule, Input<?> other) {
        this(name, tipText);
        if (rule != Validate.XOR) {
            Log.err.println("Programmer error: input rule should be XOR for this Input constructor");
        }
        this.rule = rule;
        this.other = other;
        this.other.other = this;
        this.other.rule = rule;
    } // c'tor

    /**
     * constructor for XOR rules, with type pre-specified
     */
    public Input(String name, String tipText, Validate rule, Input<?> other, Class<?> type) {
        this(name, tipText, rule, other);
        this.theClass = type;
    }

    /**
     * constructor for enumeration.
     * Typical usage is with an array of possible String values, say ["constant","exponential","lognormal"]
     * Furthermore, a default value is required (should we have another constructor that could leave
     * the value optional? When providing a 'no-input' entry in the list and setting that as the default,
     * that should cover that situation.)
     */
    public Input(String name, String tipText, T startValue, T[] possibleValues) {
        this.name = name;
        this.tipText = tipText;
        value = startValue;
        defaultValue = startValue;
        this.possibleValues = possibleValues;
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
        	Log.err.println("Found an input with invalid name: " + name);
        	Log.err.println("'id', 'idref', 'spec' and 'name' are reserved and cannot be used");
            System.exit(1);
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

    public String getHTMLTipText() {
        return "<html>" + tipText.replaceAll("\n", "<br>") + "</html>";
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
        if (theClass == Float.class) {
            return ("<float>");
        }
        if (theClass == String.class) {
            return "<string>";
        }
        if (theClass == File.class) {
            return "<filename>";
        }
        if (theClass.isEnum()) {
            return Arrays.toString(possibleValues).replaceAll(",", "|");
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

    final public Input<?> getOther() {
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
     * @param beastObject
     */
    @SuppressWarnings("unchecked")
    public void setValue(final Object value, final BEASTInterface beastObject) {
        if (value == null) {
            if (this.value != null) {
                if (this.value instanceof BEASTInterface) {
                    ((BEASTInterface) this.value).getOutputs().remove(beastObject);
                }
            }
            this.value = null;
            return;
        }
        if (theClass == null) {
            try {
                determineClass(beastObject);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to determine class of beastobject id=" + beastObject.getID());
            }
        }
        if (value instanceof String) {
            try {
                setStringValue((String) value, beastObject);
            } catch (Exception e) {
                e.printStackTrace();
            	Log.warning.println("Failed to set the string value to '" + value + "' for beastobject id=" + beastObject.getID());
                throw new RuntimeException("Failed to set the string value to '" + value + "' for beastobject id=" + beastObject.getID());
            }
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
                if (value instanceof BEASTInterface) {
                    ((BEASTInterface) value).getOutputs().add(beastObject);
                }
            } else if (value instanceof List<?> && theClass.isAssignableFrom(((List<?>) value).get(0).getClass())) {
                // add all elements in given list to input list.
                @SuppressWarnings("rawtypes")
                final List<Object> vector = (List) this.value;
                for (Object v : ((List<?>) value)) {
                    vector.add(v);
                    if (v instanceof BEASTInterface) {
                        ((BEASTInterface) v).getOutputs().add(beastObject);
                    }
                }
            } else {
                throw new RuntimeException("Input 101: type mismatch for input " + getName() +
                        ". " + theClass.getName() + ".isAssignableFrom(" + value.getClass() + ")=false");
            }

        } else {
            if (theClass.isAssignableFrom(value.getClass())) {
                if (value instanceof BEASTInterface) {
                    if (this.value != null) {
                        ((BEASTInterface) this.value).getOutputs().remove(beastObject);
                    }
                    ((BEASTInterface) value).getOutputs().add(beastObject);
                }
                this.value = (T) value;
            } else {
                throw new RuntimeException("Input 102: type mismatch for input " + getName());
            }
        }
    }

    /** Programmer friendly version of setValue() to set value of this Input
     * This should only be called after the data type of the Input was determined 
     * earlier (so theClass != null), e.g. because seValue(value, beastObject)
     * or determindClass() was called before on this Input. 
     * Any BEASTObject created by XMLParser has its inputs  
     */
    public void set(final Object value) {
    	if (theClass == null) {
    		throw new IllegalArgumentException("Progmmer error: setValue should not be called unless that datatype of the input "
    				+ "is determined (e.g. through a call to setValue(value, beastObject))");
    	}
    	setValue(value, null);
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
    public boolean canSetValue(Object value, BEASTInterface beastObject) {
        String inputName = new String(name.charAt(0) + "").toUpperCase() + name.substring(1);
        try {
            Method method = beastObject.getClass().getMethod("canSet" + inputName, Object.class);
            //System.err.println("Calling method " + beastObject.getClass().getName() +"."+ method.getName());
            Object o = method.invoke(beastObject, value);
            return (Boolean) o;
        } catch (java.lang.NoSuchMethodException e) {
            return true;
        } catch (java.lang.reflect.InvocationTargetException e) {
        	Log.warning.println(beastObject.getClass().getName() + "." + getName() + ": " + e.getCause());

            if (e.getCause() != null) {
                throw new RuntimeException(e.getCause().getMessage());
            }
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Illegal method access attempted on beastobject id=" + beastObject.getID());
        }
    }

    /**
     * Determine class through introspection,
     * This sets the theClass member of Input<T> to the actual value of T.
     * If T is a vector, i.e. Input<List<S>>, the actual value of S
     * is assigned instead
     *
     * @param beastObject whose type is to be determined
     */
    public void determineClass(final Object beastObject) {
        try {
            final Field[] fields = beastObject.getClass().getFields();
            // find this input in the beastObject
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getType().isAssignableFrom(Input.class)) {
                    final Input<?> input = (Input<?>) fields[i].get(beastObject);
                    if (input == this) {
                        // found the input, now determine the type of the input
                        Type t = fields[i].getGenericType();
                        Type[] genericTypes = ((ParameterizedType) t).getActualTypeArguments();
                        // check if it is a List
                        // NB: if the List is not initialised, there is no way 
                        // to determine the type (that I know of...)
                        if (value != null && value instanceof List<?>) {
                            Type[] genericTypes2 = ((ParameterizedType) genericTypes[0]).getActualTypeArguments();
                            try {
                            	theClass = (Class<?>) genericTypes2[0];
                            } catch (ClassCastException e) {
                            	// can get here with parameterised types, e.g Input<List<Parameter.Base<T>>>
                            	theClass = (Class<?>) ((ParameterizedType)genericTypes2[0]).getRawType();
                            }
                            // getting type of map is not possible?!?
                            //} else if (value != null && value instanceof Map<?,?>) {
                            //    Type[] genericTypes2 = ((ParameterizedType) genericTypes[0]).getActualTypeArguments();
                            //    theClass = (Class<?>) genericTypes2[0];
                        } else {
                            // it is not a list (or if it is, this will fail)
                            try {
                            	Object o = genericTypes[0];
                            	if (o instanceof ParameterizedType) {
                                    Type rawType = ((ParameterizedType) genericTypes[0]).getRawType();
                                    // Log.warning.println(rawType.getTypeName());
                            		if (rawType.getTypeName().equals("java.util.List")) {
                            			// if we got here, value==null
                            			throw new RuntimeException("Programming error: Input<List> not initialised");
                            		}
                            	}
                                theClass = (Class<?>) o;
                            } catch (Exception e) {
                                // resolve ID
                                String id = "";
                                Method method = beastObject.getClass().getMethod("getID");
                                if (method != null) {
                                    id = (String) method.invoke(beastObject);
                                }
                                // assemble error message
                                Log.err.println(beastObject.getClass().getName() + " " + id + " failed. " +
                                        "Possibly template or abstract BEASTObject used " +
                                        "or if it is a list, the list was not initilised???");
                                Log.err.println("class is " + beastObject.getClass());
                                e.printStackTrace(System.err);
                                System.exit(1);
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
     * @param stringValue value representation
     * @throws IllegalArgumentException when all conversions fail
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setStringValue(final String stringValue, final BEASTInterface beastObject) {
        // figure out the type of T and create object based on T=Integer, T=Double, T=Boolean, T=Valuable
        if (value instanceof List<?>) {
            List list = (List) value;
            list.clear();
            // remove start and end spaces
            String stringValue2 = stringValue.replaceAll("^\\s+", "");
            stringValue2 = stringValue2.replaceAll("\\s+$", "");
            // split into space-separated bits
            String[] stringValues = stringValue2.split("\\s+");
            for (int i = 0; i < stringValues.length; i++) {
                if (theClass.equals(Integer.class)) {
                    list.add(new Integer(stringValues[i % stringValues.length]));
                } else if (theClass.equals(Double.class)) {
                    list.add(new Double(stringValues[i % stringValues.length]));
                } else if (theClass.equals(Boolean.class)) {
                    String str = stringValues[i % stringValues.length].toLowerCase();
                    list.add(str.equals("1") || str.equals("true") || str.equals("yes"));
                } else if (theClass.equals(String.class)) {
                    list.add(new String(stringValues[i % stringValues.length]));
                }
            }
            return;
        }

        if (theClass.equals(Integer.class)) {
            value = (T) new Integer(stringValue);
            return;
        }
        if (theClass.equals(Long.class)) {
            value = (T) new Long(stringValue);
            return;
        }
        if (theClass.equals(Double.class)) {
            value = (T) new Double(stringValue);
            return;
        }
        if (theClass.equals(Float.class)) {
            value = (T) new Float(stringValue);
            return;
        }
        if (theClass.equals(Boolean.class)) {
        	// RRB why the local parsing instead of using the Boolean c'tor?
//            final String valueString2 = stringValue.toLowerCase();
//            if (valueString2.equals("yes") || valueString2.equals("true")) {
//                value = (T) Boolean.TRUE;
//                return;
//            } else if (valueString2.equals("no") || valueString2.equals("false")) {
//                value = (T) Boolean.FALSE;
//                return;
//            }
        	value = (T) new Boolean(stringValue);
        	return;
        }
        if (theClass.equals(Function.class)) {
            final RealParameter param = new RealParameter();
            param.initByName("value", stringValue, "upper", 0.0, "lower", 0.0, "dimension", 1);
            param.initAndValidate();
            if (value != null && value instanceof List) {
                ((List) value).add(param);
            } else {
                value = (T) param;
            }
            param.getOutputs().add(beastObject);
            return;
        }

        if (theClass.isEnum()) {
        	if (possibleValues == null) {
        		possibleValues = (T[]) theClass.getDeclaringClass().getEnumConstants();
        	}
            for (final T t : possibleValues) {
                if (stringValue.equals(t.toString())) {
                    value = t;
                    return;
                }
            }
            throw new IllegalArgumentException("Input 104: value " + stringValue + " not found. Select one of " + Arrays.toString(possibleValues));
        }

        // call a string constructor of theClass
        try {
            Constructor ctor;
            Object v = stringValue;
            try {
            	ctor = theClass.getDeclaredConstructor(String.class);
            } catch (NoSuchMethodException e) {
            	// we get here if there is not String constructor
            	// try integer constructor instead
            	try {
            		if (stringValue.startsWith("0x")) {
            			v = Integer.parseInt(stringValue.substring(2), 16);
            		} else {
            			v = Integer.parseInt(stringValue);
            		}
                	ctor = theClass.getDeclaredConstructor(int.class);
                	
            	} catch (NumberFormatException e2) {
                	// could not parse as integer, try double instead
            		v = Double.parseDouble(stringValue);
                	ctor = theClass.getDeclaredConstructor(double.class);
            	}
            }
            ctor.setAccessible(true);
            final Object o = ctor.newInstance(v);
            if (value != null && value instanceof List) {
                ((List) value).add(o);
            } else {
                value = (T) o;
            }
            if (o instanceof BEASTInterface) {
                ((BEASTInterface) o).getOutputs().add(beastObject);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Input 103: type mismatch, cannot initialize input '" + getName() +
                    "' with value '" + stringValue + "'.\nExpected something of type " + getType().getName() +
                    ". " + (e.getMessage() != null ? e.getMessage() : ""));
        }
    } // setStringValue

    /**
     * validate input according to validation rule *
     *
     */
    public void validate() {
        if (possibleValues != null) {
            // it is an enumeration, check the value is in the list
            boolean found = false;
            for (final T value : possibleValues) {
                if (value.equals(this.value)) {
                    found = true;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Expected one of " + Arrays.toString(possibleValues) + " but got " + this.value);
            }
        }

        switch (rule) {
            case OPTIONAL:
                // noting to do
                break;
            case REQUIRED:
                if (get() == null) {
                    throw new IllegalArgumentException("Input '" + getName() + "' must be specified.");
                }
                if (get() instanceof List<?>) {
                    if (((List<?>) get()).size() == 0) {
                        throw new IllegalArgumentException("At least one input of name '" + getName() + "' must be specified.");
                    }
                }
                break;
            case XOR:
                if (get() == null) {
                    if (other.get() == null) {
                        throw new IllegalArgumentException("Either input '" + getName() + "' or '" + other.getName() + "' needs to be specified");
                    }
                } else {
                    if (other.get() != null) {
                        throw new IllegalArgumentException("Only one of input '" + getName() + "' and '" + other.getName() + "' must be specified (not both)");
                    }
                }
                // noting to do
                break;
            case FORBIDDEN:
                if (get() instanceof List<?>) {
                    if (((List<?>) get()).size() > 0) {
                        throw new IllegalArgumentException("No input of name '" + getName() + "' must be specified.");
                    }
                } else if (get() != null) {
                    throw new IllegalArgumentException("Input '" + getName() + "' must not be specified.");
                }
                break;
        }
    } // validate
    
    public String toString() {
    	return String.format("Input(\"%s\")", name);
    }

} // class Input
