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
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import beast.core.Input;
//import beast.util.XMLProducer;

@Description("The state represents the current point in the state space, and " +
        "maintains values of a set of parameters and trees.")
public class State extends Plugin {

    public Input<List<StateNode>> stateNodeInput = new Input<List<StateNode>>("stateNode", "a part of the state", new ArrayList<StateNode>());
    public Input<Integer> m_storeEvery = new Input<Integer>("storeEvery", "store the state to disk every X number of samples so that we can " +
    		"resume computation later on", -1);
    String m_sStateFileName = "state.backup.xml";
    
    @Override
    public void initAndValidate() {
        stateNode = stateNodeInput.get().toArray(new StateNode[0]);
        // allocate memory for storing the state
        storedStateNode = new StateNode[stateNode.length];

        for (int i = 0; i < stateNode.length; i++) {
            stateNode[i].index = i;
        }
        // make itself known
        for (int i = 0; i < stateNode.length; i++) {
        	stateNode[i].m_state = this;
        }
        
        m_nStoreEvery = m_storeEvery.get();
        
    	m_changedStateNodeCode = new BitSet(stateNode.length);
        m_map = new HashMap<BitSet, List<CalculationNode>>();
        // add the empty list for the case none of the StateNodes have changed
    	m_map.put(m_changedStateNodeCode, new ArrayList<CalculationNode>());
    } // initAndValidate

    
    
    /**
     * the components of the state, for instance beast.tree & parameters 
     */
    public StateNode[] stateNode;

    /** copy of state nodes, for restoration if required **/
    public StateNode[] storedStateNode;
    
    /** interval for storing state to disk **/
    int m_nStoreEvery;
    
    /** Store a State.
     * This copies the state for possible later restoration
     * but does not affect any inputs, which are all still connected
     * to the original StateNodes 
     * 
     * Also, store the state to disk for resumption of analysis later on.
     **/
    public void store(int nSample) {
    	System.arraycopy(stateNode, 0, storedStateNode, 0, stateNode.length);
    	m_changedStateNodeCode = new BitSet(stateNode.length);
    	
    	if (m_nStoreEvery> 0 && nSample > 0 && nSample % m_nStoreEvery == 0) {
    		storeToFile();
    	}
    }

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
	        		stateNode[iStateNode].assignFrom(stateNode2);
	        	}
	        }
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    
    /** Restore a State. 
     * This assigns the state to the stored state 
     * but does not affect any Inputs connected to any stateNode. **/
    public void restore() {
    	StateNode [] tmp = storedStateNode;
    	storedStateNode = stateNode;
    	stateNode = tmp;
    }

    /** return currently valid state node **/
    public StateNode getStateNode(int nID) {
        return stateNode[nID];
    }

    /** Return StateNode that can be changed, but later restored
     * if necessary. If there is no copy stored already, a copy is 
     * made first, and the StateNode is marked as being dirty.
     */
    protected StateNode getEditableStateNode(int nID, Operator operator) {
    	if (stateNode[nID] == storedStateNode[nID]) {
    		storedStateNode[nID] = stateNode[nID].copy();
    		storedStateNode[nID].m_state = this;
    		stateNode[nID].setSomethingIsDirty(true);
    		storedStateNode[nID].setSomethingIsDirty(false);
    		m_changedStateNodeCode.set(nID);
    	}
        return stateNode[nID];
    }
    

    /**
     * primitive operations on the list of parameters *
     */
    public void addStateNode(StateNode node) {
        if (stateNode == null) {
            stateNode = new StateNode[1];
            stateNode[0] = node;
            return;
        } 
        StateNode[] h = new StateNode[stateNode.length + 1];
        for (int i = 0; i < h.length - 1; i++) {
            h[i] = stateNode[i];
        }
        h[h.length - 1] = node;
        stateNode = h;
    }


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
     * set dirtiness to all parameters and trees *
     */
    public void setEverythingDirty(boolean isDirty) {
        for (StateNode node : stateNode) {
            node.setEverythingDirty(isDirty);
        }
    }

    /** Sets the m_posterior, needed to calculate paths of CalculationNode
     * that need store/restore/requireCalculation checks.
     * As a side effect, outputs for every plugin in the model are calculated.
     * NB the output map only contains outputs on a path to the posterior Plugin!
     * @throws Exception 
     */
    @SuppressWarnings("unchecked")
	public void setPosterior(Plugin posterior) throws Exception {
    	m_posterior = posterior;
    	
    	m_outputMap = new HashMap<Plugin, List<Plugin>>();
    	m_outputMap.put(posterior, new ArrayList<Plugin>());
		boolean bProgress = true;
		List<Plugin> plugins = new ArrayList<Plugin>();
		plugins.add(posterior);
		while (bProgress) {
			bProgress = false;
			// loop over plugins, till no more plugins can be added
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
	} // setRunnable
    
    /** We need this information to calculate paths from a (set of) StateNodes 
     * that are changed by an operator up to the m_posterior.
     */
    Plugin m_posterior = null;
    /** maps a Plugin to a list of Outputs **/
    HashMap<Plugin, List<Plugin>> m_outputMap;
    /** same as m_outputMap, but only for StateNodes indexed by the StateNode number
     * We need this since the StateNode change regularly.
     */
    private List<CalculationNode> [] m_stateNodeOutputs;
    
    /** Code that represents configuration of StateNodes that have changed.
     * This is reset when the state is Stored, and every time a StateNode
     * is requested by an operator, the code is updated.
     */
    private BitSet m_changedStateNodeCode;
    
    /** Maps the changed states node code to 
     * the set of calculation nodes that is potentially affected by an operation **/
    private HashMap<BitSet, List<CalculationNode>> m_map;
    
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
    	//System.out.println("Map size is now " + m_map.size());
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
    
    
    /** Visit all calculation nodes in partial order determined by the Plugin-input relations
     * (i.e. if A is input of B then A < B)
     */
    public void checkCalculationNodesDirtiness() {
        List<CalculationNode> currentSetOfCalculationNodes = getCurrentCalculationNodes();
        for (CalculationNode calculationNode : currentSetOfCalculationNodes) {
            calculationNode.checkDirtiness();
        }
    }

    public void storeCalculationNodes() {
        List<CalculationNode> currentSetOfCalculationNodes = getCurrentCalculationNodes();
        for (CalculationNode calculationNode : currentSetOfCalculationNodes) {
            calculationNode.store();
        }
    }

    public void restoreCalculationNodes() {
        List<CalculationNode> currentSetOfCalculationNodes = getCurrentCalculationNodes();
        for (CalculationNode calculationNode : currentSetOfCalculationNodes) {
            calculationNode.restore();
        }
    }
   
} // class State


/*
todo:
	Traits
	Tip heights
	clade proportions on MultiMCMC
	reinstate SNAPP
	multi dim start values for parameters
	
done:
	State toXml fromXML
	State - do the store, restore, requires calculation 
	code review: naming booleans, indices
	listPlugins => listActivePlugins
	make StateNode scaleable
	StateNode assignFrom
*/