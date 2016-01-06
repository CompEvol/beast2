package beast.core;

import java.util.List;

/**
 * Typically, StateNodes are initialised through their inputs. However, there are initialisation scenarios
 * too complex for this approach to work. For example, initialisation may require additional information not
 * provided by the inputs, or several dependent beastObjects need to initialise together,
 * such as gene trees and a species tree.
 * <p/>
 * StateNodeInitialisers take one or more StateNodes as input and  initializes them in initStateNodes().
 * getInitialisedStateNodes() reports back which nodes has been initialized, but this is currently only used to
 * check for multiple initialiser for the same object.
 *  <p/>
 * Like any other iBEASTObject, a state initialiser must have an initAndValidate(), which is called once.
 * getInitialisedStateNodes(), on the other hand,  may be called multiple times as its inputs change while the system
 * tries to establish a valid starting state. initAndValidate is executed in order that the XML parser see objects,
 * so the inputs are not guaranteed to be initialized at this time. initStateNodes is executed in order of appearance
 * in MCMC, so inputs requiring initialization are properly initialized when initStateNodes is called.
 *
 * @author remco
 */
public interface StateNodeInitialiser {

    /**
     * Called to set up start state. May be called multiple times. *
     */
    void initStateNodes() throws Exception;

    /**
     * @return list of StateNodes that are initialised
     *         This information is used to ensure StateNode are not initialised more than once.
     * @param stateNodes
     */
    void getInitialisedStateNodes(List<StateNode> stateNodes);
}
