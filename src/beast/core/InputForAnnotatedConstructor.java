package beast.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import beast.core.parameter.RealParameter;
import beast.core.util.Log;

@Description("Emulates the behaviour of an Input for constructors annotated with Param annotations.")
public class InputForAnnotatedConstructor<T> extends Input<T> {
	
	/** BEAST object for which to emulate this Input **/
	BEASTInterface beastObject;
	
	/** get and set methods **/
	Method getter, setter;
	
	
	public InputForAnnotatedConstructor(BEASTInterface beastObject, Class<?> theClass, Param param) throws NoSuchMethodException, SecurityException,  IllegalArgumentException  {
		if (beastObject == null) {
			throw new NullPointerException();
		}
		this.beastObject = beastObject;
		
		if (theClass == null) {
			throw new NullPointerException();
		}
		this.theClass = theClass;
		
		// TODO: handle defaultValue from Param annotations
		// this.defaultValue = param.defaultValue();
		if (param.description().trim().length() == 0) {
			Log.warning.println("Param annotation found without proper description " + param.toString());
		}
		this.tipText = param.description();
		
		if (name == null) {
			throw new NullPointerException();
		}
		this.name = param.name();

		this.rule = param.optional() ? Validate.OPTIONAL : Validate.REQUIRED;
		
		String methodName = "get" + 
		    	name.substring(0, 1).toUpperCase() +
		    	name.substring(1);
		try {
			getter = beastObject.getClass().getMethod(methodName);
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
			Log.err.println("Programmer error: when getting here an InputType was identified, but no getter for Param annotation found");
			throw e;
		}

		methodName = "set" + 
		    	name.substring(0, 1).toUpperCase() +
		    	name.substring(1);
		try {
			setter = beastObject.getClass().getMethod(methodName, theClass);
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
			Log.err.println("Programmer error: when getting here an InputType was identified, but no setter for Param annotation found");
			throw e;
		}
	}
	
