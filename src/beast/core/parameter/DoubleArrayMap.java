package beast.core.parameter;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;


@Description("Maps names to list of doubles, e.g. taxon names to longitude/latitude pairs for locations")
public class DoubleArrayMap extends Map<List<Double>> {
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
//			List<Double> list = defaultInput.get().get(name);
//			list.add((Double) value);
//		} else {
//			List<Double> list = new ArrayList<Double>();
//			list.add((Double) value);
//			defaultInput.get().put(name, list);
//		}
//	};

}
