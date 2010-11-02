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

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import beast.core.Input;

/**"The state represents the current point in the state space, and " +
        "maintains values of a set of StateNodes, such as parameters and trees. " +
        "Furthermore, the state manages which parts of the model need to be stored/restored " +
        "and notified that recalculation is appropriate."**/



@Description("The state represents the current point in the state space, and " +
        "maintains values of a set of StateNodes, such as parameters and trees. " +
        "Furthermore, the state manages which parts of the model need to be stored/restored " +
        "and notified that recalculation is appropriate.")
public class State extends Plugin {

    public Input<List<StateNode>> stateNodeInput = 
            new Input<List<StateNode>>("stateNode", "a part of the state", new ArrayList<StateNode>());
    public Input<Integer> m_storeEvery = 
            new Input<Integer>("storeEvery", "store the state to disk every X number of samples so that we can " +
    		"resume computation later on", -1);
    
    /**
     * The components of the state, for instance tree & parameters.
     * This represents the current state, but a copy is kept so that when
     * an operation is applied to the State but the proposal is not accepted, 
     * the state can be restored. This is currently implemented by having 
     * Operators call getEditableStateNode() at which point the requested
     * StateNode is copied.
     */
    // Public so it can be interrogated. No point in creating a getter...
    public StateNode[] stateNode;

    /** Copy of state nodes, for restoration if required **/
    private StateNode[] storedStateNode;
    
    /** number of state nodes **/
    private int m_nStateNode;
    public int getNrOfStateNodes() {return m_nStateNode;}
    
    /** pointers to memory allocated to stateNodes and storedStateNodes **/
    private StateNode[] m_stateNodeMem;

    /** File naem use for storing the state, either periodically or at the end of an MCMC chain
     * so that the chain can be resumed
     */
    private String m_sStateFileName = "state.backup.xml";

    /** Interval for storing state to disk, if negative the state will not be stored periodically **/
    private int m_nStoreEvery;

    /** The following members are involved in calculating the set of
     * CalculatioNodes that need to be notified when an operation
     * has been applied to the State. The Calculation nodes are then
     * store/restore/accepted/check dirtiness in partial order.
     */
    
    /** Maps a Plugin to a list of Outputs.
     * This map only contains those plugins that have a path to the posterior **/
    private HashMap<Plugin, List<Plugin>> m_outputMap;
    
    /** Same as m_outputMap, but only for StateNodes indexed by the StateNode number
     * We need this since the StateNode changes regularly, so unlike the output map
     * for Plugins cannot be accessed by the current StateNode as key.
     */
    private List<CalculationNode> [] m_stateNodeOutputs;
    
    /** Code that represents configuration of StateNodes that have changed
     * during an operation. Currently implemented as BitSet, but there are
     * possibly more efficient implementations possible.
     * TODO: investigate.
     *  
     * Every time an operation requests a StateNode, a bit for that StateNode
     * is set.
     * The code is reset when the state is stored, and every time a StateNode
     * is requested by an operator, the code is updated.
     */
    private BitSet m_changedStateNodeCode;
    
    /** Maps the changed states node code to 
     * the set of calculation nodes that is potentially affected by an operation **/
    private HashMap<BitSet, List<CalculationNode>> m_map;
    
    @Override
    public void initAndValidate() {
        stateNode = stateNodeInput.get().toArray(new StateNode[0]);
        // allocate memory for storing the state
        storedStateNode = new StateNode[stateNode.length];

        for (int i = 0; i < stateNode.length; i++) {
            stateNode[i].index = i;
        }
        // make itself known
        for(StateNode state : stateNode) {
            state.m_state = this;
        }
        
        m_nStateNode = stateNode.length;
        // allocate memory for StateNodes and a copy.
        m_stateNodeMem = new StateNode[m_nStateNode*2];
        for (int i = 0; i < m_nStateNode; i++) {
        	m_stateNodeMem[i] = stateNode[i];
        	m_stateNodeMem[m_nStateNode + i] = m_stateNodeMem[i].copy(); 
        }
        
        // grab the interval for storing the state to file
        m_nStoreEvery = m_storeEvery.get();
        
        // set up datastructure for encoding which StateNodes change by an operation
    	m_changedStateNodeCode = new BitSet(stateNode.length);
        m_map = new HashMap<BitSet, List<CalculationNode>>();
        // add the empty list for the case none of the StateNodes have changed
    	m_map.put(m_changedStateNodeCode, new ArrayList<CalculationNode>());
    } // initAndValidate
    
    
    /** return currently valid state node. This is typically called from a
     * CalculationNode for inspecting the value of a StateNode, not for
     * changing it. To change a StateNode, say from an Operator,  
     * getEditableStateNode() should be called. **/
    public StateNode getStateNode(int nID) {
        return stateNode[nID];
    }

