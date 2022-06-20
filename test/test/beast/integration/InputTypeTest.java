package test.beast.integration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import beast.base.core.BEASTInterface;
import beast.base.core.BEASTObject;
import beast.base.core.Input;
import beast.base.core.Param;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InputTypeTest  {

	{
		System.setProperty("beast.is.junit.testing", "true");
	}

	/*
	 * Test that the type of an input can be determined, If not, the programmer
	 * has to manually initialise the type of the input (most easily done
	 * through a constructor of Input)
	 */
	@Test
	public void testInputTypeCanBeSet() throws Exception {
		final Set<String> beastObjectNames = PackageManager.listServices("beast.base.core.BEASTInterface");
		List<String> failingInputs = new ArrayList<String>();
		for (String beastObjectName : beastObjectNames) {
			try {
				BEASTObject beastObject = (BEASTObject) BEASTClassLoader.forName(beastObjectName).newInstance();
				List<Input<?>> inputs = beastObject.listInputs();
				for (Input<?> input : inputs) {
					if (input.getType() == null) {
						try {
							input.determineClass(beastObject);
							if (input.getType() == null) {
								failingInputs.add(beastObject + ":" + input.getName());
							}
						} catch (Exception e2) {
							failingInputs.add(beastObject + ":" + input.getName());
						}
					}
				}
			} catch (Exception e) {
				// ignore
			}
		}

		assertTrue(failingInputs.size() == 0,
				"Type of input could not be set for these inputs (probably requires to be set by using the appropriate constructure of Input): "
						+ failingInputs.toString());
	}

	@Test
	public void testAnnotatedInputHasGetters() throws Exception {
		final Set<String> beastObjectNames = PackageManager.listServices("beast.base.core.BEASTInterface");
		System.err.println("Testing " + beastObjectNames.size() + " classes");
		List<String> failingInputs = new ArrayList<String>();
		for (String beastObject : beastObjectNames) {
			try {
				Class<?> _class = BEASTClassLoader.forName(beastObject);
			    Constructor<?>[] allConstructors = _class.getDeclaredConstructors();
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
			    	if (paramAnnotations.size() > 0 && !BEASTInterface.class.isAssignableFrom(_class)) {
			    		failingInputs.add(_class.getName() + " has Param annotations but does not implement BEASTInterface\n");
			    	}
			    	Class<?>[] types  = ctor.getParameterTypes();	    	
		    		Type[] gtypes = ctor.getGenericParameterTypes();
			    	if (types.length > 0 && paramAnnotations.size() > 0) {
			    		int offset = 0;
			    		if (types.length == paramAnnotations.size() + 1) {
			    			offset = 1;
			    		}
			    		for (int i = 0; i < paramAnnotations.size(); i++) {
			    			Class<?> type;
			    			Class<?> clazz = null;
			    			boolean isList = false;
							String typeName = types[i + offset].getTypeName();
							if (typeName.endsWith("[]")) {
								failingInputs.add(_class.getName() + " constructor has arrray as argument, should be a List\n");
							} else {
								switch (typeName) {
								case "int" : type = Integer.class; break;
								case "long" : type = Long.class; break;
								case "float" : type = Float.class; break;
								case "double" : type = Double.class; break;
								case "boolean" : type = Boolean.class; break;
								default:
									clazz = BEASTClassLoader.forName(typeName);
					    			if (clazz.isAssignableFrom(List.class)) {
				                        Type[] genericTypes2 = ((ParameterizedType) gtypes[i + offset]).getActualTypeArguments();
				                        type = (Class<?>) genericTypes2[0];
				                        isList = true;
					    			} else {
					    				type =  types[i + offset];
					    			}
								}
				    			
						    	String name = paramAnnotations.get(i).name();
						    	String getter = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
						    	try {
						    		Method method = _class.getMethod(getter);
						    		if (isList) {
						    			if (!method.getReturnType().isAssignableFrom(List.class)) {
											failingInputs.add(_class.getName() + ":" + getter + "() should return List<" + type.getName() + "> instead of " + method.getReturnType().getName() + "\n");
						    			}
						    		} else if (!method.getReturnType().isAssignableFrom(type)) {
										failingInputs.add(_class.getName() + ":" + getter + "() should return " + type.getName() + " instead of " + method.getReturnType().getName() + "\n");
						    		}
						    	} catch (NoSuchMethodException e) {
									failingInputs.add(_class.getName() + ":" + getter + "() missing\n");
						    	}
						    	String setter = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
						    	try {
						    		_class.getMethod(setter, type);
						    	} catch (NoSuchMethodException e) {
									failingInputs.add(_class.getName() + ":" + setter + "("+ type.getName() + ") missing, or does not have correct argument\n");
						    	}
							}
			    		}
			    	}
				}
			} catch (Exception e) {
				System.err.println(beastObject + " " + e.getClass().getName() + " " + e.getMessage());
			}
		}
		System.err.println("Done!");

		assertTrue(failingInputs.size() == 0,
				"Something is wrong with these annotated constructor(s): \n"
						+ failingInputs.toString());
	}
}
