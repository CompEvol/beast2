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

import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.datatype.DataType;

import java.util.List;

@Description("Single sequence in an allignment.")
public class Sequence extends Plugin {
    public Input<Integer> m_nTotalCount = new Input<Integer>("totalcount", "number of lineages for this species");
    public Input<String> m_sTaxon = new Input<String>("taxon", "name of this species", Input.Validate.REQUIRED);
    public Input<String> m_sData = new Input<String>("value",
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
        m_sTaxon.setValue(taxon, this);
        m_sData.setValue(sequence, this);
        initAndValidate();
    }


    @Override
    public void initAndValidate() throws Exception {
    } // initAndValidate

    public List<Integer> getSequence(DataType dataType) throws Exception {
        String sData = m_sData.get();
        // remove spaces
        sData = sData.replaceAll("\\s", "");
        List<Integer> sequence = dataType.string2state(sData);

        if (m_nTotalCount.get() == null) {
            // derive default from char-map
            m_nTotalCount.setValue(dataType.getStateCount(), this);
        }
        return sequence;
    }

    int mapCharToData(String sDataMap, char c) {
        int i = sDataMap.indexOf(c);
        if (i >= 0) {
            return i;
        }
        return sDataMap.length();
    } // mapCharToData

} // class Sequence
