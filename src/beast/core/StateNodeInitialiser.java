package beast.core;

import java.util.List;

/**
 * Typically, StateNodes are initialised through their inputs. However, there
 * are initialisation scenarios that are too complex for initialisation. For
 * example, when initialising a gene tree that needs to fit inside a species tree,
 * or a parameter associated with a node in a tree from the meta-data in Newick
 * format.
 * <p/>
 * StateNodeInitialisers take one or more StateNodes as input and initStateNodes
 * is called just once to set up the start state of a chain.
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
     */
    List<StateNode> getInitialisedStateNodes();
}
