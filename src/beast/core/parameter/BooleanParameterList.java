package beast.core.parameter;

import beast.core.Description;
import beast.core.Input;
import java.util.List;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("State node describing a list of boolean parameters.")
public class BooleanParameterList extends GeneralParameterList<Boolean> {
    
    @Override
    public void initAndValidate() {
        lowerBound = false;
        upperBound = true;
        
        super.initAndValidate();
    }

    @Override
    protected void readStateFromString(String[] boundsString,
            List<String[]> parameterValueStrings,
            List<Integer> keys) {
        
        lowerBound = Boolean.parseBoolean(boundsString[0]);
        upperBound = Boolean.parseBoolean(boundsString[1]);
        
        pList.clear();
        
        for (int pidx=0; pidx<parameterValueStrings.size(); pidx++) {
            String [] pValueString = parameterValueStrings.get(pidx);
            
            QuietParameter param = new QuietParameter();
            param.key = keys.get(pidx);
            
            for (int vidx=0; vidx<pValueString.length; vidx++)
                param.values[vidx] = Boolean.parseBoolean(pValueString[vidx]);
            
            pList.add(param);
        }
    }
    
}
