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
     *
     * This is called prior to the proposal of a new state
     **/
    void store(int sample);

    /** reverse of store
     * This is called when a proposal is rejected
     **/
    void restore(int sample);

    /**
     * Tell an instance to prepare for the evaluation of the state by extracting required
     * information from the state. Some of the nodes in state may have been changed by the
     * operators or by the restoration of the previous state by the rejection of a proposal.
     *
     * This is called prior to the evaluation of a proposed state.
     * @param state
     */
    void prepare(State state);
}
