package beast.evolution.substitutionmodel;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.Nucleotide;

@Description("General Time Reversible model of nucleotide evolution. " +
        "Rates that are not specified are assumed to be 1. ")
public class GTR extends GeneralSubstitutionModel {
    public Input<RealParameter> rateACInput = new Input<RealParameter>("rateAC", "substitution rate for A to C (default 1)");
    public Input<RealParameter> rateAGInput = new Input<RealParameter>("rateAG", "substitution rate for A to G (default 1)");
    public Input<RealParameter> rateATInput = new Input<RealParameter>("rateAT", "substitution rate for A to T (default 1)");
    public Input<RealParameter> rateCGInput = new Input<RealParameter>("rateCG", "substitution rate for C to G (default 1)");
    public Input<RealParameter> rateCTInput = new Input<RealParameter>("rateCT", "substitution rate for C to T (default 1)");
    public Input<RealParameter> rateGTInput = new Input<RealParameter>("rateGT", "substitution rate for G to T (default 1)");

    RealParameter rateAC;
    RealParameter rateAG;
    RealParameter rateAT;
    RealParameter rateCG;
    RealParameter rateCT;
    RealParameter rateGT;

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

        rateAC = getParameter(rateACInput);
        rateAG = getParameter(rateAGInput);
        rateAT = getParameter(rateATInput);
        rateCG = getParameter(rateCGInput);
        rateCT = getParameter(rateCTInput);
        rateGT = getParameter(rateGTInput);
    }

    private RealParameter getParameter(Input<RealParameter> parameterInput) throws Exception {
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
    public boolean canHandleDataType(DataType dataType) throws Exception {
        if (dataType instanceof Nucleotide) {
            return true;
        }
        throw new Exception("Can only handle nucleotide data");
    }
}
