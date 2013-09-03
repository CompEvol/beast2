package beast.core.parameter;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;


@Description("Maps names to list of integers, e.g. taxon names to a sequence")
public class IntArrayMap extends Map<List<Integer>>{
	protected Class<?> mapType() {return List.class;}
	
//	@Override
//	public void setInputValue(java.lang.String name, Object value) throws Exception {
//		for (Input<?> input : listInputs()) {
//			if (input != defaultInput && input.getName().equals(name)) {
//				input.setValue(value, this);
//				return;
//			}
//		}
//		if (defaultInput.get().containsKey(name)) {
//			List<Integer> list = defaultInput.get().get(name);
//			list.add((Integer) value);
//		} else {
//			List<Integer> list = new ArrayList<Integer>();
//			list.add((Integer) value);
//			defaultInput.get().put(name, list);
//		}
//	};
}
