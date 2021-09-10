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
package beast.base.evolution.alignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.datatype.DataType;

@Description("Single sequence in an alignment.")
public class Sequence extends BEASTObject {
    final public Input<Integer> totalCountInput = new Input<>("totalcount", "number of states or the number of lineages for this species in SNAPP analysis");
    final public Input<String> taxonInput = new Input<>("taxon", "name of this species", Input.Validate.REQUIRED);
    final public Input<String> dataInput = new Input<>("value",
            "sequence data, either encoded as a string or as comma separated list of integers, or comma separated likelihoods/probabilities for each site if uncertain=true." +
                    "In either case, whitespace is ignored.", Input.Validate.REQUIRED);
    final public Input<Boolean> uncertainInput = new Input<>("uncertain", "if true, sequence is provided as comma separated probabilities for each character, with sites separated by a semi-colons. In this formulation, gaps are coded as 1/K,...,1/K, where K is the number of states in the model.");

    protected boolean uncertain = false;
    protected double[][] likelihoods = null;    
    public double[][] getLikelihoods() {
    	return likelihoods;
    }
    
    public Sequence() {
    }

    /**
     * Constructor for testing.
     *
     * @param taxon
     * @param sequence
     */
    public Sequence(String taxon, String sequence) {
        taxonInput.setValue(taxon, this);
        dataInput.setValue(sequence, this);
        initAndValidate();
    }

    @Override
    public void initAndValidate() {
    	if (uncertainInput.get() != null)  {
    		uncertain = uncertainInput.get();    		
    		if (uncertain) initProbabilities();    		
    	}
    } // initAndValidate
    
    public void initProbabilities() {
    	   	
    	String data = dataInput.get();
        // remove spaces
        data = data.replaceAll("\\s", "");
        
        String str = data.trim();		
		String[] strs = str.split(";");		
		for (int i=0; i<strs.length; i++) {
			String[] pr = strs[i].split(",");
			//double total = 0;
    		for (int j=0; j<pr.length; j++) {    			
    			if (likelihoods == null) likelihoods = new double[strs.length][pr.length];
    			likelihoods[i][j] = Double.parseDouble(pr[j].trim());
    			//total += likelihoods[i][j]; 
    		}    		
		}
    }

    public List<Integer> getSequence(DataType dataType) {
        
    	List<Integer> sequence;
    	if (uncertain) {
            sequence = new ArrayList<>();
            for (int i=0; i<likelihoods.length; i++) {
            	double m = likelihoods[i][0];
            	int index = 0;
            	for (int j=0; j<likelihoods[i].length; j++) {
            		if (likelihoods[i][j] > m ) {
            			m = likelihoods[i][j];
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
	        sequence = dataType.stringToEncoding(data);
    	}

        if (totalCountInput.get() == null) {
            // derive default from char-map
            totalCountInput.setValue(dataType.getStateCount(), this);
        }
        return sequence;
    }

    /**
     * @return the taxon of this sequence as a string.
     */
    public final String getTaxon() {
        return taxonInput.get();
    }

    /**
     * @return the data of this sequence as a string.
     */
    public final String getData() {
        return dataInput.get();
    }


    int mapCharToData(String dataMap, char c) {
        int i = dataMap.indexOf(c);
        if (i >= 0) {
            return i;
        }
        return dataMap.length();
    } // mapCharToData

    /**
     * @param id of target sequence
     * @param sequences a collection of sequences
     * @return the sequence in the collection with the given ID, or null if its not in the collection.
     */
    public static Sequence getSequenceByTaxon(String id, Collection<Sequence> sequences) {
        for (Sequence seq : sequences) {
            if (seq.getTaxon().equals(id)) return seq;
        }
        return null;
    }

    @Override
	public String toString() {
        return getTaxon() + ":" + getData();
    }


} // class Sequence
