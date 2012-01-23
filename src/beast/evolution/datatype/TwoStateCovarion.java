package beast.evolution.datatype;

import beast.core.Description;
import beast.evolution.datatype.DataType.Base;

@Description("Datatype for two state covarion sequences")
public class TwoStateCovarion extends Base {
    int[][] x = {
            {0, 2},  // 0
            {1, 3},  // 1
            {0},  // a
            {1},  // b
            {2},  // c
            {3},  // d
            {0, 1, 2, 3},  // -
            {0, 1, 2, 3},  // ?
    };

    public TwoStateCovarion() {
        m_nStateCount = 4;
        m_mapCodeToStateSet = x;
        m_nCodeLength = 1;
        m_sCodeMap = "01abcd" + GAP_CHAR + MISSING_CHAR;
    }

    @Override
    public String getDescription() {
        return "twoStateCovarion";
    }
}
