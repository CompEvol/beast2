
/*
 * File TreeParser.java
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
package beast.util;


import java.util.List;
import java.util.Vector;


import beast.evolution.alignment.Alignment;
import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

@Description("Create beast.tree by parsing from a specification of a beast.tree in Newick format " +
		"(includes parsing of any meta data in the Newick string).")
public class TreeParser extends Tree {
	/**
	 * default beast.tree branch length, used when that info is not in the Newick beast.tree
	 **/
	final static double DEFAULT_LENGTH = 0.001f;

	/** labels of leafs **/
	List<String> m_sLabels;
	/** for memory saving, set to true **/
	boolean m_bSurpressMetadata = false;
	/** if there is no translate block. This solves issues where the taxa labels are numbers e.g. in generated beast.tree data **/
	boolean m_bIsLabelledNewick = false;


	public Input<Alignment> m_oData = new Input<Alignment>("taxa", "Specifies the list of taxa represented by leafs in the beast.tree");
	public Input<String> m_oNewick = new Input<String>("newick", "initial beast.tree represented in newick format");
	public Input<String> m_oNodeType = new Input<String>("nodetype", "type of the nodes in the beast.tree", Node.class.getName());
	public Input<Integer> m_nOffset = new Input<Integer>("offset", "offset if numbers are used for taxa (offset=the lowest taxa number) default=1", new Integer(1));
	public Input<Double> m_nThreshold = new Input<Double>("threshold" ,"threshold under wich node heights (derived from lengths) are set to zero. Default=0.", new Double(0));
	/** assure the class behaves properly, even when inputs are not specified **/
	@Override
	public void initAndValidate(State state) throws Exception {
		super.initAndValidate(state);
		m_sLabels = m_oData.get().m_sTaxaNames;
		setRoot(parseNewick(m_oNewick.get()));
	} // init

	public TreeParser() {
	}

	Node newNode() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return (Node) Class.forName(m_oNodeType.get()).newInstance();
		//return new NodeData();
	}
	void processMetadata(Node node) throws Exception {
		if (node.m_sMetaData != null) {
			String [] sMetaData = node.m_sMetaData.split(",");
			for (int i = 0;i < sMetaData.length; i++) {
				try {
					String [] sStrs = sMetaData[i].split("=");
					if (sStrs.length != 2) {
						throw new Exception("misformed meta data '" + node.m_sMetaData + "'. Expected name='value' pairs");
					}
					String sPattern = sStrs[0];
					sStrs[1] = sStrs[1].replaceAll("[\"']", "");
					Double fValue = Double.parseDouble(sStrs[1]);
					node.setMetaData(sPattern, fValue);
				} catch (Exception e) {
					System.err.println("Warning 333: Attempt to parse metadata failed: " + node.m_sMetaData);
				}
			}
		}
		if (node.isLeaf()) {
			node.setID(m_sLabels.get(node.getNr()));
		} else {
			processMetadata(node.m_left);
			processMetadata(node.m_right);
		}
	}

	void convertLengthToHeight(Node node) {
		double fTotalHeight = convertLengthToHeight(node, 0);
		offset(node, -fTotalHeight);
	}
	double convertLengthToHeight(Node node, double fHeight) {
		double fLength = node.getHeight();
		node.setHeight(fHeight - fLength);
		if (node.isLeaf()) {
			return node.getHeight();
		} else {
			double fLeft = convertLengthToHeight(node.m_left, fHeight - fLength);
			double fRight = convertLengthToHeight(node.m_right, fHeight - fLength);
			return Math.min(fLeft, fRight);
		}
	}
	void offset(Node node, double fDelta) {
		node.setHeight(node.getHeight() + fDelta);
		if (node.isLeaf()) {
			if (node.getHeight() < m_nThreshold.get()) {
				node.setHeight(0);
			}
		}
		if (!node.isLeaf()) {
			offset(node.m_left, fDelta);
			offset(node.m_right, fDelta);
		}
	}

	/** Try to map sStr into an index. First, assume it is a number.
	 * If that does not work, look in list of labels to see whether it is there.
	 */
	private int getLabelIndex(String sStr) throws Exception {
		if (!m_bIsLabelledNewick) {
			try {
				return Integer.parseInt(sStr) - m_nOffset.get();
			} catch (Exception e) {
			}
		}
			for (int i = 0; i < m_sLabels.size(); i++) {
				if (sStr.equals(m_sLabels.get(i))) {
					return i;
				}
			}
			throw new Exception("Label '" + sStr + "' in Newick beast.tree could not be identified");
	}


	 char [] m_chars;
	 int m_iTokenStart;
	 int m_iTokenEnd;
	 final static int COMMA = 1;
	 final static int BRACE_OPEN = 3;
	 final static int BRACE_CLOSE = 4;
	 final static int COLON = 5;
	 final static int SEMI_COLON = 8;
	 final static int META_DATA = 6;
	 final static int TEXT = 7;
	 final static int UNKNOWN = 0;

	 int nextToken() {
		 m_iTokenStart = m_iTokenEnd;
		 while (m_iTokenEnd < m_chars.length) {
			 // skip spaces
			 while (m_iTokenEnd < m_chars.length && (m_chars[m_iTokenEnd] == ' ' || m_chars[m_iTokenEnd] == '\t')) {
				 m_iTokenStart++;
				 m_iTokenEnd++;
			 }
			 if (m_chars[m_iTokenEnd] == '(') {
				 m_iTokenEnd++;
				 return BRACE_OPEN;
			 }
			 if (m_chars[m_iTokenEnd] == ':') {
				 m_iTokenEnd++;
				 return COLON;
			 }
			 if (m_chars[m_iTokenEnd] == ';') {
				 m_iTokenEnd++;
				 return SEMI_COLON;
			 }
			 if (m_chars[m_iTokenEnd] == ')') {
				 m_iTokenEnd++;
				 return BRACE_CLOSE;
			 }
			 if (m_chars[m_iTokenEnd] == ',') {
				 m_iTokenEnd++;
				 return COMMA;
			 }
			 if (m_chars[m_iTokenEnd] == '[') {
				 m_iTokenEnd++;
				 while (m_iTokenEnd < m_chars.length && m_chars[m_iTokenEnd-1] != ']') {
					 m_iTokenEnd++;
				 }
				 return META_DATA;
			 }
			 while (m_iTokenEnd < m_chars.length && (m_chars[m_iTokenEnd] != ' ' && m_chars[m_iTokenEnd] != '\t'
				 && m_chars[m_iTokenEnd] != '('  && m_chars[m_iTokenEnd] != ')'  && m_chars[m_iTokenEnd] != '['
					 && m_chars[m_iTokenEnd] != ':'&& m_chars[m_iTokenEnd] != ','&& m_chars[m_iTokenEnd] != ';')) {
				 m_iTokenEnd++;
			 }
			 return TEXT;
		 }
		 return UNKNOWN;
	 }

	 public Node parseNewick(String sStr) throws Exception {
		 // get rid of initial and terminal spaces
		 sStr = sStr.replaceAll("^\\s+", "");
		 sStr = sStr.replaceAll("\\s+$", "");

		 try {
		m_chars = sStr.toCharArray();
		if (sStr == null || sStr.length() == 0) {
			return null;
		}
		m_iTokenStart = 0;
		m_iTokenEnd = 0;
		Vector<Node> stack = new Vector<Node>();
		Vector<Boolean> isFirstChild =  new Vector<Boolean>();
		stack.add(newNode());
		isFirstChild.add(true);
		stack.lastElement().setHeight(DEFAULT_LENGTH);
		boolean bIsLabel = true;
		while (m_iTokenEnd < m_chars.length) {
			switch (nextToken()) {
			case BRACE_OPEN:
			{
				Node node2 = newNode();
				node2.setHeight(DEFAULT_LENGTH);
				stack.add(node2);
				isFirstChild.add(true);
				bIsLabel = true;
			}
				break;
			case BRACE_CLOSE:
			{
				if (isFirstChild.lastElement()) {
					// don't know how to process single child nodes
					throw new Exception("Node with single child found.");
				}
				// process multi(i.e. more than 2)-child nodes by pairwise merging.
				while (isFirstChild.get(isFirstChild.size()-2) == false) {
					Node right = stack.lastElement();
					stack.remove(stack.size()-1);
					isFirstChild.remove(isFirstChild.size()-1);
					Node left = stack.lastElement();
					stack.remove(stack.size()-1);
					isFirstChild.remove(isFirstChild.size()-1);
					Node dummyparent = newNode();
					dummyparent.setHeight(DEFAULT_LENGTH);
					dummyparent.m_left = left;
					left.setParent(dummyparent);
					dummyparent.m_right = right;
					right.setParent(dummyparent);
					stack.add(dummyparent);
					isFirstChild.add(false);
				}
				// last two nodes on stack merged into single parent node
				Node right = stack.lastElement();
				stack.remove(stack.size()-1);
				isFirstChild.remove(isFirstChild.size()-1);
				Node left = stack.lastElement();
				stack.remove(stack.size()-1);
				isFirstChild.remove(isFirstChild.size()-1);
				Node parent = stack.lastElement();
				parent.m_left = left;
				left.setParent(parent);
				parent.m_right = right;
				right.setParent(parent);
			}
				break;
			case COMMA:
			{
				Node node2 = newNode();
				node2.setHeight(DEFAULT_LENGTH);
				stack.add(node2);
				isFirstChild.add(false);
				bIsLabel = true;
			}
				break;
			case COLON:
				bIsLabel = false;
				break;
			case TEXT:
				if (bIsLabel) {
					String sLabel = sStr.substring(m_iTokenStart, m_iTokenEnd);
					stack.lastElement().setNr(getLabelIndex(sLabel));
				} else {
					String sLength = sStr.substring(m_iTokenStart, m_iTokenEnd);
					stack.lastElement().setHeight(Double.parseDouble(sLength));
				}
				break;
			case META_DATA:
				if (stack.lastElement().m_sMetaData == null) {
					stack.lastElement().m_sMetaData = sStr.substring(m_iTokenStart+1, m_iTokenEnd-1);
				} else {
					stack.lastElement().m_sMetaData += " " +sStr.substring(m_iTokenStart+1, m_iTokenEnd-1);
				}
				break;
			case SEMI_COLON:
				//System.err.println(stack.lastElement().toString());
				Node tree = stack.lastElement();
				tree.sort();
				// at this stage, all heights are actually lengths
				convertLengthToHeight(tree);
				int n = tree.getNodeCount();
				tree.labelInternalNodes((n+1)/2);
				if (!m_bSurpressMetadata) {
					processMetadata(tree);
				}
				return stack.lastElement();
			default:
				throw new Exception("parseNewick: unknown token");
			}
		}
		Node tree = stack.lastElement();
		tree.sort();
		// at this stage, all heights are actually lengths
		convertLengthToHeight(tree);
		int n = tree.getNodeCount();
		tree.labelInternalNodes((n+1)/2);
		if (!m_bSurpressMetadata) {
			processMetadata(tree);
		}
		return tree;
		 } catch (Exception e) {
			 System.err.println(e.getMessage() + ": " + sStr.substring(Math.max(0, m_iTokenStart-100), m_iTokenStart) + " >>>" + sStr.substring(m_iTokenStart, m_iTokenEnd) + " <<< ...");
			 throw new Exception(e.getMessage() + ": " + sStr.substring(Math.max(0, m_iTokenStart-100), m_iTokenStart) + " >>>" + sStr.substring(m_iTokenStart, m_iTokenEnd) + " <<< ...");
		 }
		//return node;
	 }

} // class TreeParser
