package beast.base;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import beast.pkgmgmt.BEASTClassLoader;


/** 
 * database of all beast objects -- this allows management of VirtualBEASTObjects
 * and thus instances of classes that do not implement the BEASTInterface interface. 
 * **/
public class BEASTObjectStore {
	/** singular instance of the store **/
	public static BEASTObjectStore INSTANCE = new BEASTObjectStore();
	
	Map<Object, VirtualBEASTObject> objectStore;
	
	private BEASTObjectStore() {
		objectStore = new LinkedHashMap<>();
	}
	
	/** add object to the store and return VirtualBEASTObject for the object **/
	public BEASTObject addObject(Object o) {
		if (o instanceof BEASTObject) {
			return (BEASTObject) o;
		}
		VirtualBEASTObject vbo = new VirtualBEASTObject(o);
		objectStore.put(o, vbo);
		return vbo;
	}
	
	/** check whether an object has @Param annotations **/
	public static boolean hasAnnotations(Object o) {
	    Constructor<?>[] allConstructors = o.getClass().getDeclaredConstructors();
	    boolean hasID = false;
	    boolean hasName = false;
	    for (Constructor<?> ctor : allConstructors) {
	    	Annotation[][] annotations = ctor.getParameterAnnotations();
	    	List<Param> paramAnnotations = new ArrayList<>();
	    	int optionals = 0;
	    	for (Annotation [] a0 : annotations) {
		    	for (Annotation a : a0) {
		    		if (a instanceof Param) {
		    			return true;
		    		}
	    		}
	    	}
	    }
		return false;
	}

	/** get (Virtual)BEASTObject from Object Store 
	 *  add the object to the store if it is not already there 
	 **/
	public BEASTObject getBEASTObject(Object o) {
		BEASTObject vbo = objectStore.get(o);
		if (vbo == null) {
			vbo = addObject(o);
		}
		return vbo;
	}
	
	public static List<BEASTInterface> listActiveBEASTObjects(Object beastObject) {
		if (!(beastObject instanceof BEASTInterface)) {
			beastObject = INSTANCE.getBEASTObject(beastObject);
		}
		return ((BEASTInterface) beastObject).listActiveBEASTObjects();
	}

	public static List<Input<?>> listInputs(Object beastObject) {
		if (!(beastObject instanceof BEASTInterface)) {
			beastObject = INSTANCE.getBEASTObject(beastObject);
		}
		return ((BEASTInterface) beastObject).listInputs();
	}

	public static String getId(Object beastObject) {
		if (!(beastObject instanceof BEASTInterface)) {
			beastObject = INSTANCE.getBEASTObject(beastObject);
		}
		return ((BEASTInterface) beastObject).getID();
	}

	public static void setId(Object beastObject, String id) {
		if (!(beastObject instanceof BEASTInterface)) {
			beastObject = INSTANCE.getBEASTObject(beastObject);
		}
		((BEASTInterface) beastObject).setID(id);
	}

	/** return true if object is primitive object, 
	 * i.e., not in the object store and has no description 
	 * **/
	public static boolean isPrimitive(Object value) {			
		// The value is primitive if there are no @Param annotations, and 
		// no newInstance() method and has no @Description annotation. 
		// Any primitive object (int, short, boolean, etc) is certainly primitive.
		if (value.getClass().isPrimitive() || Number.class.isAssignableFrom(value.getClass()) 
				|| value.getClass() == Boolean.class || value.getClass() == String.class
				|| value.getClass().isEnum()) {
			return true;
		}
		
		List<Input<?>> inputs = INSTANCE.getBEASTObject(value).listInputs();
		if (inputs.size() != 0) {
			return false;
		}
		
		if (!INSTANCE.getBEASTObject(value).getDescription().equals(BEASTInterface.DEFEAULT_DESCRIPTION)) {
			return false;
		}
		
		return true;
	}
	
	/** return true if type name is for a primitive type, 
	 * i.e., not in the object store and has no description and no @Param annotations 
	 * **/
	public static boolean isPrimitiveType(String typeName) {
		if (typeName.equals("int") || typeName.equals("long") || typeName.equals("short") || 
				typeName.equals("char") || typeName.equals("boolean") || typeName.equals("byte") || 
				typeName.equals("double") || typeName.equals("float")) {
			return true;
		}
		try {
			Class clazz = BEASTClassLoader.forName(typeName);

			if (clazz.isPrimitive()) {
				return true;
			}
			
			// has @Param annotation
		    Constructor<?>[] allConstructors = clazz.getDeclaredConstructors();
		    for (Constructor<?> ctor : allConstructors) {
		    	Annotation[][] annotations = ctor.getParameterAnnotations();
		    	for (Annotation [] a0 : annotations) {
			    	for (Annotation a : a0) {
			    		if (a instanceof Param) {
			    			return false;
			    		}
		    		}
		    	}	    	
		    }
		    
			// has @Description annotation
	        final Annotation[] classAnnotations = clazz.getAnnotations();
	        for (final Annotation annotation : classAnnotations) {
	            if (annotation instanceof Description) {
	                return false;
	            }
	        }		
			
			return true;
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Incorrect type name: " + e);
		}

	}

	/** Get the name of the class of object
	 * For VirtualBEASTObjects, return name of encapsulated object
	 */
	public static String getClassName(Object beastObject) {
		if (beastObject instanceof VirtualBEASTObject) {
			return ((VirtualBEASTObject) beastObject).getClassName();
		}
		return beastObject.getClass().getName();
	}

	

}
