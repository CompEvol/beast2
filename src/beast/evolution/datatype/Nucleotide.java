package beast.evolution.datatype;

import beast.core.Description;
import beast.evolution.datatype.DataType.Base;

@Description("Nucleotide datatype for DNA sequences")
public class Nucleotide extends Base {
    int[][] x = {
            {0},  // A
            {1},  // C
            {2},  // G
            {3},  // T
            {3},  // U
            {0, 2}, // R
            {1, 3}, // Y
            {0, 1}, // M
            {0, 3}, // W
            {1, 2}, // S
            {2, 3}, // K
            {1, 2, 3}, // B
            {0, 2, 3}, // D
            {0, 1, 3}, // H
            {0, 1, 2}, // V
            {0, 1, 2, 3}, // N
            {0, 1, 2, 3}, // X
            {0, 1, 2, 3}, // -
            {0, 1, 2, 3}, // ?
    };

    public Nucleotide() {
        stateCount = 4;
        mapCodeToStateSet = x;
        codeLength = 1;
        codeMap = "ACGTURYMWSKBDHVNX" + GAP_CHAR + MISSING_CHAR;
    }

    @Override
    public String getDescription() {
        return "nucleotide";
    }

}
