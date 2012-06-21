package beast.evolution.datatype;

import beast.core.Description;
import beast.evolution.datatype.DataType.Base;

@Description("DataType for amino acids.")
public class Aminoacid extends Base {

    public Aminoacid() {
        m_nStateCount = 20;
        m_nCodeLength = 1;
        m_sCodeMap = "ACDEFGHIKLMNPQRSTVWY" + GAP_CHAR + 'X';

        m_mapCodeToStateSet = new int[22][];
        for (int i = 0; i < 20; i++) {
            m_mapCodeToStateSet[i] = new int[1];
            m_mapCodeToStateSet[i][0] = i;
        }
        int[] all = new int[20];
        for (int i = 0; i < 20; i++) {
            all[i] = i;
        }
        m_mapCodeToStateSet[20] = all;
        m_mapCodeToStateSet[21] = all;
    }

    @Override
    public String getDescription() {
        return "aminoacid";
    }

}
