package beast.core;

import java.util.List;

/**
 * Typically, StateNodes are initialised through their inputs. However, there are initialisation scenarios that are
 * too complex for initialisation. For example, when initialising a gene tree that needs to fit inside a species
 * tree, or a parameter associated with a node in a tree from the meta-data in Newick format.
 * <p/>
 * StateNodeInitialisers take one or more StateNodes as input, initializes them in initStateNodes() and pass them
 * back as the return value of getInitialisedStateNodes() to inform the "system" which nodes has been initialized.
 *
 * initAndValidate() and initStateNodes() are called just once, but getInitialisedStateNodes() may be called multiple
 * times as its inputs change while the system tries to establish a valid starting state.
 *
 *  initAndValidate is executed in order that the XML parser see objects, so the inputs are not guaranteed to be initialized at this time.
 * initStateNodes is executed in order of appearance in MCMC, so inputs requiring initialization are properly
 * initialized when  initStateNodes is called.
 *
 * @author remco
 */
public interface StateNodeInitialiser {

    /**
     * called just once to set up start state *
     */
    void initStateNodes() throws Exception;

    /**
     * @return list of StateNodes that are initialised
     *         This information is used to ensure StateNode are not
     *         initialised more than once.
     * @param stateNodes1
     */
    void getInitialisedStateNodes(List<StateNode> stateNodes1);
}
