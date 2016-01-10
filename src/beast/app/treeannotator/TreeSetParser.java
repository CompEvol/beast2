/*

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
/*
 * TreeFileParser.java
 * Copyright Remco Bouckaert remco@cs.auckland.ac.nz (C) 2011 
*/
package beast.app.treeannotator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import beast.core.util.Log;
import beast.evolution.tree.Node;

public class TreeSetParser {
	/**
	 * default tree branch length, used when that info is not in the Newick tree
	 **/
	final static float DEFAULT_LENGTH = 0.001f;

	int m_nOffset = 0;
	/** labels of leafs **/
	List<String> m_sLabels;
	/** position information for the leafs (if available) **/
	List<Float> m_fLongitude;
	List<Float> m_fLatitude;
	/** extreme values for position information **/
	float m_fMaxLong, m_fMaxLat, m_fMinLong, m_fMinLat;
	/** nr of labels in dataset **/
	int m_nNrOfLabels;
	/** burn in = nr of trees ignored at the start of tree file, can be set by command line option **/
	int m_nBurnInPercentage = 0;
	//DensiTree m_densiTree;
	/** for memory saving, set to true **/
	boolean m_bSurpressMetadata = true;
	/** if there is no translate block. This solves issues where the taxa labels are numbers e.g. in generated tree data **/
	boolean m_bIsLabelledNewick = false;
	/** flag to indicate that single child nodes are allowed **/
	boolean m_bAllowSingleChild = false;
	
	public TreeSetParser(int burnInPercentage, boolean allowSingleChild) {
		m_sLabels = new ArrayList<>();
		m_fLongitude = new ArrayList<>();
		m_fLatitude = new ArrayList<>();
		m_nBurnInPercentage = Math.max(burnInPercentage, 0);
		m_fMinLat = 90; m_fMinLong = 180;
		m_fMaxLat = -90; m_fMaxLong = -180;
		m_bAllowSingleChild = allowSingleChild;
	} // c'tor
	
	public TreeSetParser(List<String> labels, List<Float> longitude, List<Float> latitude, int burnInPercentage) {
		m_sLabels = labels;
		if (m_sLabels != null) {
			m_bIsLabelledNewick = true;
			m_nNrOfLabels = m_sLabels.size();
		}
		m_fLongitude = longitude;
		m_fLatitude = latitude;
		m_nBurnInPercentage = Math.max(burnInPercentage, 0);
		m_fMinLat = 90; m_fMinLong = 180;
		m_fMaxLat = -90; m_fMaxLong = -180;
	}
	
