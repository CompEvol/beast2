package test.beast.beast2vs1.trace;

/**
 * @author Alexei Drummond
 * @author Walter Xie
 */
public enum TraceType {
    DOUBLE("double", "D", Double.class),
    INTEGER("integer", "I", Integer.class),
    STRING("string", "S", String.class);

    TraceType(String name, String brief, Class<?> type) {
        this.name = name;
        this.brief = brief;
        this.type = type;
    }

    @Override
	public String toString() {
        return name;
    }

    public String getBrief() {
        return brief;
    }

    public Class<?> getType() {
        return type;
    }

    private final String name;
    private final String brief;
    private final Class<?> type;
}
