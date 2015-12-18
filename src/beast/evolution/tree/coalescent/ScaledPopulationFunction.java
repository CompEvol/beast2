package beast.evolution.tree.coalescent;


import java.util.List;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;


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

    public List<String> getParameterIds() {
        List<String> ids = popParameterInput.get().getParameterIds();
        ids.add(scaleFactorInput.get().getID());
        return ids;
    }

    public double getPopSize(double t) {
        return popParameterInput.get().getPopSize(t) * scaleFactorInput.get().getValue();
    }

    public double getIntensity(double t) {
        double fIntensity = popParameterInput.get().getIntensity(t);
        double fScale = scaleFactorInput.get().getValue();
        return fIntensity / fScale;
    }

    public double getInverseIntensity(double x) {
        throw new RuntimeException("unimplemented");
    }

    @Override
    protected boolean requiresRecalculation() {
        return ((CalculationNode) popParameterInput.get()).isDirtyCalculation() || scaleFactorInput.get().somethingIsDirty();
    }
}
