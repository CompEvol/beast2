package beast.evolution.likelihood;

/**
 * @author Marc Suchard
 * @author Andrew Rambaut
 */
public enum PartialsRescalingScheme2 {

    DEFAULT("default"),
    NONE("none"),
    DYNAMIC("dynamic"),
    AUTO("auto"),
    KICK_ASS("kickAss"),
    ALWAYS("always");

    PartialsRescalingScheme2(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    private final String text;

    public static PartialsRescalingScheme2 parseFromString(String text) {
        for(PartialsRescalingScheme2 scheme : PartialsRescalingScheme2.values()) {
            if (scheme.getText().compareToIgnoreCase(text) == 0)
                return scheme;
        }
        return DEFAULT;
    }

}