	@Override
	public void setValue(Object value, BEASTInterface plugin) {
        if (value == null) {
            if (this.value != null) {
                if (this.value instanceof BEASTInterface) {
                    ((BEASTInterface) this.value).getOutputs().remove(plugin);
                }
            }
            setValue(null);
            return;
        }
        if (value instanceof String) {
            try {
                setStringValue((String) value);
            } catch (Exception e) {
                e.printStackTrace();
            	Log.warning.println("Failed to set the string value to '" + value + "' for beastobject id=" + plugin.getID());
                throw new RuntimeException("Failed to set the string value to '" + value + "' for beastobject id=" + plugin.getID());
            }
        } else if (this.value != null && this.value instanceof List<?>) {
            if (theClass.isAssignableFrom(value.getClass())) {
//              // don't insert duplicates
                // RRB: DO insert duplicates: this way CompoundValuable can be set up to 
                // contain rate matrices with dependent variables/parameters.
                // There does not seem to be an example where a duplicate insertion is a problem...
//                for (Object o : vector) {
//                    if (o.equals(value)) {
//                        return;
//                    }
//                }
                setValue(value);
                if (value instanceof BEASTInterface) {
                    ((BEASTInterface) value).getOutputs().add(plugin);
                }
            } else if (value instanceof List<?> && theClass.isAssignableFrom(((List<?>) value).get(0).getClass())) {
                // add all elements in given list to input list.
                for (Object v : ((List<?>) value)) {
                    setValue(v);
                    if (v instanceof BEASTInterface) {
                        ((BEASTInterface) v).getOutputs().add(plugin);
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
                        ((BEASTInterface) this.value).getOutputs().remove(plugin);
                    }
                    ((BEASTInterface) value).getOutputs().add(plugin);
                }
                setValue(value);
            } else {
                throw new RuntimeException("Input 102: type mismatch for input " + getName());
            }
        }
	}

	
	private void setValue(Object value) {
		try {
			setter.invoke(beastObject, value);
			if (value instanceof BEASTInterface) {
	              ((BEASTInterface) value).getOutputs().add(beastObject);
			}
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    /**
     * Try to parse value of string into Integer, Double or Boolean,
     * or it this types differs, just assign as string.
     *
     * @param sValue value representation
     * @throws Exception when all conversions fail
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setStringValue(final String sValue) throws Exception {
        // figure out the type of T and create object based on T=Integer, T=Double, T=Boolean, T=Valuable
        if (value instanceof List<?>) {
            List list = (List) get();
            list.clear();
            // remove start and end spaces
            String sValue2 = sValue.replaceAll("^\\s+", "");
            sValue2 = sValue2.replaceAll("\\s+$", "");
            // split into space-separated bits
            String[] sValues = sValue2.split("\\s+");
            for (int i = 0; i < sValues.length; i++) {
                if (theClass.equals(Integer.class)) {
                    list.add(new Integer(sValues[i % sValues.length]));
                } else if (theClass.equals(Double.class)) {
                    list.add(new Double(sValues[i % sValues.length]));
                } else if (theClass.equals(Boolean.class)) {
                    String str = sValues[i % sValues.length].toLowerCase();
                    list.add(str.equals("1") || str.equals("true") || str.equals("yes"));
                } else if (theClass.equals(String.class)) {
                    list.add(new String(sValues[i % sValues.length]));
                }
            }
            return;
        }

        if (theClass.equals(Integer.class)) {
            setValue((T) new Integer(sValue));
            return;
        }
        if (theClass.equals(Double.class)) {
        	setValue((T) new Double(sValue));
            return;
        }
        if (theClass.equals(Boolean.class)) {
            final String sValue2 = sValue.toLowerCase();
            if (sValue2.equals("yes") || sValue2.equals("true")) {
            	setValue((T) Boolean.TRUE);
                return;
            } else if (sValue2.equals("no") || sValue2.equals("false")) {
            	setValue((T) Boolean.FALSE);
                return;
            }
        }
        if (theClass.equals(Function.class)) {
            final RealParameter param = new RealParameter();
            param.initByName("value", sValue, "upper", 0.0, "lower", 0.0, "dimension", 1);
            param.initAndValidate();
        	setValue((T) param);
            param.getOutputs().add(beastObject);
            return;
        }

        if (theClass.isEnum()) {
        	if (possibleValues == null) {
        		possibleValues = (T[]) theClass.getDeclaringClass().getEnumConstants();
        	}
            for (final T t : possibleValues) {
                if (sValue.equals(t.toString())) {
                	setValue(t);
                    return;
                }
            }
            throw new Exception("Input 104: value " + sValue + " not found. Select one of " + Arrays.toString(possibleValues));
        }

        // call a string constructor of theClass
        try {
            Constructor ctor;
            Object v = sValue;
            try {
            	ctor = theClass.getDeclaredConstructor(String.class);
            } catch (NoSuchMethodException e) {
            	// we get here if there is not String constructor
            	// try integer constructor instead
            	try {
            		if (sValue.startsWith("0x")) {
            			v = Integer.parseInt(sValue.substring(2), 16);
            		} else {
            			v = Integer.parseInt(sValue);
            		}
                	ctor = theClass.getDeclaredConstructor(int.class);
                	
            	} catch (NumberFormatException e2) {
                	// could not parse as integer, try double instead
            		v = Double.parseDouble(sValue);
                	ctor = theClass.getDeclaredConstructor(double.class);
            	}
            }
            ctor.setAccessible(true);
            final Object o = ctor.newInstance(v);
            setValue(o);
            if (o instanceof BEASTInterface) {
                ((BEASTInterface) o).getOutputs().add(beastObject);
            }
        } catch (Exception e) {
            throw new Exception("Input 103: type mismatch, cannot initialize input '" + getName() +
                    "' with value '" + sValue + "'.\nExpected something of type " + getType().getName() +
                    ". " + (e.getMessage() != null ? e.getMessage() : ""));
        }
    } // setStringValue

	
	@SuppressWarnings("unchecked")
	@Override
	public T get() {
		try {
			return (T) getter.invoke(beastObject);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
