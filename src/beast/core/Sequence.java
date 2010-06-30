
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
package beast.core;

import java.util.List;
import java.util.ArrayList;

@Description("Single sequence in an allignment.")
public class Sequence extends Plugin {
	public Input<Integer> m_nTotalCount = new Input<Integer>("totalcount", "number of lineages for this species");
	public Input<String> m_sTaxon = new Input<String>("taxon", "name of this species", Input.Validate.REQUIRED);
	public Input<String> m_sData = new Input<String>("value", "sequence data", Input.Validate.REQUIRED);

	@Override
	public void initAndValidate(State state) throws Exception {
	} // initAndValidate


	List<Integer> getSequence(String sDataMap) throws Exception {
		List<Integer> sequence;
		sequence = new ArrayList<Integer>();
		String sData = m_sData.get();
		// remove spaces
		sData = sData.replaceAll("\\s", "");
		if (sDataMap == null) {
			String [] sStrs = sData.split(",");
			for (String sStr : sStrs) {
				sequence.add(Integer.parseInt(sStr));
			}
		} else {
			for (int i = 0; i < sData.length(); i++) {
				char c = sData.charAt(i);
				sequence.add(mapCharToData(sDataMap, c));
			}
		}
		if (m_nTotalCount.get() == null && sDataMap != null) {
			// derive default from char-map
			m_nTotalCount.setValue(sDataMap.length(), this);
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
