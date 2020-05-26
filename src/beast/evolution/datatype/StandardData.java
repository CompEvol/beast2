package beast.evolution.datatype;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import beast.core.Description;
import beast.core.Input;

@Description("Integer data type to describe discrete morphological characters with polymorphisms")
public class StandardData extends DataType.Base {

    final public Input<Integer> maxNrOfStatesInput = new Input<>("nrOfStates", "specifies the maximum number of " +
            "character states in data matrix or in the filtered alignment");
    final public Input<String> listOfAmbiguitiesInput = new Input<>("ambiguities", "all possible ambiguities presented " +
            "as space separated sets of ordered elements. Elements are digits 0..9.");
    final public Input<List<UserDataType>> charStateLabelsInput = new Input<>("charstatelabels",
            "list of morphological character descriptions. Position in the list corresponds to the position of the" +
                    "character in the alignment", new ArrayList<>());

    private String[] ambiguities = {};
    private ArrayList<String> codeMapping;

    private int ambCount;


	@Override
	public void initAndValidate() {
        if (maxNrOfStatesInput.get() != null && maxNrOfStatesInput.get() != 0) {
            stateCount = maxNrOfStatesInput.get();
        } else {
            stateCount = -1;
        }

        mapCodeToStateSet = null;
        codeLength = -1;
        codeMap = null;
        createCodeMapping();
	}

    private void createCodeMapping() {
        if (listOfAmbiguitiesInput.get() != null) {
            ambiguities = listOfAmbiguitiesInput.get().split(" ");
        }

        ambCount = ambiguities.length;
        codeMapping = new ArrayList<>();
        for (int i=0; i<stateCount; i++) {
            codeMapping.add(Integer.toString(i));
        }
        for (int i=0; i< ambCount; i++) {
            codeMapping.add(ambiguities[i]);
        }
        codeMapping.add(Character.toString(GAP_CHAR));
        codeMapping.add(Character.toString(MISSING_CHAR));

        mapCodeToStateSet = new int[codeMapping.size()][];
        for (int i = 0; i < codeMapping.size() - 2; i++) {
        	int [] stateSet = new int[codeMapping.get(i).length()];
        	for (int k = 0; k < stateSet.length; k++) {
        		stateSet[k] = (codeMapping.get(i).charAt(k) - '0');
        	}
        	mapCodeToStateSet[i] = stateSet;
        }
        
    	// TODO: is this the correct way to deal with stateCount == -1?
    	int n = stateCount >= 0 ? stateCount : 10;
        int [] stateSet = new int[n];
        for (int i = 0; i < n; i++) {
        	stateSet[i] = i;
        }
        // GAP_CHAR
        mapCodeToStateSet[mapCodeToStateSet.length - 2] = stateSet;
        // MISSING_CHAR
        mapCodeToStateSet[mapCodeToStateSet.length - 1] = stateSet;
    }
    
    @Override
    public int[] getStatesForCode(int state) {
    	if (state >= 0) {
    		return mapCodeToStateSet[state];
    	} else {
    		return mapCodeToStateSet[mapCodeToStateSet.length - 1];
    	}
    }


    @Override
	public List<Integer> stringToEncoding(String data) {
        List<Integer> sequence;
        sequence = new ArrayList<>();
        // remove spaces
        data = data.replaceAll("\\s", "");

        ArrayList<Integer> amb = new ArrayList<>();
        boolean readingAmb=false;
        for (byte c : data.getBytes()) {
            if (!readingAmb) {
                switch (c) {
                    case GAP_CHAR:
                    case MISSING_CHAR:
                    	String missing = Character.toString(MISSING_CHAR);
                   		sequence.add(codeMapping.indexOf(missing));
                        break;
                    case '{':
                        readingAmb = true;
                        amb.clear();
                        break;
                    default:
                        sequence.add(Integer.parseInt((char) c + ""));
                }
            } else {
                if (c != '}') {
                    amb.add(Integer.parseInt((char) c + "") );
                } else {
                    readingAmb = false;
                    Collections.sort(amb);
                    String ambStr = "";
                    for (Integer a : amb) {
                        ambStr += Integer.toString(a);
                    }
                    sequence.add(codeMapping.indexOf(ambStr));
                }

            }

        }

        return sequence;
    } // string2state

    @Override
    public String getTypeDescription() {
        return "standard";
    }

    @Override
	public String getCharacter(int state) {
    	return codeMapping.get(state);
    }

//    @Description("A class to store the description of a character")
//    public class CharStateLabels extends BEASTObject {
//
//        public Input<Integer> nrOfStatesInput = new Input<>("states", "number of states fro this character");
//        public Input<String> characterNameInput = new Input<>("characterName", "the name of the charcter");
//        public Input<List<String>> stateNamesInput = new Input<>("stateNames", "the list of the state names ordered " +
//                "according to codes given, that is the first in the list is coded by 0, second, by 1 and so forth.", new ArrayList<>());
//
//        private int nrOfStates;
//        private String charName;
//        private ArrayList<String> stateNames;
//
//        public CharStateLabels(String newCharName, ArrayList<String> newStateNames) {
//            characterNameInput.setValue(newCharName, this);
//            charName = newCharName;
//            stateNamesInput.setValue(newStateNames, this);
//            stateNames = newStateNames;
//            nrOfStates = stateNames.size();
//            nrOfStatesInput.setValue(nrOfStates, this);
//        }
//
//        public int getNrOfStates() {
//            return nrOfStates;
//        }
//
//        public String getCharacterName() {
//            return charName;
//        }
//
//        public ArrayList<String> getStateNames() { return stateNames; }
//
//        @Override
//        public void initAndValidate() {
//        }
//
//    }
    
    @Override
    public boolean hasConstantCodeLength() {
    	return false;
    }
}
