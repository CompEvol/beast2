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

import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;

import java.util.ArrayList;
import java.util.List;

@Description("The state represents the current point in the state space, and " +
        "maintains values of a set of parameters and trees.")
public class State extends Plugin {

    public Input<List<StateNode>> stateNodeInput = new Input<List<StateNode>>("stateNode", "a part of the state", new ArrayList<StateNode>());

    @Override
    public void initAndValidate(State state) {
        stateNode = stateNodeInput.get().toArray(new StateNode[0]);
        // allocate memory for storing the state
        storedStateNode = new StateNode[stateNode.length];

        for (StateNode node : stateNode) {
            node.index = getStateNodeIndex(node.getID());
        }
    }

    
    
    /**
     * the components of the state, for instance beast.tree & parameters 
     */
    public StateNode[] stateNode;

    /** copy of state nodes, for restoration if required **/
    public StateNode[] storedStateNode;
    /** list of inputs connected to StateNodes in the state **/
    Input<?> [] m_inputs;
    Integer [] m_stateNodenr;
    
    /** Store a State.
     * This copies the state for possible later restoration
     * but does not affect any inputs, which are all still connected
     * to the StateNodes in  **/
    public void store() {
for (int iStateNode = 0; iStateNode < stateNode.length; iStateNode++) {
    		storedStateNode[iStateNode] = stateNode[iStateNode].copy();
    	}
    }
    /** Restore a State. 
     * This assigns the state to the stored state and
     * reassigns all Inputs connected to the state. **/
    public void restore() {
    	for (int iStateNode = 0; iStateNode < stateNode.length; iStateNode++) {
    		stateNode[iStateNode] = storedStateNode[iStateNode];
    	}
    	for(int iInput = 0; iInput < m_inputs.length; iInput++) {
    		m_inputs[iInput].set(stateNode[m_stateNodenr[iInput]]);
    	}
    }

    
    public int stateNumber = 0;



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

    /**
     * return a value with identifier sID. This assumes a single dimensional parameter. *
     */
    public int getStateNodeIndex(String sID) {
        for (int i = 0; i < stateNode.length; i++) {
            if (stateNode[i].getID().equals(sID)) {
                return i;
            }
        }
        return -1;
        //throw new Exception("Error 124: No such id (" + sID + ") in parameters");
    }

    public boolean isDirty(Input<? extends StateNode> p) {
        return stateNode[p.get().index].isDirty();
    }

//    public Double getValue(Input<Parameter> p) {
//        return getValue(p.get());
//    }

//    public Double getValue(Parameter p) {
//        return (Double) getValue(p.getParamNr(this));
//    }

//    public Object getValue(int nID) {
//        return m_parameters[nID].getValue();
//    }

//    public Object getValue(int nID, int iDim) {
//        return m_parameters[nID].getValue(iDim);
//    }

    public boolean isDirty(int nID) {
        return stateNode[nID].isDirty();
    }

    public StateNode getStateNode(int nID) {
        return stateNode[nID];
    }

    public StateNode getStateNode(String sID) {
        int nID = getStateNodeIndex(sID);
        return stateNode[nID];
    }

    public StateNode getStateNode(Input<? extends StateNode> p) {

        //for (int i = 0; i < stateNode.length; i++) {
        //    if (stateNode[i].getID().equals(p.get().getID())) return stateNode[i];
        //}
        //throw new IllegalArgumentException(p.getName() + " is not found in this state");

        int nID = p.get().getIndex(this);
        return stateNode[nID];
    }

    public RealParameter getParameter(Input<RealParameter> p) {
        return (RealParameter) getStateNode(p);
    }

    public IntegerParameter getIntParameter(Input<IntegerParameter> p) {
        return (IntegerParameter) getStateNode(p);
    }


//    public void setValue(int nID, Object fValue) {
//	        m_parameters[nID].setValue(fValue);
//	}
//	void setValue(int nID, int iDim, Object fValue) {
//	        m_parameters[nID].setValue(iDim, fValue);
//	}

