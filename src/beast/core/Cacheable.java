package beast.core;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public interface Cacheable {
    /** Store internal state.
     * Used by MCMC algorithm on its all objects that implement this to make sure
     * their internal state are stored so that if a proposal is rejected
     * the object can return to its last known good state.
     **/
    void store(int sample);

    /** reverse of store **/
    void restore(int sample);

}
