package beast.evolution.datatype;


import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.datatype.DataType.Base;



@Description("User defined datatype. Allows custom symbols to map onto statesets.")
public class UserDataType extends Base {
    final public Input<Integer> stateCountInput = new Input<>("states", "total number of states", Validate.REQUIRED);
    final public Input<Integer> codeLengthInput = new Input<>("codelength", "length of code, if negative a variable length code is assumed, default 1", 1);
    final public Input<String> codeMapInput = new Input<>("codeMap", "mapping of codes to states. " +
            "A comma separated string of codes with a subset of states. " +
            "A state set is a space separates list of zero based integers, up to the number of states, " +
            "e.g. A=0, C=1, R=0 2, ? = 0 1 2 3", Validate.REQUIRED);
    
    final public Input<String> characterNameInput = new Input<>("characterName", "the name of the character");
    final public Input<String> stateNamesInput = new Input<>("value", "the list of the state names ordered " +
    		"according to codes given, that is the first in the list is coded by 0, second, by 1 and so forth.");

    public UserDataType() {} // default c'tor
    public UserDataType(String newCharName, ArrayList<String> newStateNames) {
        characterNameInput.setValue(newCharName, this);
        if (newStateNames.size() > 0) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < newStateNames.size(); i++) {
                buf.append(i + "=" + i +", ");
            }
            buf.append("? =");
            for (int i = 0; i < newStateNames.size(); i++) {
                buf.append(i +" ");
            }
            codeMapInput.setValue(buf.toString(), this);
            buf = new StringBuilder();
            for (int i = 0; i < newStateNames.size(); i++) {
                buf.append(newStateNames.get(i) +", ");
            }
            buf.delete(buf.length()-2, buf.length());
            stateNamesInput.setValue(buf.toString(), this);
            stateCountInput.setValue(newStateNames.size(), this);
        } else {
            codeMapInput.setValue("", this);
            stateNamesInput.setValue("", this);
            stateCountInput.setValue(-1, this);
        }
    }
    
    @Override
    public void initAndValidate() throws Exception {
        stateCount = stateCountInput.get();
        codeLength = codeLengthInput.get();

        String sCodeMap = codeMapInput.get();
        if (!codeMapInput.get().equals("")) {
            String[] sStrs = sCodeMap.split(",");
            codeMap = "";
            mapCodeToStateSet = new int[sStrs.length][];
            int k = 0;
            for (String sStr : sStrs) {
                String[] sStrs2 = sStr.split("=");
                // parse the code
                String sCode = sStrs2[0].replaceAll("\\s", "");

                codeMap += sCode;
                if (codeLength > 0) {
                    if (sCode.length() != codeLength) {
                        throw new Exception("Invalide code '" + sCode + "'. Expected code of length " + codeLength);
                    }
                } else {
                    codeMap += ",";
                }
                // parse the state set
                List<Integer> stateSet = new ArrayList<>();
                sStrs2 = sStrs2[1].split("\\s+");
                for (String sStr2 : sStrs2) {
                    if (sStr2.length() > 0) {
                        int i = Integer.parseInt(sStr2);
                        if (i < 0 || (stateCount > 0 && i >= stateCount)) {
                            throw new Exception("state index should be from 0 to statecount, not " + i);
                        }
                        stateSet.add(i);
                    }
                }

                int[] stateSet2 = new int[stateSet.size()];
                for (int i = 0; i < stateSet.size(); i++) {
                    stateSet2[i] = stateSet.get(i);
                }
                mapCodeToStateSet[k++] = stateSet2;
            }
        }
    }

    @Override
    public String getCode(int state) {
        return String.valueOf(codeMap.split(",")[state]);
    }

    @Override
    public String getTypeDescription() {
        return "user defined";
    }

}