	long fileStep;
	long fileRead = 0;
	long fileMarked = 0;

	
	public Node [] parseFile(String fileName) throws Exception {
		//List<String> newickTrees = new List<>();
		List<Node> trees = new ArrayList<>();
		m_nOffset = 0;
		// parse Newick tree file
		File file = new File(fileName);
		fileStep = Math.max(file.length() / 61, 1);
		fileRead = 0;
		fileMarked = 0;
		
		BufferedReader fin = new BufferedReader(new FileReader(fileName));
		
        int nrOfTrees = 0;
        // first, sweep through the log file to determine the number of trees
        while (fin.ready()) {
            if (fin.readLine().toLowerCase().startsWith("tree ")) {
            	nrOfTrees++;
            }
        }
        fin.close();
        
        fin = new BufferedReader(new FileReader(fileName));
		String str = readLine(fin);
		// grab translate block
		while (fin.ready() && str.toLowerCase().indexOf("translate") < 0) {
			str = readLine(fin);
		}
		m_bIsLabelledNewick = false;
		m_nNrOfLabels = m_sLabels.size();
		boolean addLabels = (m_nNrOfLabels == 0);
		if (str.toLowerCase().indexOf("translate") < 0) {
			m_bIsLabelledNewick = true;
			// could not find translate block, assume it is a list of Newick trees instead of Nexus file
			fin.close();
			fileRead = 0;
			fileMarked = 0;
			fin = new BufferedReader(new FileReader(fileName));
			while (fin.ready() && m_nNrOfLabels == 0) {
				str = readLine(fin);
				fileRead += str.length();
				if (str.length() > 2 && str.indexOf("(") >= 0) {
					String str2 = str;
					str2 = str2.substring(str2.indexOf("("));
					while (str2.indexOf('[') >= 0) {
						int i0 = str2.indexOf('[');
						int i1 = str2.indexOf(']');
						str2 = str2.substring(0, i0) + str2.substring(i1 + 1);
					}
					str2 = str2.replaceAll("[;\\(\\),]"," ");
					str2 = str2.replaceAll(":[0-9\\.Ee-]+"," ");
					String [] labels = str2.split("\\s+");
					if (addLabels) {
						m_nNrOfLabels = 0;
						for (int i = 0; i < labels.length; i++) {
							if (labels[i].length() > 0) {
									m_sLabels.add(labels[i]);
								m_nNrOfLabels++;
							}
						}
					}
					Node tree = parseNewick(str);
					tree.sort();
					tree.labelInternalNodes(m_nNrOfLabels);
					trees.add(tree);
//					newickTrees.add(str);
				}
			}

			while (fin.ready()) {
				str = readLine(fin);
				if (str.length() > 2 && str.indexOf("(") >= 0) {
					Node tree = parseNewick(str);
					tree.sort();
					tree.labelInternalNodes(m_nNrOfLabels);
					trees.add(tree);
					if (trees.size() % 100 ==0) {if (m_nNrOfLabels>=100||trees.size() % 1000 ==0) {Log.warning.print(trees.size() + " ");}}
//					newickTrees.add(str);
				}
			}
			
		} else {
			// read tree set from file, and store in individual strings
			str = readLine(fin);
			//m_nNrOfLabels = 0;
			boolean isLastLabel = false;
			while (fin.ready() && !isLastLabel) {
				if (str.indexOf(";") >= 0) {
					str = str.replace(';',' ');
					str = str.trim();
					if (str.isEmpty()) {
						break;
					}
					isLastLabel = true;
				}
				str = str.replaceAll(",", "");
				str = str.replaceAll("^\\s+", "");
				String[] strs = str.split("\\s+");
				int labelIndex = new Integer(strs[0]).intValue();
				String label = strs[1];
				if (m_sLabels.size() < labelIndex) {
					//m_sLabels.add("__dummy__");
					m_nOffset = 1;
				}
				// check if there is geographic info in the name
				if (label.contains("(")) {
					int strIndex = label.indexOf('(');
					int str2 = label.indexOf('x', strIndex);
					if (str2 >= 0) {
						int str3 = label.indexOf(')', str2);
						if (str3 >= 0) {
							float lat = Float.parseFloat(label.substring(strIndex+1, str2));// + 180;
							float _long = Float.parseFloat(label.substring(str2+1, str3));// + 360)%360;
							if (lat!=0 || _long!=0) {
								m_fMinLat = Math.min(m_fMinLat, lat);
								m_fMaxLat = Math.max(m_fMaxLat, lat);
								m_fMinLong = Math.min(m_fMinLong, _long);
								m_fMaxLong = Math.max(m_fMaxLong, _long);
							}
							while (m_fLatitude.size() < m_sLabels.size()) {
								m_fLatitude.add(0f);
								m_fLongitude.add(0f);
							}
							m_fLatitude.add(lat);
							m_fLongitude.add(_long);
						}
					}
					label = label.substring(0, label.indexOf("("));
				}
				if (addLabels) {
					m_sLabels.add(label);
					m_nNrOfLabels++;
				}
				if (!isLastLabel) {
					str = readLine(fin);
				}
			}
			
			// read trees
			// read trees
            int burnIn = m_nBurnInPercentage * nrOfTrees / 100;
            //int k = 0;                    
            while (fin.ready()) {
                    str = readLine(fin);
                    str = str.trim();
                    if (str.length() > 5) {
                            String tree = str.substring(0,5);
                            if (tree.toLowerCase().startsWith("tree ")) {
                                    //k++;
                                    if (burnIn <= 0) {
                                            int i = str.indexOf('(');
                                            if (i > 0) {
                                                    str = str.substring(i);
                                            }
                                            Node treeRoot = parseNewick(str);
                                            treeRoot.sort();
                                            treeRoot.labelInternalNodes(m_nNrOfLabels);
                                            trees.add(treeRoot);
                                            //if (trees.size() % 100 ==0) {if (m_nNrOfLabels>=100||trees.size() % 1000 ==0) {Log.warning.print(trees.size() + " ");}}
                                    } else {
                                            burnIn--;
                                    }
                            }
                    }
            }
            fin.close();
		}
		
		// discard burn-in percentage
//		int burnIn = m_nBurnInPercentage * trees.size() / 100;
//		for (int i = 0; i < burnIn; i++) {
//			trees.remove(i);
//		}
		
		
		// convert lengths (stored as node heights) to heights
		double maxHeight = 0;
		double [] heights = new double[trees.size()];
		for (int i = 0; i < trees.size(); i++) {
			heights[i] = lengthToHeight(trees.get(i), 0);
			maxHeight = Math.max(maxHeight, heights[i]);
		}
		for (int i = 0; i < trees.size(); i++) {
			offsetHeight(trees.get(i), heights[i]);
		}

		Log.warning.println();
		//System.err.println("Geo: " +m_fMinLong + "x" + m_fMinLat + " " + m_fMaxLong + "x" + m_fMaxLat);
		return trees.toArray(new Node[1]);
	} // parseFile

	
	int k = 0;
	private String readLine(BufferedReader fin) throws IOException {
		String s = fin.readLine();
		fileRead += s.length();
		if (fileRead > fileMarked - 10) {
			Log.warning.print("*");
			fileMarked += fileStep;
			k++;
		}
//		System.err.println(fileRead + " " + fileMarked + " " + k);
		return s;
	}

