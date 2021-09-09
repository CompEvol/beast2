package beast.inference.parameter;

import java.util.List;

import beast.base.Description;
import beast.base.Input;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("State node describing a list of real-valued parameters.")
public class RealParameterList extends GeneralParameterList<Double> {
    
    final public Input<Double> lowerBoundInput = new Input<>("lower",
            "Lower bound on parameter values.", Double.NEGATIVE_INFINITY);
    final public Input<Double> upperBoundInput = new Input<>("upper",
            "Upper bound on parameter values.", Double.POSITIVE_INFINITY);
    
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