    /**
     * multiply a value by a given amount *
     */
//    public void mulValue(double fValue, int m_nParamID) {
//        ((Parameter) m_parameters[m_nParamID]).values[0] *= fValue;
//        m_parameters[m_nParamID].m_bIsDirty = State.IS_DIRTY;
//    }
//
//    public void mulValue(int iParam, double fValue, Parameter param) {
//        param.values[iParam] *= fValue;
//        param.m_bIsDirty = State.IS_DIRTY;
//    }
//
//    public void mulValues(double fValue, Parameter param) {
//        double[] values = param.values;
//        for (int i = 0; i < values.length; i++) {
//            values[i] *= fValue;
//        }
//        param.m_bIsDirty = State.IS_DIRTY;
//    }
    public State copy() throws Exception {
        State copy = new State();
        copy.stateNode = new StateNode[stateNode.length];
        for (int i = 0; i < stateNode.length; i++) {
            copy.stateNode[i] = stateNode[i].copy();
        }
        return copy;
    }


//    public void prepare() throws Exception {
//        for (int i = 0; i < m_parameters.length; i++) {
//            m_parameters[i].prepare();
//        }
//    }


//    public String toString(List<String> sTaxaNames) {
//        StringBuffer buf = new StringBuffer();
//        for (int i = 0; i < m_parameters.length; i++) {
//            buf.append(m_parameters[i].toString());
//            buf.append("\n");
//        }
//        return buf.toString();
//    }

