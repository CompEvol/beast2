package beast.evolution.substitutionmodel;

import java.lang.reflect.InvocationTargetException;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.Nucleotide;

@Description("Symmetrical model of nucleotide evolution with equal base frequencies." +
        "Rates that are not specified are assumed to be 1.")
public class SYM extends GeneralSubstitutionModel {
    final public Input<RealParameter> rateACInput = new Input<>("rateAC", "substitution rate for A to C (default 1)");
    final public Input<RealParameter> rateAGInput = new Input<>("rateAG", "substitution rate for A to G (default 1)");
    final public Input<RealParameter> rateATInput = new Input<>("rateAT", "substitution rate for A to T (default 1)");
    final public Input<RealParameter> rateCGInput = new Input<>("rateCG", "substitution rate for C to G (default 1)");
    final public Input<RealParameter> rateCTInput = new Input<>("rateCT", "substitution rate for C to T (default 1)");
    final public Input<RealParameter> rateGTInput = new Input<>("rateGT", "substitution rate for G to T (default 1)");

    RealParameter rateAC;
    RealParameter rateAG;
    RealParameter rateAT;
    RealParameter rateCG;
    RealParameter rateCT;
    RealParameter rateGT;

    // For hardcoding equal base frequencies
    //double[] frequencies;

    public SYM() {
        ratesInput.setRule(Validate.OPTIONAL);
        try {
            ratesInput.setValue(null, this);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }

        // Override the superclass SubstitutionModel.Base requirement for input frequencies, since they are equal in SYM
        //frequenciesInput.setRule(Validate.OPTIONAL);
        //try {
        //    initAndValidate();
        //} catch (Exception e) {
        //    e.printStackTrace();
        //    throw new RuntimeException("initAndValidate() call failed when constructing SYM()");
        //}
    }

    @Override
    public void initAndValidate() {
        if (ratesInput.get() != null) {
            throw new IllegalArgumentException("the rates attribute should not be used. Use the individual rates rateAC, rateCG, etc, instead.");
        }

        //if (frequenciesInput.get() != null) {
        //    throw new RuntimeException("Frequencies must not be specified in the SYM model. They are assumed equal.");
        // }

        // Set equal base frequencies
        //frequencies = new double[]{0.25, 0.25, 0.25, 0.25};
        frequencies = frequenciesInput.get();
        updateMatrix = true;
        nrOfStates = frequencies.getFreqs().length;

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
        rateAG = getParameter(rateAGInput);
        rateAT = getParameter(rateATInput);
        rateCG = getParameter(rateCGInput);
        rateCT = getParameter(rateCTInput);
        rateGT = getParameter(rateGTInput);
    }

    private RealParameter getParameter(Input<RealParameter> parameterInput) {
        if (parameterInput.get() != null) {
            return parameterInput.get();
        }
        return new RealParameter("1.0");
    }

    @Override
    protected void setupRelativeRates() {
        relativeRates[0] = rateAC.getValue(); // A->C
        relativeRates[1] = rateAG.getValue(); // A->G
        relativeRates[2] = rateAT.getValue(); // A->T

        relativeRates[3] = rateAC.getValue(); // C->A
        relativeRates[4] = rateCG.getValue(); // C->G
        relativeRates[5] = rateCT.getValue(); // C->T

        relativeRates[6] = rateAG.getValue(); // G->A
        relativeRates[7] = rateCG.getValue(); // G->C
        relativeRates[8] = rateGT.getValue(); // G->T

        relativeRates[9] = rateAT.getValue(); // T->A
        relativeRates[10] = rateCT.getValue(); //T->C
        relativeRates[11] = rateGT.getValue(); //T->G
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Nucleotide;
    }
}
