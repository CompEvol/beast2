/*
* File State.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is part of BEAST2.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/
package beast.core;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import beast.core.util.Log;




@Description("The state represents the current point in the state space, and " +
        "maintains values of a set of StateNodes, such as parameters and trees. " +
        "Furthermore, the state manages which parts of the model need to be stored/restored " +
        "and notified that recalculation is appropriate.")
public class State extends BEASTObject {

    public final Input<List<StateNode>> stateNodeInput =
            new Input<>("stateNode", "anything that is part of the state", new ArrayList<>());
    final public Input<Integer> m_storeEvery =
            new Input<>("storeEvery", "store the state to disk every X number of samples so that we can " +
                    "resume computation later on if the process failed half-way.", -1);
//    public Input<Boolean> m_checkPoint =
//            new Input<>("checkpoint", "keep saved states (every X samples).", false);

    /**
     * The components of the state, for instance tree & parameters.
     * This represents the current state, but a copy is kept so that when
     * an operation is applied to the State but the proposal is not accepted,
     * the state can be restored. This is currently implemented by having
     * Operators call getEditableStateNode() at which point the requested
     * StateNode is copied.
     * Access through getNrStatNodes() and getStateNode(.).
     */
    protected StateNode[] stateNode;

    /**
     * number of state nodes *
     */
    private int nrOfStateNodes;

    public int getNrOfStateNodes() {
        return nrOfStateNodes;
    }

    /**
     * pointers to memory allocated to stateNodes and storedStateNodes *
     */
    private StateNode[] stateNodeMem;

    /**
     * File name used for storing the state, either periodically or at the end of an MCMC chain
     * so that the chain can be resumed.
     */
    private String stateFileName = "state.backup.xml";


    /** The following members are involved in calculating the set of
     * CalculatioNodes that need to be notified when an operation
     * has been applied to the State. The Calculation nodes are then
     * store/restore/accepted/check dirtiness in partial order.
     */

    /**
     * Maps a BEASTObject to a list of Outputs.
     * This map only contains those plug-ins that have a path to the posterior *
     */
    private HashMap<BEASTInterface, List<BEASTInterface>> outputMap;

    /**
     * Returns a list of BEAST objects
     * @return
     */
    public List<BEASTInterface> getOutputs(BEASTInterface beastInterface) {
        return outputMap.get(beastInterface);
    }

    /**
     * Same as m_outputMap, but only for StateNodes indexed by the StateNode number
     * We need this since the StateNode changes regularly, so unlike the output map
     * for BEASTObjects cannot be accessed by the current StateNode as key.
     */
    private List<CalculationNode>[] stateNodeOutputs;

    /**
     * Code that represents configuration of StateNodes that have changed
     * during an operation.
     * <p/>
     * Every time an operation requests a StateNode, an entry is added to changeStateNodes
     * changeStateNodes records how many StateNodes are changed.
     * The code is reset when the state is stored, and every time a StateNode
     * is requested by an operator, changeStateNodes is updated.
     */
    private int[] changeStateNodes;
    private int nrOfChangedStateNodes;

    /**
     * Maps the changed states node code to
     * the set of calculation nodes that is potentially affected by an operation *
     */
    Trie trie;

    /**
     * class for quickly finding which calculation nodes need to be updated
     * due to state-node changes
     */
    class Trie {
        List<CalculationNode> list;
        final Trie[] children;

        Trie() {
            children = new Trie[stateNode.length];
        }

        /**
         * get entry from Trie, return null if no entry is present yet *
         * @param pos
         */
        List<CalculationNode> get(final int pos) {
            if (pos == 0) {
                return list;
            }
            final Trie child = children[changeStateNodes[pos - 1]];
            if (child == null) {
                return null;
            }
            return child.get(pos - 1);
        }

        /**
         * set entry int Trie, create new entries if no entry is present yet *
         */
        void set(final List<CalculationNode> list, final int pos) {
            if (pos == 0) {
                this.list = list;
                return;
            }
            Trie child = children[changeStateNodes[pos - 1]];
            if (child == null) {
                child = new Trie();
                children[changeStateNodes[pos - 1]] = child;
            }
            child.set(list, pos - 1);
        }
    }


    @Override
    public void initAndValidate() {
    }

    public void initialise() {
        stateNode = stateNodeInput.get().toArray(new StateNode[0]);

        for (int i = 0; i < stateNode.length; i++) {
            stateNode[i].index = i;
        }
        // make itself known
        for (StateNode state : stateNode) {
            state.state = this;
        }

        nrOfStateNodes = stateNode.length;
        // allocate memory for StateNodes and a copy.
        stateNodeMem = new StateNode[nrOfStateNodes * 2];
        for (int i = 0; i < nrOfStateNodes; i++) {
            stateNodeMem[i] = stateNode[i];
            stateNodeMem[nrOfStateNodes + i] = stateNodeMem[i].copy();
        }

        // set up data structure for encoding which StateNodes change by an operation
        changeStateNodes = new int[stateNode.length];
        //Arrays.fill(changeStateNodes, -1);
        nrOfChangedStateNodes = 0;
        trie = new Trie();
        // add the empty list for the case none of the StateNodes have changed
        trie.list = new ArrayList<>();
    } // initAndValidate


    /**
     * return currently valid state node. This is typically called from a
     * CalculationNode for inspecting the value of a StateNode, not for
     * changing it. To change a StateNode, say from an Operator,
     * getEditableStateNode() should be called. *
     */
    public StateNode getStateNode(final int _id) {
        return stateNode[_id];
    }

    /**
     * Return StateNode that can be changed, but later restored
     * if necessary. If there is no copy stored already, a copy is
     * made first, and the StateNode is marked as being dirty.
     * <p/>
     * NB This should only be called from an Operator that wants to
     * change the particular StateNode through the Input.get(Operator)
     * method on the input associated with this StateNode.
     */
    protected StateNode getEditableStateNode(int _id, Operator operator) {
        for (int i = 0; i < nrOfChangedStateNodes; i++) {
            if (changeStateNodes[i] == _id) {
                return stateNode[_id];
            }
        }
        changeStateNodes[nrOfChangedStateNodes++] = _id;
        return stateNode[_id];
    }

    /**
     * Store a State before applying an operation proposal to the state.
     * This copies the state for possible later restoration
     * but does not affect any inputs, which are all still connected
     * to the original StateNodes
     * <p/>
     * Also, store the state to disk for resumption of analysis later on.
     *
     * @param sample chain state number
     * @return  true if stored  to disk
     */
    public void store(final long sample) {
        //Arrays.fill(changeStateNodes, -1);
        nrOfChangedStateNodes = 0;
    }

    /**
     * Restore a State after rejecting the operation proposal.
     * This assigns the state to the stored state.
     * NB this does not affect any Inputs connected to any stateNode. *
     */
    public void restore() {
        for (int i = 0; i < nrOfChangedStateNodes; i++) {
            stateNode[changeStateNodes[i]].restoreStateNode();
        }
    }

    /**
     * Visit all calculation nodes in partial order determined by the BEASTObject-input relations
     * (i.e. if A is input of B then A < B). There are 4 operations that can be propagated this
     * way:
     * <p/>
     * store() makes sure all calculation nodes store their internal state
     * <p/>
     * checkDirtiness() makes all calculation nodes check whether they give a different answer
     * when interrogated by one of its outputs
     * <p/>
     * accept() allows all calculation nodes to mark themselves as being clean without further
     * calculation
     * <p/>
     * restore() if a proposed state is not accepted, all calculation nodes need to restore
     * themselves
     */
    public void storeCalculationNodes() {
        final List<CalculationNode> currentSetOfCalculationNodes = getCurrentCalculationNodes();
        for (final CalculationNode calculationNode : currentSetOfCalculationNodes) {
            calculationNode.store();
        }
    }

    public void checkCalculationNodesDirtiness() {
        final List<CalculationNode> currentSetOfCalculationNodes = getCurrentCalculationNodes();
        for (final CalculationNode calculationNode : currentSetOfCalculationNodes) {
            calculationNode.checkDirtiness();
        }
    }

    public void restoreCalculationNodes() {
        final List<CalculationNode> currentSetOfCalculationNodes = getCurrentCalculationNodes();
        for (final CalculationNode calculationNode : currentSetOfCalculationNodes) {
            calculationNode.restore();
        }
    }

    public void acceptCalculationNodes() {
        final List<CalculationNode> currentSetOfCalculationNodes = getCurrentCalculationNodes();
        for (final CalculationNode calculationNode : currentSetOfCalculationNodes) {
            calculationNode.accept();
        }
    }

    /**
     * set name of state file, used when storing/restoring the state to disk *
     */
    public void setStateFileName(final String fileName) {
        if (fileName != null) {
            stateFileName = fileName;
        }
    }

    /**
     * Print state to file. This is called either periodically or at the end
     * of an MCMC chain, so that the state can be resumed later on.
     *
     * @param sample TODO
     */
    public void storeToFile(final long sample) {
        try {
            PrintStream out = new PrintStream(stateFileName + ".new");
            out.print(toXML(sample));
            //out.print(new XMLProducer().toXML(this));
            out.close();
            File newStateFile = new File(stateFileName + ".new");
            File oldStateFile = new File(stateFileName);
            oldStateFile.delete();
            // newStateFile.renameTo(oldStateFile); -- unstable under windows
            Files.move(newStateFile.toPath(), oldStateFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * convert state to XML string,
     * The state can be reconstructed using the fromXML() method
     *
     * @param sample TODO*
     */
    public String toXML(final long sample) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<itsabeastystatewerein version='2.0' sample='").append(sample).append("'>\n");
        for (final StateNode node : stateNode) {
            buf.append(node.toXML());
        }
        buf.append("</itsabeastystatewerein>\n");
        return buf.toString();
    }

    /**
     * Restore state from an XML fragment *
     */
    public void fromXML(final String xml) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes()));
            doc.normalize();
            final NodeList nodes = doc.getElementsByTagName("*");
            final Node topNode = nodes.item(0);
            final NodeList children = topNode.getChildNodes();
            for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                final Node child = children.item(childIndex);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    final String id = child.getAttributes().getNamedItem("id").getNodeValue();
                    int stateNodeIndex = 0;
                    while (!stateNode[stateNodeIndex].getID().equals(id)) {
                        stateNodeIndex++;
                    }
                    final StateNode stateNode2 = stateNode[stateNodeIndex].copy();
                    stateNode2.fromXML(child);
                    stateNode[stateNodeIndex].assignFromFragile(stateNode2);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * restore a state from file for resuming an MCMC chain 
     * @throws ParserConfigurationException 
     * @throws IOException 
     * @throws SAXException *
     */
    public void restoreFromFile() throws SAXException, IOException, ParserConfigurationException  {
        Log.info.println("Restoring from file");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new File(stateFileName));
        doc.normalize();
        final NodeList nodes = doc.getElementsByTagName("*");
        final Node topNode = nodes.item(0);
        final NodeList children = topNode.getChildNodes();
        for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
            final Node child = children.item(childIndex);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
            	Node idNode = child.getAttributes().getNamedItem("id");
            	if (idNode != null) {
	                final String id = idNode.getNodeValue();
	                int stateNodeIndex = 0;
	
	                // An init node without ID - should not bring the house down, does it?
	                // I have not checked if the state is restored correctly or not (JH)
	                while (stateNode[stateNodeIndex].getID() != null &&
	                        !stateNode[stateNodeIndex].getID().equals(id)) {
	                    stateNodeIndex++;
	                    if (stateNodeIndex >= stateNode.length) {
	                    	Log.warning.println("Cannot restore statenode id " + id + " -- item is ignored");
	                    	break;
	                    }
	                }
	                if (stateNodeIndex < stateNode.length) {
		                final StateNode stateNode2 = stateNode[stateNodeIndex].copy();
		                stateNode2.fromXML(child);
		                stateNode[stateNodeIndex].assignFromFragile(stateNode2);
	                }
            	} else {
                	Log.warning.println("Cannot restore statenode without id -- item is ignored");
            	}
            }
        }
    }

    @Override
    public String toString() {
        if (stateNode == null) {
            return "";
        }
        final StringBuilder buf = new StringBuilder();
        for (final StateNode node : stateNode) {
            buf.append(node.toString());
            buf.append("\n");
        }
        return buf.toString();
    }


    /**
     * Set dirtiness to all StateNode, this means that
     * apart from marking all StateNode.someThingIsDirty as isDirty
     * parameters mark all their dimension as isDirty and
     * trees mark all their nodes as isDirty.
     */
    public void setEverythingDirty(final boolean isDirty) {
        for (final StateNode node : stateNode) {
            node.setEverythingDirty(isDirty);
        }

        if (isDirty) {
            // happens only during debugging and start of MCMC chain
            for (int i = 0; i < stateNode.length; i++) {
                changeStateNodes[i] = i;
            }
            nrOfChangedStateNodes = stateNode.length;
        }
    }

    /**
     * Sets the posterior, needed to calculate paths of CalculationNode
     * that need store/restore/requireCalculation checks.
     * As a side effect, outputs for every beastObject in the model are calculated.
     * NB the output map only contains outputs on a path to the posterior BEASTObject!
     */
    @SuppressWarnings("unchecked")
    public void setPosterior(BEASTObject posterior) {
        // first, calculate output map that maps BEASTObjects on a path
        // to the posterior to the list of output BEASTObjects. Strictly
        // speaking, this is a bit of overkill, since only
        // CalculationNodes need to be taken in account, but for
        // debugging purposes (developer forgot to derive from CalculationNode)
        // we keep track of the lot.
        outputMap = new HashMap<>();
        outputMap.put(posterior, new ArrayList<>());
        boolean progress = true;
        List<BEASTInterface> beastObjects = new ArrayList<>();
        beastObjects.add(posterior);
        while (progress) {
            progress = false;
            // loop over plug-ins, till no more plug-ins can be added
            // efficiency is no issue here
            for (int i = 0; i < beastObjects.size(); i++) {
            	BEASTInterface beastObject = beastObjects.get(i);
                try {
                    for (BEASTInterface inputBEASTObject : beastObject.listActiveBEASTObjects()) {
                        if (!outputMap.containsKey(inputBEASTObject)) {
                            outputMap.put(inputBEASTObject, new ArrayList<>());
                            beastObjects.add(inputBEASTObject);
                            progress = true;
                        }
                        if (!outputMap.get(inputBEASTObject).contains(beastObject)) {
                            outputMap.get(inputBEASTObject).add(beastObject);
                            progress = true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // Set of array of StateNode outputs. Since the StateNodes have a potential
        // to be changing objects (when store/restore is applied) it is necessary
        // to use another method to find the outputs, an array in this case.
        stateNodeOutputs = new List[stateNode.length];
        for (int i = 0; i < stateNode.length; i++) {
            stateNodeOutputs[i] = new ArrayList<>();
            if (outputMap.containsKey(stateNode[i])) {
                for (BEASTInterface beastObject : outputMap.get(stateNode[i])) {
                    if (beastObject instanceof CalculationNode) {
                        stateNodeOutputs[i].add((CalculationNode) beastObject);
                    } else {
                        throw new RuntimeException("DEVELOPER ERROR: output of StateNode (" + stateNode[i].getID() + ") should be a CalculationNode, but " + beastObject.getClass().getName() + " is not.");
                    }
                }
            } else {
                Log.warning.println("\nWARNING: StateNode (" + stateNode[i].getID() + ") found that has no effect on posterior!\n");
            }
        }
    } // setPosterior

    /**
     * return current set of calculation nodes based on the set of StateNodes that have changed *
     */
    private List<CalculationNode> getCurrentCalculationNodes() {
        List<CalculationNode> calcNodes = trie.get(nrOfChangedStateNodes);
        if (calcNodes != null) {
            // the list is pre-calculated
            return calcNodes;
        }
        // we need to calculate the list of CalculationNodes now
        try {
            calcNodes = calculateCalcNodePath();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        trie.set(calcNodes, nrOfChangedStateNodes);

//    	System.err.print(Arrays.toString(changeStateNodes) + ":");
//    	for (CalculationNode node : calcNodes) {
//    		Log.warning.print(node.m_sID + " ");
//    	}
//    	System.err.println();

        return calcNodes;
    } // getCurrentCalculationNodes


    /**
     * Collect all CalculationNodes on a path from any StateNode that is changed (as
     * indicated by m_changedStateNodeCode) to the posterior. Return the list in
     * partial order as determined by the BEASTObjects input relations.
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    private List<CalculationNode> calculateCalcNodePath() throws IllegalArgumentException, IllegalAccessException {
        final List<CalculationNode> calcNodes = new ArrayList<>();
//    	for (int i = 0; i < stateNode.length; i++) {
//    		if (m_changedStateNodeCode.get(i)) {
        for (int k = 0; k < nrOfChangedStateNodes; k++) {
            int i = changeStateNodes[k];
            // go grab the path to the Runnable
            // first the outputs of the StateNodes that is changed
            boolean progress = false;
            for (CalculationNode node : stateNodeOutputs[i]) {
                if (!calcNodes.contains(node)) {
                    calcNodes.add(node);
                    progress = true;
                }
            }
            // next the path following the outputs
            while (progress) {
                progress = false;
                // loop over beastObjects till no more beastObjects can be added
                // efficiency is no issue here, assuming the graph remains 
                // constant
                for (int calcNodeIndex = 0; calcNodeIndex < calcNodes.size(); calcNodeIndex++) {
                    CalculationNode node = calcNodes.get(calcNodeIndex);
                    for (BEASTInterface output : outputMap.get(node)) {
                        if (output instanceof CalculationNode) {
                            final CalculationNode calcNode = (CalculationNode) output;
                            if (!calcNodes.contains(calcNode)) {
                                calcNodes.add(calcNode);
                                progress = true;
                            }
                        } else {
                            throw new RuntimeException("DEVELOPER ERROR: found a"
                                    + " non-CalculatioNode ("
                                    +output.getClass().getName()
                                    +") on path between StateNode and Runnable");
                        }
                    }
                }
            }
//    		}
        }

        // put calc nodes in partial order
        for (int i = 0; i < calcNodes.size(); i++) {
            CalculationNode node = calcNodes.get(i);
            List<BEASTInterface> inputList = node.listActiveBEASTObjects();
            for (int j = calcNodes.size() - 1; j > i; j--) {
                if (inputList.contains(calcNodes.get(j))) {
                    // swap
                    final CalculationNode node2 = calcNodes.get(j);
                    calcNodes.set(j, node);
                    calcNodes.set(i, node2);
                    j = 0;
                    i--;
                }
            }
        }

        return calcNodes;
    } // calculateCalcNodePath


    public double robustlyCalcPosterior(final Distribution posterior) {
        store(-1);
        setEverythingDirty(true);
        //state.storeCalculationNodes();
        checkCalculationNodesDirtiness();
        final double logLikelihood = posterior.calculateLogP();
        setEverythingDirty(false);
        acceptCalculationNodes();
        return logLikelihood;
    }

	public double robustlyCalcNonStochasticPosterior(Distribution posterior)  {
        store(-1);
        setEverythingDirty(true);
        storeCalculationNodes();
        checkCalculationNodesDirtiness();
        final double logLikelihood = posterior.getNonStochasticLogP();
        setEverythingDirty(false);
        acceptCalculationNodes();
        return logLikelihood;
	}
} // class State
