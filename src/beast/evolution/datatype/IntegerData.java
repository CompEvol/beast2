package beast.evolution.datatype;

import beast.core.Description;
import beast.evolution.datatype.DataType.Base;

@Description("Datatype for integer sequences")
public class IntegerData extends Base {

    public IntegerData() {
        m_nStateCount = -1;
        m_mapCodeToStateSet = null;
        m_nCodeLength = -1;
        m_sCodeMap = null;
    }

    @Override
    public String getDescription() {
        return "integer";
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

}
