package beast.evolution.tree.coalescent;


import beast.core.*;
import beast.core.Input.Validate;

import java.util.List;


/**
 * @author Joseph Heled
 *         Date: 2/03/2011
 */

@Description("Scale a demographic function by a constant factor")
public class ScaledPopulationFunction extends PopulationFunction.Abstract {
    final public Input<PopulationFunction> popParameterInput = new Input<>("population",
            "population function to scale. ", Validate.REQUIRED);

    final public Input<Function> scaleFactorInput = new Input<>("factor",
            "scale population by this facor.", Validate.REQUIRED);

    public ScaledPopulationFunction() {
    }

    // Implementation of abstract methods

    @Override
	public List<String> getParameterIds() {
        List<String> ids = popParameterInput.get().getParameterIds();
        if (scaleFactorInput.get() instanceof BEASTObject)
            ids.add(((BEASTObject)scaleFactorInput.get()).getID());

        return ids;
    }

    @Override
	public double getPopSize(double t) {
        return popParameterInput.get().getPopSize(t) * scaleFactorInput.get().getArrayValue();
    }

    @Override
	public double getIntensity(double t) {
        double intensity = popParameterInput.get().getIntensity(t);
        double scale = scaleFactorInput.get().getArrayValue();
        return intensity / scale;
    }

    @Override
	public double getInverseIntensity(double x) {
        throw new RuntimeException("unimplemented");
    }

    @Override
    protected boolean requiresRecalculation() {
        return ((CalculationNode) popParameterInput.get()).isDirtyCalculation()
                || ((CalculationNode)scaleFactorInput.get()).isDirtyCalculation();
    }
}
