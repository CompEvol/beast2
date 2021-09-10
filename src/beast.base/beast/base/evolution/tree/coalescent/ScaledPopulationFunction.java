package beast.base.evolution.tree.coalescent;


import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.CalculationNode;
import beast.base.inference.parameter.RealParameter;


/**
 * @author Joseph Heled
 *         Date: 2/03/2011
 */

@Description("Scale a demographic function by a constant factor")
public class ScaledPopulationFunction extends PopulationFunction.Abstract {
    final public Input<PopulationFunction> popParameterInput = new Input<>("population",
            "population function to scale. ", Validate.REQUIRED);

    final public Input<RealParameter> scaleFactorInput = new Input<>("factor",
            "scale population by this facor.", Validate.REQUIRED);

    public ScaledPopulationFunction() {
    }

    // Implementation of abstract methods

    @Override
	public List<String> getParameterIds() {
        List<String> ids = popParameterInput.get().getParameterIds();
        ids.add(scaleFactorInput.get().getID());
        return ids;
    }

    @Override
	public double getPopSize(double t) {
        return popParameterInput.get().getPopSize(t) * scaleFactorInput.get().getValue();
    }

    @Override
	public double getIntensity(double t) {
        double intensity = popParameterInput.get().getIntensity(t);
        double scale = scaleFactorInput.get().getValue();
        return intensity / scale;
    }

    @Override
	public double getInverseIntensity(double x) {
        throw new RuntimeException("unimplemented");
    }

    @Override
    protected boolean requiresRecalculation() {
        return ((CalculationNode) popParameterInput.get()).isDirtyCalculation() || scaleFactorInput.get().somethingIsDirty();
    }
}
