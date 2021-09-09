package beast.base;


import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

@Description("Emulates the behaviour of an Input for constructors annotated with Param annotations.")
public class InputForAnnotatedConstructor<T> extends Input<T> {
	
	/** BEAST object for which to emulate this Input **/
	Object beastObject;
	
	/** get and set methods **/
	Method getter, setter;
	
	
	public InputForAnnotatedConstructor(Object beastObject, Class<?> theClass, Param param) throws NoSuchMethodException, SecurityException,  IllegalArgumentException  {
		if (beastObject == null) {
			throw new NullPointerException();
		}
		this.beastObject = beastObject;
		
		if (theClass == null) {
			throw new NullPointerException();
		}
		this.theClass = theClass;
				
		if (name == null) {
			throw new NullPointerException();
		}
		this.name = param.name();

		this.rule = param.optional() ? Validate.OPTIONAL : Validate.REQUIRED;
		
		String methodName = "get" + 
		    	name.substring(0, 1).toUpperCase() +
		    	name.substring(1)
		    	// + "List"
		    	;
		try {
			getter = beastObject.getClass().getMethod(methodName);
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
			Log.err.println("Programmer error: when getting here an InputType was identified, but no getter for Param annotation found");
			throw e;
		}

		methodName = "set" + 
		    	name.substring(0, 1).toUpperCase() +
		    	name.substring(1)
		    	//  + "List"
		    	;
		try {
			setter = beastObject.getClass().getMethod(methodName, theClass /*List.class*/);
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
			Log.err.println("Programmer error: when getting here an InputType was identified, but no setter for Param annotation found");
			throw e;
		}
		
		this.tipText = param.description().trim();
		for (Annotation annotation :getter.getAnnotations()) {
			if (annotation.annotationType() == Description.class) {
				if (this.tipText.length() > 0) {
					throw new RuntimeException("Programmer error: Double description found on @Param(" + name + ") and its getter method " + theClass.getName() + "." + getter.getName() +"()"); 
				}
				this.tipText = ((Description) annotation).value();
			}
		}
		if (tipText.length() == 0) {
			Log.warning.println("Param annotation found without proper description " + param.toString());
		}

		if (param.optional()) {
			String defaultValue = param.defaultValue();
			try {
				this.defaultValue = (T) fromString(defaultValue, this.theClass);

				methodName = "default" + 
				    	name.substring(0, 1).toUpperCase() +
				    	name.substring(1)
				    	//  + "List"
				    	;
				try {
					Method defaultValueMethod = beastObject.getClass().getMethod(methodName, theClass);
					// check whether this is a static method
					if (!Modifier.isStatic(defaultValueMethod.getModifiers())) {
						throw new RuntimeException("Programmer error: method " + theClass.getName() + "." +  methodName + "() must be static");
					}
					this.defaultValue = (T) defaultValueMethod.invoke(null);
				} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | 
						InvocationTargetException | IllegalAccessException e) {
					// ignore
				}
				
			} catch (RuntimeException e) {
				if (defaultValue.equals("")) {
					// ignore failure to find default value
					// if the annotation sets it to an empty String.
				} else {
					throw e;
				}
//				e.printStackTrace();
//				throw new IllegalArgumentException(e.getMessage());
			}
		}
		
	}
	
	@Override
	public void setValue(Object value, BEASTInterface beastObject) {
        if (value == null) {
            if (this.value != null) {
                if (this.value instanceof BEASTInterface) {
                    ((BEASTInterface) this.value).getOutputs().remove(beastObject);
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
            	Log.warning.println("Failed to set the string value to '" + value + "' for beastobject id=" + beastObject.getID());
                throw new RuntimeException("Failed to set the string value to '" + value + "' for beastobject id=" + beastObject.getID());
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
                    ((BEASTInterface) value).getOutputs().add(beastObject);
                }
            } else if (value instanceof List<?> && theClass.isAssignableFrom(((List<?>) value).get(0).getClass())) {
                // add all elements in given list to input list.
                for (Object v : ((List<?>) value)) {
                    setValue(v);
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
                setValue(value);
            } else {
            	// check whether this has a primitive type
            	if (theClass.equals(Integer.TYPE) && Integer.class.isAssignableFrom(value.getClass())) {
					setValue(value);
				} else if (theClass.equals(Long.TYPE) && Long.class.isAssignableFrom(value.getClass())) {
					setValue(value);
				} else if (theClass.equals(Short.TYPE) && Short.class.isAssignableFrom(value.getClass())) {
					setValue(value);
				} else if (theClass.equals(Float.TYPE) && Float.class.isAssignableFrom(value.getClass())) {
					setValue(value);
				} else if (theClass.equals(Double.TYPE) && Double.class.isAssignableFrom(value.getClass())) {
					setValue(value);
				} else if (theClass.equals(Boolean.TYPE) && Boolean.class.isAssignableFrom(value.getClass())) {
					setValue(value);
				} else if (theClass.equals(Byte.TYPE) && Byte.class.isAssignableFrom(value.getClass())) {
					setValue(value);
				} else if (theClass.equals(Character.TYPE) && Character.class.isAssignableFrom(value.getClass())) {
					setValue(value);
				} else {
					throw new RuntimeException("Input 102: type mismatch for input " + getName());
				}
            }
        }
	}

	
	private void setValue(Object value) {
		try {
			setter.invoke(beastObject, value);
			if (value instanceof BEASTInterface) {
	              ((BEASTInterface) value).getOutputs().add(BEASTObjectStore.INSTANCE.getBEASTObject(beastObject));
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
     * @param valueString value representation
     * @throws IllegalArgumentException when all conversions fail
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setStringValue(final String valueString) {
        // figure out the type of T and create object based on T=Integer, T=Double, T=Boolean, T=Valuable
        if (value instanceof List<?>) {
            List list = (List) get();
            list.clear();
            // remove start and end spaces
            String valueString2 = valueString.replaceAll("^\\s+", "");
            valueString2 = valueString2.replaceAll("\\s+$", "");
            // split into space-separated bits
            String[] valuesString = valueString2.split("\\s+");
            for (int i = 0; i < valuesString.length; i++) {
                if (theClass.equals(Integer.class)) {
                    list.add(new Integer(valuesString[i % valuesString.length]));
                } else if (theClass.equals(Double.class)) {
                    list.add(new Double(valuesString[i % valuesString.length]));
                } else if (theClass.equals(Boolean.class)) {
                    String str = valuesString[i % valuesString.length].toLowerCase();
                    list.add(str.equals("1") || str.equals("true") || str.equals("yes"));
                } else if (theClass.equals(String.class)) {
                    list.add(new String(valuesString[i % valuesString.length]));
                }
            }
            return;
        }

        if (theClass.equals(Integer.class)) {
            setValue(new Integer(valueString));
            return;
        }
        if (theClass.equals(Double.class)) {
        	setValue(new Double(valueString));
            return;
        }
        if (theClass.equals(Boolean.class)) {
            final String valueString2 = valueString.toLowerCase();
            if (valueString2.equals("yes") || valueString2.equals("true")) {
            	setValue(Boolean.TRUE);
                return;
            } else if (valueString2.equals("no") || valueString2.equals("false")) {
            	setValue(Boolean.FALSE);
                return;
            }
        }
        if (theClass.equals(Function.class)) {
        	final Function.Constant param = new Function.Constant(valueString);
        	setValue(param);
            param.getOutputs().add(BEASTObjectStore.INSTANCE.getBEASTObject(beastObject));
            return;
        }

        if (theClass.isEnum()) {
        	if (possibleValues == null) {
        		possibleValues = (T[]) theClass.getDeclaringClass().getEnumConstants();
        	}
            for (final T t : possibleValues) {
                if (valueString.equals(t.toString())) {
                	setValue(t);
                    return;
                }
            }
            throw new IllegalArgumentException("Input 104: value " + valueString + " not found. Select one of " + Arrays.toString(possibleValues));
        }

        // call a string constructor of theClass
        try {
            Constructor ctor;
            Object v = valueString;
            try {
            	ctor = theClass.getDeclaredConstructor(String.class);
            } catch (NoSuchMethodException e) {
            	// we get here if there is not String constructor
            	// try integer constructor instead
            	try {
            		if (valueString.startsWith("0x")) {
            			v = Integer.parseInt(valueString.substring(2), 16);
            		} else {
            			v = Integer.parseInt(valueString);
            		}
                	ctor = theClass.getDeclaredConstructor(int.class);
                	
            	} catch (NumberFormatException e2) {
                	// could not parse as integer, try double instead
            		v = Double.parseDouble(valueString);
                	ctor = theClass.getDeclaredConstructor(double.class);
            	}
            }
            ctor.setAccessible(true);
            final Object o = ctor.newInstance(v);
            setValue(o);
            if (o instanceof BEASTInterface) {
                ((BEASTInterface) o).getOutputs().add(BEASTObjectStore.INSTANCE.getBEASTObject(beastObject));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Input 103: type mismatch, cannot initialize input '" + getName() +
                    "' with value '" + valueString + "'.\nExpected something of type " + getType().getName() +
                    ".\n" + (e.getMessage() != null ? e.getMessage() : ""));
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

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InputForAnnotatedConstructor)) {
			return false;
		}
		InputForAnnotatedConstructor other = (InputForAnnotatedConstructor) obj;
		return other.getName().equals(getName()) &&
				other.tipText.equals(tipText) &&
				other.getType().equals(getType());
	}
}
