package beast.evolution.datatype;

import beast.core.BEASTObject;
import beast.core.Description;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public interface DataType {
	final static public char GAP_CHAR = '-';
	final static public char MISSING_CHAR = '?';

	/**
	 * NOMENCLATURE A data type is a tool to map between potentially four
	 * different representations of a, possibly ambiguous, sequence: The
	 * SEQUENCE STRING, for example "AGCTR?" (a String); the ENCODING CHAIN, for
	 * example { 0, 2, 1, 3, 5, 18 } (a Collection<Integer>, mostly used
	 * internally); the STATE LIST, for example { {0}, {2}, {1}, {3}, {0, 2},
	 * {0, 1, 2, 3} } (a Sequence<Collection<Integer>>); and the STATE SET
	 * ARRAY, for example { {true, false, false, false}, {true, true, true,
	 * true} } (a Sequence<bool[]>, only available for finite state count data
	 * types)
	 * 
	 * The CHARACTER "R" corresponds to the CODE 5 and the STATE LIST {0, 2},
	 * which contains the STATES 0 and 2 and can be expressed as a boolean array
	 * {true, false, true, false}
	 */

	/**
	 * @return number of states for this data type. Assuming there is a finite
	 *         number of states, or -1 otherwise.
	 */
	int getStateCount();

	/**
	 * Convert a sequence represented by a string into a sequence of integers
	 * representing the encoding chain for this data type. Ambiguous characters
	 * are represented by integer numbers less than 0 or greater or equal to
	 * getStateCount().
	 * 
	 * @throws IllegalArgumentException
	 *             when parsing error occurs
     *
     * @deprecated Use stringToEncoding() instead.
	 */
	@Deprecated
	List<Integer> string2state(String sequence) throws IllegalArgumentException;

    /**
     * Convert a sequence represented by a string into a sequence of integers
     * representing the encoding chain for this data type. Ambiguous characters
     * are represented by integer numbers less than 0 or greater or equal to
     * getStateCount().
     *
     * @throws IllegalArgumentException
     *             when parsing error occurs
     */
	List<Integer> stringToEncoding(String sequence) throws IllegalArgumentException;

	/**
	 * Convert an encoding chain into a sequence string. This is the inverse of
	 * stringToEncoding() throws IllegalArgumentException when a code cannot be
	 * mapped
     *
     * @deprecated Use encodingToString() instead
	 */
	@Deprecated
	String state2string(List<Integer> encoding);

	/**
	 * Convert an encoding chain into a sequence string. This is the inverse of
	 * stringToEncoding() throws IllegalArgumentException when a code cannot be
	 * mapped *
	 */
	String encodingToString(List<Integer> encoding);

	/**
	 * Convert an encoding chain into a sequence string. This is the inverse of
	 * stringToEncoding() throws IllegalArgumentException when a code cannot be
	 * mapped
     *
     * @deprecated Use encodingToString() instead.
	 */
	@Deprecated
	String state2string(int[] encoding);

	/**
	 * Convert an encoding chain into a sequence string. This is the inverse of
	 * stringToEncoding() throws IllegalArgumentException when a code cannot be
	 * mapped *
	 */
	String encodingToString(int[] encoding);

	/**
	 * returns an array of length getStateCount() containing the (possibly
	 * ambiguous) states that this code represents.
	 */
	public boolean[] getStateSet(int code);

	/**
	 * returns the state list for a code.
	 */
	public int[] getStatesForCode(int code);

	/**
	 * Returns true if the code corresponds to more than one state, false
	 * otherwise.
     *
     * @deprecated Use isAmbiguousCode instead.
	 */
	@Deprecated
	boolean isAmbiguousState(int code);

	/**
	 * Returns true if the code corresponds to more than one state, false
	 * otherwise.
	 */
	boolean isAmbiguousCode(int code);

	/**
	 * true if the class is completely self contained and does not need any
	 * further initialisation.
	 */
	// FIXME: This is VERY confusing, because StandardData is **not**
	// isStandard()==true
	boolean isStandard();

	/**
	 * data type description, e.g. nucleotide, codon *
	 */
	public String getTypeDescription();

	/**
	 * Get character (as data type char, which is not always possible)
	 * corresponding to a given code
	 */
	@Deprecated
	public char getChar(int code);

	/**
	 * Get character corresponding to a given code
	 */
	@Deprecated
	public String getCode(int code);

	public String getCharacter(int code);
	

	/**
	 * Usually, alignments have codes with constant lengths,
	 * but occasionally (like for integer values, or user defined data types) they don't.
	 * This information can be useful for sanity checks.  
	 * @return true if code length is constant
	 */
	default public boolean hasConstantCodeLength() {
		return true;
	}


	@Description(value = "Basic data type implementation, with methods for decoding and encoding sequence strings", isInheritable = false)
	public abstract class Base extends BEASTObject implements DataType {
		public int stateCount;
		public String codeMap;
		public int codeLength;
		/**
		 * mapping codes to sets of states FIXME: This should be named
		 * "mapCodeToState*List*", because that's what it does.
		 */
		public int[][] mapCodeToStateSet;

		@Override
		public void initAndValidate() {
			if (mapCodeToStateSet != null) {
				if (mapCodeToStateSet.length != codeMap.length() / codeLength) {
					throw new IllegalArgumentException("codeMap and mapCodeToStateSet have incompatible lengths");
				}
			}
		}

		// property getters

		/**
		 * size of the state space *
		 */
		@Override
		public int getStateCount() {
			return stateCount;
		}

		/**
		 * maps string encoding to state lists *
		 */
		public String getCodeMap() {
			return codeMap;
		}

		/**
		 * length of the encoding (i.e. how many characters give one code), e.g.
		 * 1 for nucleotide, 3 for codons. For variable code length data types,
		 * codeLength<1 (usually -1).
		 */
		public int getCodeLength() {
			return codeLength;
		}

		// Converters

		/**
		 * Encode a sequence string into an encoding chain.
		 */
		@Deprecated
		public List<Integer> string2state(String sequence) {
			return stringToEncoding(sequence);
		}

		@Override
		public List<Integer> stringToEncoding(String data) throws IllegalArgumentException {
			List<Integer> sequence;
			sequence = new ArrayList<>();
			// remove spaces
			data = data.replaceAll("\\s", "");
			data = data.toUpperCase();
			if (codeMap == null) {
				if (data.contains(",")) {
					// assume it is a comma separated string of integers
					String[] strs = data.split(",");
					for (String str : strs) {
						try {
							sequence.add(Integer.parseInt(str));
						} catch (NumberFormatException e) {
							sequence.add(-1);
						}
					}
				} else {
					// assume it is a string where each character is a state
					for (byte c : data.getBytes()) {
						switch (c) {
						case GAP_CHAR:
						case MISSING_CHAR:
							sequence.add(-1);
							break;
						default:
							sequence.add(Integer.parseInt((char) c + ""));
						}
					}
				}
			} else {
				if (codeLength == 1) {
					// single character codes
					for (int i = 0; i < data.length(); i++) {
						char cCode = data.charAt(i);
						int stateCount = codeMap.indexOf(cCode);
						if (stateCount < 0) {
							throw new IllegalArgumentException("Unknown code found in sequence: " + cCode);
						}
						sequence.add(stateCount);
					}
				} else if (codeLength > 1) {
					// multi-character codes of fixed length

					// use code map to resolve state codes
					Map<String, Integer> map = new HashMap<>();
					// fixed length code
					for (int i = 0; i < codeMap.length(); i += codeLength) {
						String code = codeMap.substring(i, i + codeLength);
						map.put(code, i / codeLength);
					}

					for (int i = 0; i < data.length(); i += codeLength) {
						String code = data.substring(i, i + codeLength).toUpperCase();
						if (map.containsKey(code)) {
							sequence.add(map.get(code));
						} else {
							throw new IllegalArgumentException("Unknown code found in sequence: " + code);
						}
					}
				} else {
					// variable length code of strings
					String[] codes = codeMap.toUpperCase().split(",");
					for (String code : data.split(",")) {
						boolean isFound = false;
						for (int codeIndex = 0; codeIndex < codes.length; codeIndex++) {
							if (code.equals(codes[codeIndex])) {
								sequence.add(codeIndex);
								isFound = true;
								break;
							}
						}
						if (!isFound) {
							throw new IllegalArgumentException("Could not find code " + code + " in codemap");
						}
					}
				}
			}
			return sequence;
		} // string2state

		@Deprecated
		@Override
		public String state2string(List<Integer> nrOfStates) {
			return encodingToString(nrOfStates);
		}

		@Override
		public String encodingToString(List<Integer> nrOfStates) {
			int[] nrOfStates2 = new int[nrOfStates.size()];
			for (int i = 0; i < nrOfStates2.length; i++) {
				nrOfStates2[i] = nrOfStates.get(i);
			}
			return encodingToString(nrOfStates2);
		}

		/**
		 * implementation for single character per state encoding *
		 */
		@Deprecated
		@Override
		public String state2string(int[] states) {
			return encodingToString(states);
		}

		@Override
		public String encodingToString(int[] codes) {
			String separator;
			if (codeMap == null || codeLength < 1) {
				separator = ",";
			} else {
				separator = "";
			}
			StringBuffer buf = new StringBuffer();
			boolean first = true;
			for (int code : codes) {
				if (first) {
					first = false;
				} else {
					buf.append(separator);
				}
				String character = getCharacter(code);
				buf.append(character);
			}
			return buf.toString();
		}

		@Override
		public int[] getStatesForCode(int code) {
			return mapCodeToStateSet[code];
		}

		@Override
		public boolean[] getStateSet(int code) {
			boolean[] stateSet = new boolean[stateCount];
			int[] stateNumbers = getStatesForCode(code);
			for (int i : stateNumbers) {
				stateSet[i] = true;
			}
			return stateSet;
		} // getStateSet

		/**
		 * Default implementations represent non-ambiguous characters as codes 0
		 * ... stateCount-1, and ambiguous characters using codes >= stateCount
		 * For data types that count something -- like microsattelites, or
		 * number of lineages in SNAPP -- a codes < 0 represents missing data.
		 */
		@Override
		@Deprecated
		public boolean isAmbiguousState(int code) {
			return isAmbiguousCode(code);
		}

		@Override
		public boolean isAmbiguousCode(int code) {
			return (code < 0 || code >= stateCount);
		}

		@Override
		public boolean isStandard() {
			return true;
		}

		@Deprecated
		@Override
		public char getChar(int code) {
			return getCharacter(code).charAt(0);
		}

		@Deprecated
		@Override
		public String getCode(int code) {
			return getCharacter(code);
		}

		@Override
		public String getCharacter(int code) {
			if (codeMap != null) {
				if (codeLength >= 1) {
					return codeMap.substring(code * codeLength, code * codeLength + codeLength);
				} else {
					String[] codes = codeMap.toUpperCase().split(",");
					return codes[code];
				}
			} else {
				return Integer.toString(code);
			}
		}

		@Override
		public String toString() {
			return getTypeDescription();
		}

		@Deprecated
		public Integer char2state(String character) {
			return string2state(character).get(0);
		}

	} // class Base

	
} // class DataType