    /** Return StateNode that can be changed, but later restored
     * if necessary. If there is no copy stored already, a copy is 
     * made first, and the StateNode is marked as being dirty.
	 *
     * NB This should only be called from an Operator that wants to
     * change the particular StateNode through the Input.get(Operator) 
     * method on the input associated with this StateNode.
     */
    protected StateNode getEditableStateNode(int nID, Operator operator) {
    	if (stateNode[nID] == storedStateNode[nID]) {
    		if (stateNode[nID] == m_stateNodeMem[nID]) {
    			storedStateNode[nID] = m_stateNodeMem[m_nStateNode + nID];
    		} else {
    			storedStateNode[nID] = m_stateNodeMem[nID];
    		}
    		storedStateNode[nID].assignFromFragile(stateNode[nID]);

    		storedStateNode[nID].m_state = this;
    		stateNode[nID].setSomethingIsDirty(true);
    		storedStateNode[nID].setSomethingIsDirty(false);
    		m_changedStateNodeCode.set(nID);
    	}
        return stateNode[nID];
    }

    /** Store a State before applying an operation proposal to the state.
     * This copies the state for possible later restoration
     * but does not affect any inputs, which are all still connected
     * to the original StateNodes 
     * 
     * Also, store the state to disk for resumption of analysis later on.
     *
     * @param nSample chain state number
     **/
    public void store(int nSample) {
    	System.arraycopy(stateNode, 0, storedStateNode, 0, m_nStateNode);
    	m_changedStateNodeCode.clear();// = new BitSet(m_nStateNode);
    	
    	if (m_nStoreEvery> 0 && nSample % m_nStoreEvery == 0 && nSample > 0) {
    		storeToFile();
    	}
    }
    
    /** Restore a State after rejecting the operation proposal. 
     * This assigns the state to the stored state.
     * NB this does not affect any Inputs connected to any stateNode. **/
    public void restore() {
    	StateNode [] tmp = storedStateNode;
    	storedStateNode = stateNode;
    	stateNode = tmp;
    }

    /** Visit all calculation nodes in partial order determined by the Plugin-input relations
     * (i.e. if A is input of B then A < B). There are 4 operations that can be propagated this
     * way:
     * 
     * store() makes sure all calculation nodes store their internal state
     * 
     * checkDirtiness() makes all calculation nodes check whether they give a different answer 
     * when interrogated by one of its outputs
     * 
     * accept() allows all calculation nodes to mark themselves as being clean without further
     * calculation
     * 
     * restore() if a proposed state is not accepted, all calculation nodes need to restore 
     * themselves
     */
    public void storeCalculationNodes() {
        List<CalculationNode> currentSetOfCalculationNodes = getCurrentCalculationNodes();
        for (CalculationNode calculationNode : currentSetOfCalculationNodes) {
            calculationNode.store();
        }
    }

    public void checkCalculationNodesDirtiness() {
        List<CalculationNode> currentSetOfCalculationNodes = getCurrentCalculationNodes();
        for (CalculationNode calculationNode : currentSetOfCalculationNodes) {
            calculationNode.checkDirtiness();
        }
    }

    public void restoreCalculationNodes() {
        List<CalculationNode> currentSetOfCalculationNodes = getCurrentCalculationNodes();
        for (CalculationNode calculationNode : currentSetOfCalculationNodes) {
            calculationNode.restore();
        }
    }
  
