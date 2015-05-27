/*
* File Sequence.java
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
package beast.evolution.alignment;


import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.BEASTObject;
import beast.evolution.datatype.DataType;


@Description("Single sequence in an alignment.")
public class Sequence extends BEASTObject {
    public Input<Integer> totalCountInput = new Input<Integer>("totalcount", "number of states or the number of lineages for this species in SNAPP analysis");
    public Input<String> taxonInput = new Input<String>("taxon", "name of this species", Input.Validate.REQUIRED);
    public Input<Boolean> uncertainInput = new Input<Boolean>("uncertain", "if true, sequence is provided as comma separated probabilities for each character, with sites separated by a semi-colons");
    public Input<String> dataInput = new Input<String>("value",
            "sequence data, either encoded as a string or as comma separated list of integers, or comma separated probabilities for each site if uncertain=true." +
                    "In either case, whitespace is ignored.", Input.Validate.REQUIRED);
  
    protected boolean uncertain = false;
    protected double[][] probabilities = null;    
    public double[][] getProbabilities() {
    	return probabilities;
    }
    
    public Sequence() {
    }

    /**
     * Constructor for testing.
     *
     * @param taxon
     * @param sequence
     * @throws Exception
     */
    public Sequence(String taxon, String sequence) throws Exception {
        taxonInput.setValue(taxon, this);
        dataInput.setValue(sequence, this);
        initAndValidate();
    }

    @Override
    public void initAndValidate() throws Exception {
    	if (uncertainInput.get() != null && uncertainInput.get())  {
    		uncertain = true;
    		initProbabilities();    		
    	}
    } // initAndValidate
    
    public void initProbabilities() throws Exception {
    	
    	//TODO Test that the input format is correct while parsing
    	
    	String data = dataInput.get();
        // remove spaces
        data = data.replaceAll("\\s", "");
        
        String sStr = data.trim();		
		String[] strs = sStr.split(";");		
		for (int i=0; i<strs.length; i++) {
			String[] pr = strs[i].split(",");
	    	// TODO Handle gap characters here
			double total = 0;
    		for (int j=0; j<pr.length; j++) {    			
    			if (probabilities == null) probabilities = new double[strs.length][pr.length];
    			probabilities[i][j] = Double.parseDouble(pr[j].trim());
    			total += probabilities[i][j]; 
    		}
    		if (Math.abs(total - 1) > 1e-2) {
    			throw new Exception("Probabilities for '" + taxonInput.get() + "' do not sum to unity at site "+i+".");
    		}
		}
    }

    public List<Integer> getSequence(DataType dataType) throws Exception {
        
    	List<Integer> sequence;
    	if (uncertain) {
            sequence = new ArrayList<Integer>();
            for (int i=0; i<probabilities.length; i++) {
            	double m = probabilities[i][0];
            	int index = 0;
            	for (int j=0; j<probabilities[i].length; j++) {
            		if (probabilities[i][j] > m ) {
            			m = probabilities[i][j];
            			index = j;
            		}        		
            	}
            	sequence.add(index);
            }
    	}
    	else {
	    	String data = dataInput.get();
	        // remove spaces
	        data = data.replaceAll("\\s", "");
	        sequence = dataType.string2state(data);
    	}

        if (totalCountInput.get() == null) {
            // derive default from char-map
            totalCountInput.setValue(dataType.getStateCount(), this);
        }
        return sequence;
    }

    int mapCharToData(String dataMap, char c) {
        int i = dataMap.indexOf(c);
        if (i >= 0) {
            return i;
        }
        return dataMap.length();
    } // mapCharToData

} // class Sequence
