package beast.base.evolution.substitutionmodel;

import java.lang.reflect.InvocationTargetException;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.Nucleotide;
import beast.base.inference.parameter.RealParameter;

@Description("Transversion model of nucleotide evolution (variable transversion rates, equal transition rates)." +
        "Rates that are not specified are assumed to be 1.")
public class TVM extends GeneralSubstitutionModel {

    // Transversion rates
    final public Input<RealParameter> rateACInput = new Input<>("rateAC", "substitution rate for A to C (default 1)");
    final public Input<RealParameter> rateATInput = new Input<>("rateAT", "substitution rate for A to T (default 1)");
    final public Input<RealParameter> rateCGInput = new Input<>("rateCG", "substitution rate for C to G (default 1)");
    final public Input<RealParameter> rateGTInput = new Input<>("rateGT", "substitution rate for G to T (default 1)");

    // Transition rates
    final public Input<RealParameter> rateTransitionsInput = new Input<>("rateTransitions", "substitution rate for A<->G and C<->T");

    RealParameter rateAC;
    RealParameter rateGT;
    RealParameter rateAT;
    RealParameter rateCG;
    RealParameter rateTransitions;

    public TVM() {
        ratesInput.setRule(Validate.OPTIONAL);
        try {
            ratesInput.setValue(null, this);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }
    }

    @Override
    public void initAndValidate() {
        if (ratesInput.get() != null) {
            throw new IllegalArgumentException("the rates attribute should not be used. Use the individual rates rateAC, rateCG, etc, instead.");
        }

        frequencies = frequenciesInput.get();
        updateMatrix = true;
        nrOfStates = frequencies.getFreqs().length;
        if (nrOfStates != 4) {
            throw new IllegalArgumentException("Frequencies has wrong size. Expected 4, but got " + nrOfStates);
        }

        try {
			eigenSystem = createEigenSystem();
		} catch (SecurityException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
        rateMatrix = new double[nrOfStates][nrOfStates];
        relativeRates = new double[nrOfStates * (nrOfStates - 1)];
        storedRelativeRates = new double[nrOfStates * (nrOfStates - 1)];

        rateAC = getParameter(rateACInput);
        rateAT = getParameter(rateATInput);
        rateCG = getParameter(rateCGInput);
        rateGT = getParameter(rateGTInput);

        rateTransitions = getParameter(rateTransitionsInput);
    }

    private RealParameter getParameter(Input<RealParameter> parameterInput) {
        if (parameterInput.get() != null) {
            return parameterInput.get();
        }
        return new RealParameter("1.0");
    }

    @Override
    public void setupRelativeRates() {
        relativeRates[0] = rateAC.getValue(); // A->C
        relativeRates[1] = rateTransitions.getValue(); // A->G
        relativeRates[2] = rateAT.getValue(); // A->T

        relativeRates[3] = rateAC.getValue(); // C->A
        relativeRates[4] = rateCG.getValue(); // C->G
        relativeRates[5] = rateTransitions.getValue(); // C->T

        relativeRates[6] = rateTransitions.getValue(); // G->A
        relativeRates[7] = rateCG.getValue(); // G->C
        relativeRates[8] = rateGT.getValue(); // G->T

        relativeRates[9] = rateAT.getValue(); // T->A
        relativeRates[10] = rateTransitions.getValue(); //T->C
        relativeRates[11] = rateGT.getValue(); //T->G
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Nucleotide;
    }
}
