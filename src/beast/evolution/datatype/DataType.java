package beast.evolution.datatype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.core.Description;
import beast.core.Plugin;

public interface DataType {
	final static public char GAP_CHAR = '-';
	final static public char MISSING_CHAR = '?';
	
	/** @return number of states for this data type.
	 * Assuming there is a finite number of states, or -1 otherwise.  
	 */
	int getStateCount();
	
	/** Convert a sequence represented by a string into a sequence of integers 
	 * representing the state for this data type.
	 * Ambiguous states should be represented by integer numbers higher than getStateCount()
	 * throws exception when parsing error occur **/
	List<Integer> string2state(String sSequence) throws Exception;
	
	/** Convert an array of states into a sequence represented by a string.
	 * This is the inverse of string2state() 
	 * throws exception when State cannot be mapped **/
	String state2string(List<Integer> nStates) throws Exception;
	String state2string(int [] nStates) throws Exception;
	
    /**
     * returns an array of length getStateCount() containing the (possibly ambiguous) states 
     * that this state represents.
     */
    public boolean[] getStateSet(int iState);
    
    /** returns an array with all non-ambiguous states represented by
     * a state.
     */
    public int [] getStatesForCode(int iState);

    boolean isAmbiguousState(int state);
    
    /** true if the class is completely self contained and does not need any 
     * further initialisation. Notable exception: GeneralDataype
     */
    boolean isStandard();

    /** data type description, e.g. nucleotide, codon **/
    public String getDescription();
    
    @Description(value="Base class bringing class and interfaces together", isInheritable=false)
    public abstract class Base extends Plugin implements DataType {
    	/** size of the state space **/
    	int m_nStateCount;

    	/** maps string encoding to state codes **/
    	String m_sCodeMap;
    	public String getCodeMap() {return m_sCodeMap;}
    	
    	/** length of the encoding, e.g. 1 for nucleotide, 3 for codons **/
    	int m_nCodeLength;
    	
    	/** mapping codes to sets of states **/
    	int [][] m_mapCodeToStateSet;

    	@Override
    	public void initAndValidate() throws Exception {
    		if (m_mapCodeToStateSet != null) {
    			if (m_mapCodeToStateSet.length != m_sCodeMap.length() / m_nCodeLength) {
    				throw new Exception("m_sCodeMap and m_mapCodeToStateSet have incompatible lengths");
    			}
    		}
    	}
    	
    	@Override
    	public int getStateCount() {
    		return m_nStateCount;
    	}

    	/** implementation for single character per state encoding **/
    	@Override
    	public List<Integer> string2state(String sData) throws Exception {
            List<Integer> sequence;
            sequence = new ArrayList<Integer>();
            // remove spaces
            sData = sData.replaceAll("\\s", "");
            sData = sData.toUpperCase();
            if (m_sCodeMap == null) {
                if (sData.contains(",")) {
                	// assume it is a comma separated string of integers
                	String[] sStrs = sData.split(",");
                	for (String sStr : sStrs) {
                		sequence.add(Integer.parseInt(sStr));
                	}
                } else {
                	// assume it is a string where each character is a state
                	for (byte c: sData.getBytes()) {
                		sequence.add(Integer.parseInt((char)c+""));
                	}
                }
            } else {
            	if (m_nCodeLength == 1) {
            		// single character codes
            		for (int i = 0; i < sData.length(); i ++) {
            			char cCode = sData.charAt(i);
            			int nState = m_sCodeMap.indexOf(cCode);
            			if (nState < 0) {
            				throw new Exception("Unknown code found in sequence: " + cCode);
            			}
                		sequence.add(nState);
            		}
            	} else	if (m_nCodeLength > 1) {
            		// multi-character codes of fixed length
            		
	            	// use code map to resolve state codes
	            	Map<String, Integer> map = new HashMap<String, Integer>();
            		// fixed length code
	            	for (int i = 0; i < m_sCodeMap.length(); i+=m_nCodeLength) {
	            		String sCode = m_sCodeMap.substring(i, i + m_nCodeLength);
	            		map.put(sCode, i/m_nCodeLength);
	            	}
	            	
	            	for (int i = 0; i < sData.length(); i += m_nCodeLength) {
	            		String sCode = sData.substring(i, i + m_nCodeLength).toUpperCase();
	            		if (map.containsKey(sCode)) {
	                		sequence.add(map.get(sCode));
	            		} else {
	        				throw new Exception("Unknown code found in sequence: " + sCode);
	            		}            		
	            	}
	            } else {
            		// variable length code of strings
            		String [] sCodes = m_sCodeMap.toUpperCase().split(",");
            		for (String sCode : sData.split(",")) {
            			boolean bFound = false;
            			for (int iCode = 0; iCode < sCodes.length - 1; iCode++) {
            				if (sCode.equals(sCodes[iCode])) {
                    			sequence.add(iCode);
                    			bFound = true;
                    			break;
            				}
            			}
            			if (!bFound) {
            				throw new Exception("Could not find code " + sCode + " in codemap");
            			}
            		}
            	}
            }
            return sequence;
    	} // string2state

    	@Override
    	public String state2string(List<Integer> nStates) {
    		int [] nStates2 = new int[nStates.size()];
    		for (int i = 0; i < nStates2.length; i++) {
    			nStates2[i] = nStates.get(i);
    		}
    		return state2string(nStates2);
    	}
    	
    	/** implementation for single character per state encoding **/
    	@Override
       	public String state2string(int [] nStates) {
    		StringBuffer buf = new StringBuffer();
    		if (m_sCodeMap != null) {
    			for (int iState : nStates) {
    				String sCode = m_sCodeMap.substring(iState * m_nCodeLength, iState * m_nCodeLength + m_nCodeLength);
    				buf.append(sCode);
    			}
    		} else {
    			// produce a comma separated string of integers
    			for (int i = 0; i < nStates.length - 1; i++) {
    				buf.append(nStates[i] + ",");
    			}    			
				buf.append(nStates[nStates.length-1] + "");
    		}
    		return buf.toString();
    	} // state2string
    	
    	
    	@Override
    	public int[] getStatesForCode(int iState) {
    		return m_mapCodeToStateSet[iState];
    	}
    	
    	@Override
        public boolean[] getStateSet(int state) {
            boolean[] stateSet = new boolean[m_nStateCount];
        	int [] stateNumbers = getStatesForCode(state);
        	for (int i : stateNumbers) {
        		stateSet[i] = true;
        	}
            return stateSet;
        } // getStateSet
    	
    	@Override
        public boolean isAmbiguousState(int state) {
        	return (state < 0 && state >= m_nStateCount);
        }

    	@Override
        public boolean isStandard() {
    		return true;
    	}
    	
    	@Override
    	public String toString() {
    		return getDescription();
    	}
    } // class Base

} // class DataType