	/** move y-position of a tree with offset f **/
	public void offsetHeight(Node node, double f) {
		if (!node.isLeaf()) {
			offsetHeight(node.getLeft(), f);
			if (node.getRight() != null) {
				offsetHeight(node.getRight(), f);
			}
		}
		node.setHeight(node.getHeight() + f);
	}

	/** convert length to height
	 *  and set ID of leafs
	 */
	private double lengthToHeight(Node node, double offSet) {
		if (node.isLeaf()) {
			node.setHeight(-offSet - node.getHeight());
			node.setID(m_sLabels.get(node.getNr()));
			return -node.getHeight();
		} else {
			double posY = offSet + node.getHeight();
			double yMax = 0;
			yMax = Math.max(yMax, lengthToHeight(node.getLeft(), posY));
			if (node.getRight() != null) {
				yMax = Math.max(yMax, lengthToHeight(node.getRight(), posY));
			}
			node.setHeight(-posY);
			return yMax;
		}
	}

	/** Try to map str into an index. First, assume it is a number.
	 * If that does not work, look in list of labels to see whether it is there.
	 */
	private int getLabelIndex(String str) throws Exception {
		if (!m_bIsLabelledNewick) {
			try {
				return Integer.parseInt(str) - m_nOffset;
			} catch (Exception e) {
			}
		}
		for (int i = 0; i < m_nNrOfLabels; i++) {
			if (str.equals(m_sLabels.get(i))) {
				return i;
			}
		}
		// str may have (double) qoutes missing
		for (int i = 0; i < m_nNrOfLabels; i++) {
			String label = m_sLabels.get(i);
			if (label.startsWith("'") && label.endsWith("'") ||
					label.startsWith("\"") && label.endsWith("\"")) {
				label = label.substring(1, label.length()-1);
				if (str.equals(label)) {
					return i;
				}
			}
		}
		// str may have extra (double) qoutes
		if (str.startsWith("'") && str.endsWith("'") ||
				str.startsWith("\"") && str.endsWith("\"")) {
			str = str.substring(1, str.length()-1);
			return getLabelIndex(str);
		}
		throw new IllegalArgumentException("Label '" + str + "' in Newick tree could not be identified");
	}
	

