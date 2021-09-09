package beast.inference.parameter;

import java.util.List;

import beast.base.Description;
import beast.base.Input;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("State node describing a list of integer-valued parameters.")
public class IntegerParameterList extends GeneralParameterList<Integer> {
    
    final public Input<Integer> lowerBoundInput = new Input<>("lower",
            "Lower bound on parameter values.", Integer.MIN_VALUE+1);
    final public Input<Integer> upperBoundInput = new Input<>("upper",
            "Upper bound on parameter values.", Integer.MAX_VALUE-1);
    
    @Override
    public void initAndValidate() {
        lowerBound = lowerBoundInput.get();
        upperBound = upperBoundInput.get();
        
        super.initAndValidate();
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