    public String toString() {
    	if (stateNode == null) {
    		return "";
    	}
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < stateNode.length; i++) {
            buf.append(stateNode[i].toString());
            buf.append("\n");
        }
        return buf.toString();
    }

    /**
     * Make sure that state is still consistent
     * For debugging purposes only
     *
     * @throws Exception
     */
    public void validate() throws Exception {
    }

    /**
     * set dirtiness to all parameters and trees *
     */
    public void setDirty(boolean isDirty) {
        for (StateNode node : stateNode) {
            node.setDirty(isDirty);
        }
    }



    /** 
     * Collect all inputs connected to a state node that can be reached from
     * the run-plug-in via a path connecting plug-ins with inputs (except 
     * operators and loggers).
     * 
     * This should be called by any runnable in its initAndValidate method.
     */
    protected void calcInputsConnectedToState(Plugin run) {
    	List<Input<?>> inputsConnectedToState = new ArrayList<Input<?>>();
    	List<Integer> stateNodeNr = new ArrayList<Integer>();

    	List<Plugin> plugins = new ArrayList<Plugin>();
    	getAllPrecedingPlugins(plugins, run);
    	for (Plugin plugin : plugins) {
    		// ignore operators
    		if (!(plugin instanceof Operator) && !(plugin instanceof Logger)) {
    		try {
	    		for (Input<?> input : plugin.listInputs()) {
	    			if (input.get() instanceof StateNode) {
	    				// check it is part of the state
	    				for (int iStateNode= 0; iStateNode < stateNode.length; iStateNode++) {
	    					if (stateNode[iStateNode] == input.get()) {
	    						if (!inputsConnectedToState.contains(input)) {
	    							inputsConnectedToState.add(input);
	    							stateNodeNr.add(iStateNode);
	    						}
	    						break;
	    					}
	    				}
	    			}
	    		}
    		} catch (Exception e) {
    			// ignore
				System.err.println(e.getMessage());
			}
    		}
    	}
    	
    	m_inputs = inputsConnectedToState.toArray(new Input<?>[0]);
    	m_stateNodenr = stateNodeNr.toArray(new Integer[0]);
    } // calcInputsConnectedToState
    
    /** 
     * Collect all Cacheables that are on a path from a StateNode to 
     * the run Plugin.
     * 
     * This should be called by any runnable in its initAndValidate method.
     */
    protected List<Cacheable> getCacheableOutputs(Plugin run) {
    	List<Plugin> plugins = new ArrayList<Plugin>();
    	getAllOutputPlugins(plugins, run);
    	//getAllPrecedingPlugins(plugins, run);
    	
    	List<Cacheable> cacheables = new ArrayList<Cacheable>();
    	for (Plugin plugin: plugins) {
    		if (plugin instanceof Cacheable) {
    			cacheables.add((Cacheable) plugin);
    		}
    	}
    	return cacheables;
    } // getCacheableOutputs

    /** get all Plugins on a path from the a StateNode to the run 
     * Plugin connected through Plugin Inputs.
     */
	void getAllOutputPlugins(List<Plugin> plugins, Plugin run) {
		// collect all plug-ins in the model
    	List<Plugin> allPlugins = new ArrayList<Plugin>();
    	getAllPrecedingPlugins(allPlugins, run);
    	
    	// start with all plug-ins connected to the state
    	for (Plugin plugin : allPlugins) {
    		try {
	    		for (Input<?> input : plugin.listInputs()) {
	    			if (input.get() instanceof StateNode) {
	    				// check it is part of the state
	    				for (int iStateNode= 0; iStateNode < stateNode.length; iStateNode++) {
	    					if (stateNode[iStateNode] == input.get()) {
	    						if (!plugins.contains(plugin)) {
	    							plugins.add(plugin);
	    						}
	    						break;
	    					}
	    				}
	    			}
	    		}
    		} catch (Exception e) {
    			// ignore
				System.err.println(e.getMessage());
			}
    	}
    	
    	// add plug-ins connected to plug-ins connected to the state, etc.
    	boolean bProgress = false;
    	do {
    		bProgress = false;
        	for (Plugin plugin : allPlugins) {
        		if (plugin.getClass().getName().contains("Compound")) {
        			int h = 34;
        			h++;
        		}
        			
        		try {
    	    		for (Input<?> input : plugin.listInputs()) {
    	    			if (input.get() instanceof Plugin) {
    	    				Plugin inputPlugin = (Plugin) input.get();
    	    				// check it is part of the state
    	    				for (Plugin connectedPlugin : plugins) {
    	    					if (connectedPlugin == inputPlugin) {
    	    						if (!plugins.contains(plugin)) {
    	    							plugins.add(plugin);
    	    							bProgress = true;
    	    						}
    	    					}
    	    				}
    	    			} else if (input.get() instanceof List<?>) {
        					for (Object o : (List<?>) input.get()) {
        						if (o instanceof Plugin) {
            	    				// check it is part of the state
            	    				for (Plugin connectedPlugin : plugins) {
            	    					if (connectedPlugin == (Plugin) o) {
            	    						if (!plugins.contains(plugin)) {
            	    							plugins.add(plugin);
            	    							bProgress = true;
            	    						}
            	    					}
            	    				}
        						}
        					}    	    				
    	    			}

    	    		}
        		} catch (Exception e) {
        			// ignore
    				System.err.println(e.getMessage());
    			}
        	}
    	} while (bProgress);
	} // getAllOutputPlugins

    
    
    /** Get all plug-ins that have a path via inputs to plugin (including plugin itself)
     */
	void getAllPrecedingPlugins(List<Plugin> plugins, Plugin plugin) {
		if (!plugins.contains(plugin)) {
			plugins.add(plugin);
		}
		try {
			Input<?> [] inputs = plugin.listInputs();
    		for (Input<?> input : inputs) {
    			if (input.get() != null) {
    				if (input.get() instanceof Plugin) {
    					// recurse
    					getAllPrecedingPlugins(plugins, (Plugin) input.get());
    				} else if (input.get() instanceof List<?>) {
    					for (Object o : (List<?>) input.get()) {
    						if (o instanceof Plugin) {
    	    					// recurse
    							getAllPrecedingPlugins(plugins, (Plugin) o);
    						}
    					}
    				}
    			}
    		}
		} catch (Exception e) {
			// ignore
			System.err.println(e.getMessage());
		}
	} // getAllPrecedingPlugins
	
} // class State