	 double height(Node node) {
		 if (node.isLeaf()) {
			 return node.getLength();
		 } else {
			 return node.getLength() + Math.max(height(node.getLeft()), height(node.getRight()));
		 }
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

	 public Node parseNewick(String str) throws Exception {
		 try {
		if (str == null || str.length() == 0) {
			return null;
		}
		
		m_chars = str.toCharArray();
		m_iTokenStart = str.indexOf('(');
		if (m_iTokenStart < 0) {
			return null;
		}
		m_iTokenEnd = m_iTokenStart;
		Vector<Node> stack = new Vector<>();
		Vector<Boolean> isFirstChild =  new Vector<>();
		Vector<String> metaDataString =  new Vector<>();
		stack.add(new Node());
		isFirstChild.add(true);
		stack.lastElement().setHeight(DEFAULT_LENGTH);
		metaDataString.add(null);
		boolean isLabel = true;
		while (m_iTokenEnd < m_chars.length) {
			switch (nextToken()) {
			case BRACE_OPEN:
			{
				Node node2 = new Node();
				node2.setHeight(DEFAULT_LENGTH);
				stack.add(node2);
				isFirstChild.add(true);
				metaDataString.add(null);
				isLabel = true;
			}
				break;
			case BRACE_CLOSE:
			{
				if (isFirstChild.lastElement()) {
					if (m_bAllowSingleChild) {
						// process single child nodes
						Node left = stack.lastElement();
						stack.remove(stack.size()-1);
						isFirstChild.remove(isFirstChild.size()-1);
						Node dummyparent = new Node();
						dummyparent.setHeight(DEFAULT_LENGTH);
						dummyparent.setLeft(left);
						left.setParent(dummyparent);
						dummyparent.setRight(null);
						Node parent = stack.lastElement();
						parent.setLeft(left);
						left.setParent(parent);
						String metaData = metaDataString.remove(metaDataString.size() - 1);
						left.metaDataString = metaData;
						parseMetaData(left, metaData);
						break;
					} else {
						// don't know how to process single child nodes
						throw new IllegalArgumentException("Node with single child found.");
					}
				}
				// process multi(i.e. more than 2)-child nodes by pairwise merging.
				while (isFirstChild.elementAt(isFirstChild.size()-2) == false) {
					Node right = stack.lastElement();
					stack.remove(stack.size()-1);
					isFirstChild.remove(isFirstChild.size()-1);
					Node left = stack.lastElement();
					stack.remove(stack.size()-1);
					isFirstChild.remove(isFirstChild.size()-1);
					Node dummyparent = new Node();
					dummyparent.setHeight(DEFAULT_LENGTH);
					dummyparent.setLeft(left);
					left.setParent (dummyparent);
					dummyparent.setRight(right);
					right.setParent(dummyparent);
					stack.add(dummyparent);
					isFirstChild.add(false);
					String metaData = metaDataString.remove(metaDataString.size() - 1);
					parseMetaData(left, metaData);
				}
				// last two nodes on stack merged into single parent node 
				Node right = stack.lastElement();
				stack.remove(stack.size()-1);
				isFirstChild.remove(isFirstChild.size()-1);
				String metaData = metaDataString.remove(metaDataString.size() - 1);
				parseMetaData(right, metaData);

				Node left = stack.lastElement();
				stack.remove(stack.size()-1);
				isFirstChild.remove(isFirstChild.size()-1);
				metaData = metaDataString.remove(metaDataString.size() - 1);
				parseMetaData(left, metaData);

				Node parent = stack.lastElement();
				parent.setLeft(left);
				left.setParent(parent);
				parent.setRight(right);
				right.setParent(parent);
				metaData = metaDataString.lastElement();
				parseMetaData(parent, metaData);
			}
				break;
			case COMMA:
			{
				Node node2 = new Node();
				node2.setHeight(DEFAULT_LENGTH);
				stack.add(node2);
				isFirstChild.add(false);
				metaDataString.add(null);
				isLabel = true;
			}
				break;
			case COLON:
				isLabel = false;
				break;
			case TEXT:
				if (isLabel) {
					String label = str.substring(m_iTokenStart, m_iTokenEnd);
					stack.lastElement().setNr(getLabelIndex(label)); 
				} else {
					String length = str.substring(m_iTokenStart, m_iTokenEnd);
					stack.lastElement().setHeight(Float.parseFloat(length)); 
				}
				break;
			case META_DATA:
				if (metaDataString.lastElement() == null) {
					metaDataString.set(metaDataString.size()-1, str.substring(m_iTokenStart+1, m_iTokenEnd-1));
				} else {
					metaDataString.set(metaDataString.size()-1, metaDataString.lastElement() 
					 + ("," +str.substring(m_iTokenStart+1, m_iTokenEnd-1)));
				}
				break;
			case SEMI_COLON:
				//System.err.println(stack.lastElement().toString());
				parseMetaData(stack.lastElement(), metaDataString.lastElement());
				return stack.lastElement();
			default:
				throw new IllegalArgumentException("parseNewick: unknown token");	
			}
		}
		return stack.lastElement();
		 } catch (Exception e) {
			 e.printStackTrace();
			 throw new IllegalArgumentException(e.getMessage() + ": " + str.substring(Math.max(0, m_iTokenStart-100), m_iTokenStart) + " >>>" + str.substring(m_iTokenStart, m_iTokenEnd) + " <<< ..."); 
		 }
		//return node;
	 }
	 
	 
		public void parseMetaData(Node node, String metaDataString) {
			node.metaDataString = metaDataString;
			if (metaDataString == null) {
				return;
			}
			// parse by key=value pairs
			int i = 0;
			int start = 1;
			try {
				while ((i = metaDataString.indexOf('=', i)) >= 0) {
					String key = metaDataString.substring(start, i).trim();
					String value = null;
					int k = 0;
					if ((k = metaDataString.indexOf('=', i+1)) >= 0) {
						int j = metaDataString.lastIndexOf(',', k);
						value = metaDataString.substring(i + 1, j);
						start = j + 1;
					} else {
						value = metaDataString.substring(i+1);
					}
					if (value.length() > 0 && value.charAt(0) != '{') {
						try {
							Double dvalue = Double.parseDouble(value);
							node.setMetaData(key, dvalue);
							
						} catch (Exception e) {
							node.setMetaData(key, value);
						}
					} else 	if (value.length() > 0 && value.charAt(0) == '{' && value.charAt(value.length() - 1) == '}') {
						try {
							String str = value.substring(1, value.length() - 1); 
							String [] strs = str.split(",");
							Double [] values = new Double[strs.length];
							for (int j = 0; j < strs.length; j++) {
								values[j] = Double.parseDouble(strs[j]); 
							}
							node.setMetaData(key, values);
						} catch (Exception e) {
							node.setMetaData(key, value);
						}
					} else {
						node.setMetaData(key, value);
					}
					i++;
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

} // class TreeFileParser