    public void acceptCalculationNodes() {
	    List<CalculationNode> currentSetOfCalculationNodes = getCurrentCalculationNodes();
	    for (CalculationNode calculationNode : currentSetOfCalculationNodes) {
	        calculationNode.accept();
	    }
    }

    /** set name of state file, used when storing/restoring the state to disk **/
    public void setStateFileName(String sFileName) {
    	if (sFileName != null) {
    		m_sStateFileName = sFileName;
    	}
    }
    
    /** Print state to file. This is called either periodically or at the end
     * of an MCMC chain, so that the state can be resumed later on.
     */
    public void storeToFile() {
		try {
			PrintStream out = new PrintStream(m_sStateFileName);
			out.print("<itsabeastystatewerein version='2.0'>\n");
			for(StateNode node : stateNode) {
				node.toXML(out);
			}
			out.print("</itsabeastystatewerein>\n");
			//out.print(new XMLProducer().toXML(this));
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    /** restore a state from file for resuming an MCMC chain **/
    public void restoreFromFile() {
		try {
			System.out.println("Restoring from file");
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        Document doc= factory.newDocumentBuilder().parse(new File(m_sStateFileName));
	        doc.normalize();
	        NodeList nodes = doc.getElementsByTagName("*");
	        Node topNode = nodes.item(0);
	        NodeList children = topNode.getChildNodes();
	        for (int iChild = 0; iChild < children.getLength(); iChild++) {
	        	Node child = children.item(iChild);
	        	if (child.getNodeType() == Node.ELEMENT_NODE) {
	        		String sID = child.getAttributes().getNamedItem("id").getNodeValue();
	        		int iStateNode = 0;
	        		while (!stateNode[iStateNode].getID().equals(sID)) {
	        			iStateNode ++;
	        		}
	        		StateNode stateNode2 = stateNode[iStateNode].copy();
	        		stateNode2.fromXML(child);
	        		stateNode[iStateNode].assignFromFragile(stateNode2);
	        	}
	        }
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
    }

    @Override
    public String toString() {
    	if (stateNode == null) {
    		return "";
    	}
        StringBuffer buf = new StringBuffer();
        for (StateNode node : stateNode) {
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
    public void setEverythingDirty(boolean isDirty) {
        for (StateNode node : stateNode) {
            node.setEverythingDirty(isDirty);
        }
        
        if (isDirty) {
        	// happens only during debugging and start of MCMC chain
        	m_changedStateNodeCode.clear();// = new BitSet(stateNode.length);
        	for (int i = 0; i < stateNode.length; i++) {
        		m_changedStateNodeCode.set(i);
        	}
        }
    }

    /** Sets the posterior, needed to calculate paths of CalculationNode
     * that need store/restore/requireCalculation checks.
     * As a side effect, outputs for every plugin in the model are calculated.
     * NB the output map only contains outputs on a path to the posterior Plugin!
     */
    @SuppressWarnings("unchecked")
	public void setPosterior(Plugin posterior) throws Exception {
    	// first, calculate output map that maps Plugins on a path
    	// to the posterior to the list of output Plugins. Strictly
    	// speaking, this is a bit of overkill, since only 
    	// CalculationNodes need to be taken in account, but for
    	// debugging purposes (developer forgot to derive from CalculationNode)
    	// we keep track of the lot.
    	m_outputMap = new HashMap<Plugin, List<Plugin>>();
    	m_outputMap.put(posterior, new ArrayList<Plugin>());
		boolean bProgress = true;
		List<Plugin> plugins = new ArrayList<Plugin>();
		plugins.add(posterior);
		while (bProgress) {
			bProgress = false;
			// loop over plug-ins, till no more plug-ins can be added
			// efficiency is no issue here
			for (int iPlugin = 0; iPlugin < plugins.size(); iPlugin++) {
				Plugin plugin = plugins.get(iPlugin);
				try {
					for (Plugin inputPlugin : plugin.listActivePlugins()) {
						if (!m_outputMap.containsKey(inputPlugin)) {
							m_outputMap.put(inputPlugin, new ArrayList<Plugin>());
							plugins.add(inputPlugin);
							bProgress = true;
						}
						if (!m_outputMap.get(inputPlugin).contains(plugin)) {
							m_outputMap.get(inputPlugin).add(plugin);
							bProgress = true;
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
		m_stateNodeOutputs = new List[stateNode.length];
		for (int i = 0; i < stateNode.length; i++) {
			m_stateNodeOutputs[i] = new ArrayList<CalculationNode>();
			if (m_outputMap.containsKey(stateNode[i])) {
				for (Plugin plugin : m_outputMap.get(stateNode[i])) {
					if (plugin instanceof CalculationNode) {
						m_stateNodeOutputs[i].add((CalculationNode)plugin);
					} else {
						throw new Exception("DEVELOPER ERROR: output of StateNode should be a CalculationNode");
					}
				}
			} else {
				System.out.println("\nWARNING: StateNode ("+stateNode[i].getID()+") found that has no effect on posterior!\n");
			}
		}
	} // setPosterior
    
    /** return current set of calculation nodes based on the set of StateNodes that have changed **/ 
    private List<CalculationNode> getCurrentCalculationNodes() {
    	List<CalculationNode> calcNodes = m_map.get(m_changedStateNodeCode);
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

    	m_map.put(m_changedStateNodeCode, calcNodes);
    	
    	System.out.print(m_changedStateNodeCode + ":");
    	for (CalculationNode node : calcNodes) {
    		System.out.print(node.m_sID + " ");
    	}
    	System.out.println();
    	
    	return calcNodes;
    } // getCurrentCalculationNodes

    
    /** Collect all CalculationNodes on a path from any StateNode that is changed (as
     * indicated by m_changedStateNodeCode) to the posterior. Return the list in
     * partial order as determined by the Plugins input relations.
     */
    private List<CalculationNode> calculateCalcNodePath() throws Exception {
    	List<CalculationNode> calcNodes = new ArrayList<CalculationNode>();
    	for (int i = 0; i < stateNode.length; i++) {
    		if (m_changedStateNodeCode.get(i)) {
    			// go grab the path to the Runnable
    			// first the outputs of the StateNodes that is changed
    			boolean bProgress = false;
    			for (CalculationNode node : m_stateNodeOutputs[i]) {
    				if (!calcNodes.contains(node)) {
    					calcNodes.add(node);
    	    			bProgress = true;
    				}
    			}
    			// next the path following the outputs
    			while (bProgress) {
    				bProgress = false;
    				// loop over plugins, till no more plugins can be added
    				// efficiency is no issue here
    				for (int iCalcNode = 0; iCalcNode < calcNodes.size(); iCalcNode++) {
    					CalculationNode node = calcNodes.get(iCalcNode);
    					for (Plugin output : m_outputMap.get(node)) {
    						if (output instanceof CalculationNode) {
    							CalculationNode calcNode = (CalculationNode) output;
    							if (!calcNodes.contains(calcNode)) {
    								calcNodes.add(calcNode);
    								bProgress = true;
    							}
    						} else {
    							throw new Exception ("DEVELOPER ERROR: found a non-CalculatioNode on path between StateNode and Runnable");
    						}
    					}
    				}
    			}
    		}
    	}
    	
    	// put calc nodes in partial order
    	for (int i = 0; i < calcNodes.size(); i++) {
    		CalculationNode node = calcNodes.get(i);
    		List<Plugin> inputList = node.listActivePlugins();
    		for (int j = calcNodes.size() - 1; j > i; j--) {
        		if (inputList.contains(calcNodes.get(j))) {
        			// swap
            		CalculationNode node2 = calcNodes.get(j);
            		calcNodes.set(j, node);
            		calcNodes.set(i, node2);
        			j = 0;
        			i--;
        		}
    		}
    	}
    	
    	return calcNodes;
    } // calculateCalcNodePath
    
} // class State
