package beast.base.evolution.substitutionmodel;

import java.lang.reflect.InvocationTargetException;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.Nucleotide;
import beast.base.inference.parameter.RealParameter;

@Description("Transversion model of nucleotide evolution (variable transversion rates, equal transition rates)." +
        "Rates that are not specified are assumed to be 1.")
public class TVM extends GeneralSubstitutionModel {

    // Transversion rates
    final public Input<Function> rateACInput = new Input<>("rateAC", "substitution rate for A to C (default 1)");
    final public Input<Function> rateATInput = new Input<>("rateAT", "substitution rate for A to T (default 1)");
    final public Input<Function> rateCGInput = new Input<>("rateCG", "substitution rate for C to G (default 1)");
    final public Input<Function> rateGTInput = new Input<>("rateGT", "substitution rate for G to T (default 1)");

    // Transition rates
    final public Input<Function> rateTransitionsInput = new Input<>("rateTransitions", "substitution rate for A<->G and C<->T");

    Function rateAC;
    Function rateGT;
    Function rateAT;
    Function rateCG;
    Function rateTransitions;

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

    private Function getParameter(Input<Function> parameterInput) {
        if (parameterInput.get() != null) {
            return parameterInput.get();
        }
        return new RealParameter("1.0");
    }

    @Override
    public void setupRelativeRates() {
        relativeRates[0] = rateAC.getArrayValue(); // A->C
        relativeRates[1] = rateTransitions.getArrayValue(); // A->G
        relativeRates[2] = rateAT.getArrayValue(); // A->T

        relativeRates[3] = rateAC.getArrayValue(); // C->A
        relativeRates[4] = rateCG.getArrayValue(); // C->G
        relativeRates[5] = rateTransitions.getArrayValue(); // C->T

        relativeRates[6] = rateTransitions.getArrayValue(); // G->A
        relativeRates[7] = rateCG.getArrayValue(); // G->C
        relativeRates[8] = rateGT.getArrayValue(); // G->T

        relativeRates[9] = rateAT.getArrayValue(); // T->A
        relativeRates[10] = rateTransitions.getArrayValue(); //T->C
        relativeRates[11] = rateGT.getArrayValue(); //T->G
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Nucleotide;
    }
}
