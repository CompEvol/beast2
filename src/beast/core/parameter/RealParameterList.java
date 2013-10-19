package beast.core.parameter;

import beast.core.Description;
import java.util.List;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("State node describing a list of real-valued parameters.")
public class RealParameterList extends GeneralParameterList<Double> {
    
    @Override
    public void initAndValidate() {
        super.initAndValidate();
        
        if (lowerBound == null)
            lowerBound = Double.NEGATIVE_INFINITY;
        
        if (upperBound == null)
            upperBound = Double.POSITIVE_INFINITY;
    }

    @Override
    protected void readStateFromString(String[] boundsString,
            List<String[]> parameterValueStrings,
            List<Integer> keys) {
        
        lowerBound = Double.parseDouble(boundsString[0]);
        upperBound = Double.parseDouble(boundsString[1]);
        
        pList.clear();
        
        for (int pidx=0; pidx<parameterValueStrings.size(); pidx++) {
            String [] pValueString = parameterValueStrings.get(pidx);
            
            QuietParameter param = new QuietParameter();
            param.key = keys.get(pidx);
            
            for (int vidx=0; vidx<pValueString.length; vidx++)
                param.values[vidx] = Double.parseDouble(pValueString[vidx]);
            
            pList.add(param);
        }
    }
    
}
