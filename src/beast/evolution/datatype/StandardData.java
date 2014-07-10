package beast.evolution.datatype;


import beast.core.Description;
import beast.core.Input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Description("Integer data type to describe discrete morphological characters with polymorphisms")
public class StandardData extends DataType.Base {

    public Input<Integer> maxNrOrStatesInput = new Input<Integer>("nrOfStates", "specifies the maximum number of " +
            "character states in data matrix or in the filtered alignment");
    public Input<String> listOfAmbiguitiesInput = new Input<String>("ambiguities", "all possible ambiguities presented " +
            "as space separated sets of ordered elements. Elements are digits 0..9.");
//    public Input<List<StandardData.CharStateLabels>> charStateLabelsInput= new Input<List<StandardData.CharStateLabels>>("charstatelabels",
//            "list of morphological character descriptions. Position in the list corresponds to the position of the" +
//                    "character in the alignment");

    private String[] ambiguities = {};
    private ArrayList<String> codeMapping;

    private int ambCount;


    public StandardData() {
        if (maxNrOrStatesInput.get() != null) {
            stateCount = maxNrOrStatesInput.get();
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
        codeMapping = new ArrayList<String>();
        for (int i=0; i<stateCount; i++) {
            codeMapping.add(Integer.toString(i));
        }
        for (int i=0; i< ambCount; i++) {
            codeMapping.add(ambiguities[i]);
        }
        codeMapping.add(Character.toString(GAP_CHAR));
        codeMapping.add(Character.toString(MISSING_CHAR));

    }

    @Override
    public List<Integer> string2state(String data) throws Exception {
        List<Integer> sequence;
        sequence = new ArrayList<Integer>();
        // remove spaces
        data = data.replaceAll("\\s", "");

        ArrayList<Integer> amb = new ArrayList<Integer>();
        boolean readingAmb=false;
        for (byte c : data.getBytes()) {
            if (!readingAmb) {
                switch (c) {
                    case GAP_CHAR:
                    case MISSING_CHAR:
                        sequence.add(-1);
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
                    for (int i=0; i<amb.size(); i++) {
                        ambStr += Integer.toString(amb.get(i));
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
    public boolean isAmbiguousState(int state) {
        return state < 0;
    }

    @Override
    public char getChar(int state) {
        if (state < 0) {
            return '?';
        }
        return (char)('0'+state);
    }

    @Description("A class to store the description of a character")
    public class CharStateLabels {
        private int nrOfStates;
        private String description;

        public CharStateLabels(int newNrOfStates, String newDescription) {
            nrOfStates = newNrOfStates;
            description = newDescription;
        }

        public int getNrOfStates() {
            return nrOfStates;
        }

        public String getDescription() {
            return description;
        }
    }
}
