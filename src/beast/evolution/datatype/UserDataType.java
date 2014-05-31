package beast.evolution.datatype;


import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.datatype.DataType.Base;



@Description("User defined datatype. Allows custom symbols to map onto statesets.")
public class UserDataType extends Base {
    public Input<Integer> stateCountInput = new Input<Integer>("states", "total number of states", Validate.REQUIRED);
    public Input<Integer> codeLengthInput = new Input<Integer>("codelength", "length of code, if negative a variable length code is assumed, default 1", 1);
    public Input<String> codeMapInput = new Input<String>("codeMap", "mapping of codes to states. " +
            "A comma separated string of codes with a subset of states. " +
            "A state set is a space separates list of zero based integers, up to the number of states, " +
            "e.g. A=0, C=1, R=0 2, ? = 0 1 2 3", Validate.REQUIRED);

    @Override
    public void initAndValidate() throws Exception {
        stateCount = stateCountInput.get();
        codeLength = codeLengthInput.get();

        String sCodeMap = codeMapInput.get();
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
            List<Integer> stateSet = new ArrayList<Integer>();
            sStrs2 = sStrs2[1].split("\\s+");
            for (String sStr2 : sStrs2) {
                if (sStr2.length() > 0) {
                    int i = Integer.parseInt(sStr2);
                    if (i < 0 || i >= stateCount) {
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

    @Override
    public String getCode(int state) {
        return String.valueOf(codeMap.split(",")[state]);
    }

    @Override
    public String getTypeDescription() {
        return "user defined";
    }

}