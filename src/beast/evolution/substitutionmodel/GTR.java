package beast.evolution.substitutionmodel;

import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.Nucleotide;

@Description("General Time Reversible model of nucleotide evolution. " +
        "Rates that are not specified are assumed to be 1. ")
public class GTR extends GeneralSubstitutionModel {
    public Input<Function> rateACInput = new Input<Function>("rateAC", "substitution rate for A to C (default 1)");
    public Input<Function> rateAGInput = new Input<Function>("rateAG", "substitution rate for A to G (default 1)");
    public Input<Function> rateATInput = new Input<Function>("rateAT", "substitution rate for A to T (default 1)");
    public Input<Function> rateCGInput = new Input<Function>("rateCG", "substitution rate for C to G (default 1)");
    public Input<Function> rateCTInput = new Input<Function>("rateCT", "substitution rate for C to T (default 1)");
    public Input<Function> rateGTInput = new Input<Function>("rateGT", "substitution rate for G to T (default 1)");

    Function rateAC;
    Function rateAG;
    Function rateAT;
    Function rateCG;
    Function rateCT;
    Function rateGT;

    public GTR() {
        ratesInput.setRule(Validate.OPTIONAL);
        try {
        	ratesInput.setValue(null, this);
        } catch (Exception e) {
        	e.printStackTrace();
			// TODO: handle exception
		}
    }

    @Override
    public void initAndValidate() throws Exception {
        if (ratesInput.get() != null) {
            throw new Exception("the rates attribute should not be used. Use the individual rates rateAC, rateCG, etc, instead.");
        }

        frequencies = frequenciesInput.get();
        updateMatrix = true;
        nrOfStates = frequencies.getFreqs().length;
        if (nrOfStates != 4) {
            throw new Exception("Frequencies has wrong size. Expected 4, but got " + nrOfStates);
        }

        eigenSystem = createEigenSystem();
        rateMatrix = new double[nrOfStates][nrOfStates];
        relativeRates = new double[nrOfStates * (nrOfStates - 1)];
        storedRelativeRates = new double[nrOfStates * (nrOfStates - 1)];

        rateAC = rateACInput.get();
        rateAG = rateAGInput.get();
        rateAT = rateATInput.get();
        rateCG = rateCGInput.get();
        rateCT = rateCTInput.get();
        rateGT = rateGTInput.get();
    }

    private RealParameter getParameter(Input<RealParameter> parameterInput) throws Exception {
        if (parameterInput.get() != null) {
            return parameterInput.get();
        }
        return new RealParameter("1.0");
    }

    @Override
    protected void setupRelativeRates() {
        relativeRates[0] = rateAC.getArrayValue(); // A->C
        relativeRates[1] = rateAG.getArrayValue(); // A->G
        relativeRates[2] = rateAT.getArrayValue(); // A->T

        relativeRates[3] = rateAC.getArrayValue(); // C->A
        relativeRates[4] = rateCG.getArrayValue(); // C->G
        relativeRates[5] = rateCT.getArrayValue(); // C->T

        relativeRates[6] = rateAG.getArrayValue(); // G->A
        relativeRates[7] = rateCG.getArrayValue(); // G->C
        relativeRates[8] = rateGT.getArrayValue(); // G->T

        relativeRates[9] = rateAT.getArrayValue(); // T->A
        relativeRates[10] = rateCT.getArrayValue(); //T->C
        relativeRates[11] = rateGT.getArrayValue(); //T->G
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Nucleotide;
    }
}
