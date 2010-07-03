package beast.core;

import java.util.List;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public interface Loggable {
    
    List<String> getLabels();

    List<Object> log(int sample, State state);
}
