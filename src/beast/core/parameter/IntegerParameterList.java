package beast.core.parameter;

import beast.core.Description;
import java.util.List;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("State node describing a list of integer-valued parameters.")
public class IntegerParameterList extends GeneralParameterList<Integer> {
    
    @Override
    public void initAndValidate() {
        super.initAndValidate();
        
        if (lowerBound == null)
            lowerBound = Integer.MIN_VALUE+1;
        
        if (upperBound == null)
            upperBound = Integer.MAX_VALUE-1;
    }

    @Override
    protected void readStateFromString(String[] boundsString,
            List<String[]> parameterValueStrings,
            List<Integer> keys) {
        
        lowerBound = Integer.parseInt(boundsString[0]);
        upperBound = Integer.parseInt(boundsString[1]);
        
        pList.clear();
        
        for (int pidx=0; pidx<parameterValueStrings.size(); pidx++) {
            String [] pValueString = parameterValueStrings.get(pidx);
            
            QuietParameter param = new QuietParameter();
            param.key = keys.get(pidx);
            
            for (int vidx=0; vidx<pValueString.length; vidx++)
                param.values[vidx] = Integer.parseInt(pValueString[vidx]);
            
            pList.add(param);
        }
    }
    
}
