package beast.base.evolution.datatype;

import beast.base.core.Description;
import beast.base.evolution.datatype.DataType.Base;

@Description("Datatype for integer sequences")
public class IntegerData extends Base {

    public IntegerData() {
        stateCount = -1;
        mapCodeToStateSet = null;
        codeLength = -1;
        codeMap = null;
    }

    @Override
    public String getTypeDescription() {
        return "integer";
    }
    
    @Override
    public boolean isAmbiguousCode(int code) {
    	return code < 0;
    }

    @Override
    public String getCharacter(int code) {
    	if (code < 0) {
    		return "?";
    	}
    	return code + "";
    }
    
	@Override
	public int[] getStatesForCode(int code) {
		return new int[]{code};
	}
}
