package beast.base.evolution.substitutionmodel;

import java.lang.reflect.InvocationTargetException;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.Nucleotide;
import beast.base.inference.parameter.RealParameter;

@Description("Transition model of nucleotide evolution (variable transition rates, two transversion rates). " +
        "Rates that are not specified are assumed to be 1.")
public class TIM extends GeneralSubstitutionModel {

    // Transition rates
    final public Input<Function> rateAGInput = new Input<>("rateAG", "substitution rate for A to G (default 1)");
    final public Input<Function> rateCTInput = new Input<>("rateCT", "substitution rate for C to T (default 1)");

    // Transversion rates
    final public Input<Function> rateTransversions1Input = new Input<>("rateTransversions1", "substitution rate for A<->C and G<->T");
    final public Input<Function> rateTransversions2Input = new Input<>("rateTransversions2", "substitution rate for C<->G and A<->T");

    Function rateAG;
    Function rateCT;
    Function rateTransversions1;
    Function rateTransversions2;

    public TIM() {
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
            throw new IllegalArgumentException("the rates attribute should not be used. Use the individual rates rateAG, rateCT, etc, instead.");
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

        rateAG = getParameter(rateAGInput);
        rateCT = getParameter(rateCTInput);
        rateTransversions1 = getParameter(rateTransversions1Input);
        rateTransversions2 = getParameter(rateTransversions2Input);
    }

    private Function getParameter(Input<Function> parameterInput) {
        if (parameterInput.get() != null) {
            return parameterInput.get();
        }
        return new RealParameter("1.0");
    }

    @Override
    public void setupRelativeRates() {
        relativeRates[0] = rateTransversions1.getArrayValue(); // A->C
        relativeRates[1] = rateAG.getArrayValue(); // A->G
        relativeRates[2] = rateTransversions2.getArrayValue(); // A->T

        relativeRates[3] = rateTransversions1.getArrayValue(); // C->A
        relativeRates[4] = rateTransversions2.getArrayValue(); // C->G
        relativeRates[5] = rateCT.getArrayValue(); // C->T

        relativeRates[6] = rateAG.getArrayValue(); // G->A
        relativeRates[7] = rateTransversions2.getArrayValue(); // G->C
        relativeRates[8] = rateTransversions1.getArrayValue(); // G->T

        relativeRates[9] = rateTransversions2.getArrayValue(); // T->A
        relativeRates[10] = rateCT.getArrayValue(); //T->C
        relativeRates[11] = rateTransversions1.getArrayValue(); //T->G
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Nucleotide;
    }
}