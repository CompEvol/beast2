package beast.core.parameter;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.lang.String;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;




@Description("Unordered set mapping keys to values")
abstract public class Map<T> extends CalculationNode {	
	
	public Input<java.util.Map<java.lang.String, T>> defaultInput = new Input<java.util.Map<java.lang.String, T>>("*",
			"Input containing the map", new HashMap<java.lang.String, T>());
	
	public java.util.Map<java.lang.String, T> map;

	public Map() {
		// set up type of default input, since it cannot be discovered through introspection
		defaultInput.setType(mapType());
	}
	
	abstract protected Class<?> mapType();
	
	@Override
	public void initAndValidate() throws Exception {
		map = defaultInput.get();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void setInputValue(java.lang.String name, Object value) throws Exception {
		for (Input<?> input : listInputs()) {
			if (input != defaultInput && input.getName().equals(name)) {
				input.setValue(value, this);
				return;
			}
		}
		map = defaultInput.get();
		if (defaultInput.getType().equals(List.class)) {
			if (defaultInput.get().containsKey(name)) {
				List list = (List) defaultInput.get().get(name);
				list.add(value);
			} else {
				List list = new ArrayList();
				list.add(value);
				defaultInput.get().put(name, (T) list);
			}
			
		} else {
			defaultInput.get().put(name, (T) value);
		}
	};

	@Override
	public Input<?> getInput(java.lang.String name) throws Exception {
		for (Input<?> input : listInputs()) {
			if (input != defaultInput && input.getName().equals(name)) {
				return input;
			}
		}
		return defaultInput;
	}
	
	/** some utility methods **/
	
	public T get(String key) {
		return map.get(key); 
	}
	
	public boolean contains(String key) {
		return map.containsKey(key);
	}

	public T remove(String key) {
		return map.remove(key);
	}
	
}
