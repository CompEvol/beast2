package test.beast.integration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.Policy.Parameters;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import beast.core.BEASTObject;
import beast.core.Input;
import beast.core.InputForAnnotatedConstructor;
import beast.core.Param;
import beast.util.AddOnManager;
import junit.framework.TestCase;

public class InputTypeTest extends TestCase {

	/*
	 * Test that the type of an input can be determined, If not, the programmer
	 * has to manually initialise the type of the input (most easily done
	 * through a constructor of Input)
	 */
	@Test
	public void testInputTypeCanBeSet() throws Exception {
		List<String> beastObjectNames = AddOnManager.find(beast.core.BEASTObject.class,
				AddOnManager.IMPLEMENTATION_DIR);
		List<String> failingInputs = new ArrayList<String>();
		for (String beastObject : beastObjectNames) {
			try {
				BEASTObject plugin = (BEASTObject) Class.forName(beastObject).newInstance();
				List<Input<?>> inputs = plugin.listInputs();
				for (Input<?> input : inputs) {
					if (input.getType() == null) {
						try {
							input.determineClass(plugin);
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

		assertTrue(
				"Type of input could not be set for these inputs (probably requires to be set by using the appropriate constructure of Input): "
						+ failingInputs.toString(),
				failingInputs.size() == 0);
	}

	@Test
	public void testAnnotatedInputHasGetters() throws Exception {
		testAnnotatedInputHasGetters(AddOnManager.IMPLEMENTATION_DIR);
	}
	
	public void testAnnotatedInputHasGetters(String [] packages) throws Exception {
		List<String> beastObjectNames = AddOnManager.find(beast.core.BEASTObject.class, packages);
		System.err.println("Testing " + beastObjectNames.size() + " classes");
		List<String> failingInputs = new ArrayList<String>();
		for (String beastObject : beastObjectNames) {
				Class<?> _class = Class.forName(beastObject);
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
							try {
								clazz = Class.forName(types[i + offset].getTypeName());
							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			    			if (clazz.isAssignableFrom(List.class)) {
		                        Type[] genericTypes2 = ((ParameterizedType) gtypes[i + offset]).getActualTypeArguments();
		                        type = (Class<?>) genericTypes2[0];
			    			} else {
			    				type =  types[i + offset];
			    			}
			    			
					    	String name = paramAnnotations.get(i).name();
					    	String getter = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
					    	try {
					    		Method method = _class.getMethod(getter);
					    		if (!method.getReturnType().isAssignableFrom(type)) {
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
		System.err.println("Done!");

		assertTrue(
				"Type of input could not be set for these inputs (probably requires to be set by using the appropriate constructure of Input): "
						+ failingInputs.toString(),
				failingInputs.size() == 0);
	}
}
