package beast.evolution.datatype;

import beast.base.Description;
import beast.evolution.datatype.DataType.Base;

@Description("Datatype for binary sequences")
public class Binary extends Base {
    int[][] x = {
            {0},  // 0
            {1},  // 1
            {0, 1}, // -
            {0, 1}, // ?
    };

    public Binary() {
        stateCount = 2;
        mapCodeToStateSet = x;
        codeLength = 1;
        codeMap = "01" + GAP_CHAR + MISSING_CHAR;
    }

    @Override
    public String getTypeDescription() {
        return "binary";
    }

}
