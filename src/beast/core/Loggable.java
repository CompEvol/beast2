package beast.core;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public interface Loggable {
    void log(int sample, State state);
}
