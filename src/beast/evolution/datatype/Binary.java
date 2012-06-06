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
        m_nStateCount = 2;
        m_mapCodeToStateSet = x;
        m_nCodeLength = 1;
        m_sCodeMap = "01" + GAP_CHAR + MISSING_CHAR;
    }

    @Override
    public String getDescription() {
        return "binary";
    }

    @Override
    public char getChar(int state) {
        return (char) (state + '0');
    }

}
