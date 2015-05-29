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

import java.util.Collection;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.BEASTObject;
import beast.evolution.datatype.DataType;

@Description("Single sequence in an alignment.")
public class Sequence extends BEASTObject {
    public Input<Integer> totalCountInput = new Input<Integer>("totalcount", "number of states or the number of lineages for this species in SNAPP analysis");
    public Input<String> taxonInput = new Input<String>("taxon", "name of this species", Input.Validate.REQUIRED);
    public Input<String> dataInput = new Input<String>("value",
            "sequence data, either encoded as a string or as comma separated list of integers." +
                    "In either case, whitespace is ignored.", Input.Validate.REQUIRED);

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
    } // initAndValidate

    public List<Integer> getSequence(DataType dataType) throws Exception {
        String data = dataInput.get();
        // remove spaces
        data = data.replaceAll("\\s", "");
        List<Integer> sequence = dataType.string2state(data);

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

    public String toString() {
        return getTaxon() + ":" + getData();
    }


} // class Sequence
