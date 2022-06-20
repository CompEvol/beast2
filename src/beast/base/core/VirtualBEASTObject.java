package beast.base.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Description("BEAST Object that encapsulates an object that does not implement BEASTInterface")
public class VirtualBEASTObject extends BEASTObject {
	Object o;

	public VirtualBEASTObject(Object o) {
		this.o = o;
		// this initialises inputs
		getInputs();
	}

	public Object getObject() {
		return o;
	}
	
	public String getClassName() {
		return o.getClass().getName();
	}

//	@Override
//	public String getId() {
//		String id;
//		try {
//			Method getter = o.getClass().getMethod("getId");
//			id = (String) getter.invoke(o);
//		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
//			Log.trace.println("Method getId() could not be found on " + o.getClass().getName());			
//			id = super.getId();
//		}
//		return id;
//	}
//	
//	@Override
//	public void setId(String ID) {
//		try {
//			Method setter = o.getClass().getMethod("setId", String.class);
//			setter.invoke(o, ID);
//		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
//			Log.trace.println("Method setId() could not be found on " + o.getClass().getName());
//		}
//		super.setId(ID);
//	}
	

	@Override
	public void initAndValidate() {
		try {
			Method initAndValidate = o.getClass().getMethod("initAndValidate");
			initAndValidate.invoke(o);
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
//			e.printStackTrace();
//			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Input<?>> listInputs() {
        final List<Input<?>> inputs = new ArrayList<>();        
        Map<String, Input> inputNames = new LinkedHashMap<>();
        listAnnotatedInputs(o, inputs, inputNames);
		return inputs; 
	}

	@Override
	public String getDescription() {
        final Annotation[] classAnnotations = o.getClass().getAnnotations();
        for (final Annotation annotation : classAnnotations) {
            if (annotation instanceof Description) {
                final Description description = (Description) annotation;
                return description.value();
            }
        }		
		return super.getDescription();
	}

	
	@Override
	public List<BEASTInterface> listActiveBEASTObjects() {
        final List<BEASTInterface> beastObjects = new ArrayList<>();

        for (Input<?> input : getInputs().values()) {
        	if (input.get() != null) {
        		if (input.get() instanceof List<?>) {
        			final List<?> list = (List<?>) input.get();
        			for (final Object o : list) {
        				if (!BEASTObjectStore.isPrimitive(o)) {
        					beastObjects.add(BEASTObjectStore.INSTANCE.getBEASTObject(o));
        				}
        			}
        		} else if (input.get() != null && input.get() instanceof BEASTInterface) {
        			beastObjects.add((BEASTInterface) input.get());
        		}
        	}
        }
        return beastObjects;
	}
}
