package beast.evolution.datatype;

import beast.core.Description;
import beast.evolution.datatype.DataType.Base;

@Description("DataType for amino acids.")
public class Aminoacid extends Base {

    public Aminoacid() {
        stateCount = 20;
        codeLength = 1;
        codeMap = "ACDEFGHIKLMNPQRSTVWY" + "X" + GAP_CHAR + MISSING_CHAR;

        mapCodeToStateSet = new int[23][];
        for (int i = 0; i < 20; i++) {
            mapCodeToStateSet[i] = new int[1];
            mapCodeToStateSet[i][0] = i;
        }
        int[] all = new int[20];
        for (int i = 0; i < 20; i++) {
            all[i] = i;
        }
        mapCodeToStateSet[20] = all;
        mapCodeToStateSet[21] = all;
        mapCodeToStateSet[22] = all;
    }

    @Override
    public String getTypeDescription() {
        return "aminoacid";
    }

}
