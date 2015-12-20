package beast.util;

/** helper class that shares common properties of Inputs and Param annotations **/
class InputType {

	/** name of Input or Param annotation **/
	String name;
	
	/** type of Input or Param annotation **/
	Class<?> type;
	
	/** whether this is Input or Param annotation **/
	boolean isInput;
	
	/** default value when no value is specified, if any **/
	Object defaultValue;
	
	/** c'tor **/
	public InputType(String name, Class<?> type, boolean isInput, Object defaultValue) {
		this.name = name;
		this.type = type;
		this.isInput = isInput;
		this.defaultValue = defaultValue;
	}

	
	/** getters & setters **/
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}
	
	public boolean isInput() {
		return isInput;
	}

	public void setInput(boolean isInput) {
		this.isInput = isInput;
	}


	public Object getDefaultValue() {
		return defaultValue;
	}


	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public String toString() {
		return getName() + " " + getType().getName() + " " + isInput() + " " + getDefaultValue();
	}
}
