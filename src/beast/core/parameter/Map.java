package beast.core.parameter;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    public void setInputValue(java.lang.String name, Object value) {
        try {
            for (Input<?> input : listInputs()) {
                if (input != defaultInput && input.getName().equals(name)) {
                    input.setValue(value, this);
                    return;
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to set input named '" + name + "' with value '" + value + "'");
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
    }

    ;

    @Override
    public Input<?> getInput(java.lang.String name) {
        try {
            for (Input<?> input : listInputs()) {
                if (input != defaultInput && input.getName().equals(name)) {
                    return input;
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException("Failed to get input named '" + name + "'");
        }
        return defaultInput;
    }

    /**
     * some utility methods *
     */

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
