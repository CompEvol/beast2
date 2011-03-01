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

import beast.core.parameter.RealParameter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents input of a Plugin class.
 * Inputs connect Plugins with outputs of other Plugins,
 * e.g. a Logger can get the result it needs to log from a
 * Plugin that actually performs a calculation.
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
    Class<?> theClass;

    /**
     * validation rules *
     */
    public enum Validate {
        OPTIONAL, REQUIRED, XOR
    }

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

    public Input(String sName, String sTipText) {
        name = sName;
        tipText = sTipText;
        value = null;
        checkName();
    } // c'tor

    public Input(String sName, String sTipText, T startValue) {
        name = sName;
        tipText = sTipText;
        value = startValue;
        defaultValue = startValue;
        checkName();
    } // c'tor

    public Input(String sName, String sTipText, T startValue, Class<?> type) {
        name = sName;
        tipText = sTipText;
        value = startValue;
        defaultValue = startValue;
        theClass = type;
        checkName();
    } // c'tor

    public Input(String sName, String sTipText, T startValue, Validate rule, Class<?> type) {
        name = sName;
        tipText = sTipText;
        value = startValue;
        defaultValue = startValue;
        theClass = type;
        this.rule = rule;
        checkName();
    } // c'tor

    /**
     * constructor for REQUIRED rules for array-inputs *
     */
    public Input(String sName, String sTipText, T startValue, Validate rule) {
        name = sName;
        tipText = sTipText;
        value = startValue;
        defaultValue = startValue;
        if (rule != Validate.REQUIRED) {
            System.err.println("Programmer error: input rule should be REQUIRED for this Input constructor"
                    + " (" + sName + ")");
        }
        this.rule = rule;
        checkName();
    } // c'tor

    /**
     * constructor for REQUIRED rules *
     */
    public Input(String sName, String sTipText, Validate rule) {
        name = sName;
        tipText = sTipText;
        value = null;
        if (rule != Validate.REQUIRED) {
            System.err.println("Programmer error: input rule should be REQUIRED for this Input constructor"
                    + " (" + sName + ")");
        }
        this.rule = rule;
        checkName();
    } // c'tor

    /**
     * constructor for XOR rules *
     */
    public Input(String sName, String sTipText, Validate rule, Input<?> other) {
        name = sName;
        tipText = sTipText;
        value = null;
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

    /** check name is not one of the reserved ones **/
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

    public Class<?> getType() {
        return theClass;
    }

    public Validate getRule() {
        return rule;
    }

    public void setRule(Validate rule) {
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
    @SuppressWarnings("unchecked")
    public T get() {
        if (value instanceof StateNode) {
            // return current version of StateNode
            // TODO: rrb: the commented line seems a lot slower. Why???
            //value=(T) ((StateNode)value).getCurrent();
            return (T) ((StateNode) value).getCurrent();
        }
        // TODO: is there a smarter way to test this is a list of StateNodes?
        if (value instanceof List && ((List<?>) value).size() > 0 && ((List<?>) value).get(0) instanceof StateNode) {
            // if it is a list of StateNodes, make sure the current version is returned
            List<StateNode> tmp = new ArrayList<StateNode>();
            for (Object o : (List<?>) value) {
                // need to check every element, since the first element
                // may be a StateNode (e.g. for logging), but the rest
                // may not.
                if (o instanceof StateNode) {
                    tmp.add(((StateNode) o).getCurrent());
                } else {
                    return value;
                }
            }
            return (T) tmp;
        }
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
    public T get(Operator operator) {
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
        T value = get();

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
            for (Object o : (List<?>) value) {
                if (o instanceof CalculationNode && ((CalculationNode) o).isDirtyCalculation()) {
                    return true;
                } else if (o instanceof StateNode && ((StateNode) o).somethingIsDirty()) {
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
    public void setValue(Object value, Plugin plugin) throws Exception {
        if (value == null) {
            this.value = null;
            return;
        }
        if (theClass == null) {
            determineClass(plugin);
        }
        if (value instanceof String) {
            setStringValue((String) value);
        } else if (this.value != null && this.value instanceof List<?>) {
            if (theClass.isAssignableFrom(value.getClass())) {
                @SuppressWarnings("rawtypes")
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
            } else {
                throw new Exception("Input 101: type mismatch for input " + getName());
            }

        } else {
            if (theClass.isAssignableFrom(value.getClass())) {
                this.value = (T) value;
            } else {
                throw new Exception("Input 102: type mismatch for input " + getName());
            }
        }
    }

    /**
     * Determine class through introspection,
     * This sets the m_class member of Input<T> to the actual value of T.
     * If T is a vector, i.e. Input<List<S>>, the actual value of S
     * is assigned instead
     *
     * @param plugin whose type is to be determined
     * @throws Exception
     */
    public void determineClass(Plugin plugin) throws Exception {
        try {
            Field[] fields = plugin.getClass().getFields();
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getType().isAssignableFrom(Input.class)) {
                    Input<?> input = (Input<?>) fields[i].get(plugin);
                    if (input == this) {
                        Type t = fields[i].getGenericType();
                        Type[] genericTypes = ((ParameterizedType) t).getActualTypeArguments();
                        if (value != null && value instanceof ArrayList<?>) {
                            Type[] genericTypes2 = ((ParameterizedType) genericTypes[0]).getActualTypeArguments();
                            theClass = (Class<?>) genericTypes2[0];
                        } else {


                            try {
                                theClass = (Class<?>) genericTypes[0];
                            } catch (Exception e) {
                                System.err.println(plugin.getClass().getName() + " " + plugin.getID() + " failed. Possibly template or abstract Plugin used???");
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
    private void setStringValue(String sValue) throws Exception {
        // figure out the type of T and create object based on T=Integer, T=Double, T=Boolean, T=Valuable
        if (theClass.equals(Integer.class)) {
            value = (T) new Integer(sValue);
            return;
        }
        if (theClass.equals(Double.class)) {
            value = (T) new Double(sValue);
            return;
        }
        if (theClass.equals(Boolean.class)) {
            String sValue2 = sValue.toLowerCase();
            if (sValue2.equals("yes") || sValue2.equals("true")) {
                value = (T) Boolean.TRUE;
                return;
            } else if (sValue2.equals("no") || sValue2.equals("false")) {
                value = (T) Boolean.FALSE;
                return;
            }
        }
        if (theClass.equals(Valuable.class)) {
            RealParameter param = new RealParameter(sValue, 0.0, 0.0, 1);
            param.initAndValidate();
            if (value != null && value instanceof List) {
                ((List) value).add(param);
            } else {
                value = (T) param;
            }
            return;
        }

        if (theClass.isEnum()) {
            for (T t : possibleValues) {
                if (sValue.equals(t.toString())) {
                    value = t;
                    return;
                }
            }
            throw new Exception("Input 104: value " + sValue + " not found in " + Arrays.toString(possibleValues));
        }

        // call a string constructor of theClass
        try {
            Constructor ctor = theClass.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            Object o = ctor.newInstance(sValue);
            if (value != null && value instanceof List) {
                ((List) value).add(o);
            } else {
                value = (T) o;
            }
        } catch (Exception e) {
            throw new Exception("Input 103: type mismatch, cannot initialize input '" + getName() +
                    "' with value '" + sValue + "' because " + e.getMessage());
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
            for (T value : possibleValues) {
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
        }
    } // validate

} // class Input
