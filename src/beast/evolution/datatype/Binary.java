package beast.evolution.datatype;

import beast.core.Description;
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
    public String getDescription() {
        return "binary";
    }

    @Override
    public char getChar(int state) {
    	switch (state) {
    	case 0: return '0';
    	case 1: return '1';
    	case 2: return GAP_CHAR;
    	case 3: return MISSING_CHAR;
    	}
    	return MISSING_CHAR;
    }

}
