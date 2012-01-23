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
    public Input<RealParameter> m_rateACInput = new Input<RealParameter>("rateAC", "substitution rate for A to C (default 1)");
    public Input<RealParameter> m_rateAGInput = new Input<RealParameter>("rateAG", "substitution rate for A to G (default 1)");
    public Input<RealParameter> m_rateATInput = new Input<RealParameter>("rateAT", "substitution rate for A to T (default 1)");
    public Input<RealParameter> m_rateCGInput = new Input<RealParameter>("rateCG", "substitution rate for C to G (default 1)");
    public Input<RealParameter> m_rateCTInput = new Input<RealParameter>("rateCT", "substitution rate for C to T (default 1)");
    public Input<RealParameter> m_rateGTInput = new Input<RealParameter>("rateGT", "substitution rate for G to T (default 1)");

    RealParameter m_rateAC;
    RealParameter m_rateAG;
    RealParameter m_rateAT;
    RealParameter m_rateCG;
    RealParameter m_rateCT;
    RealParameter m_rateGT;

    public GTR() {
        m_rates.setRule(Validate.OPTIONAL);
    }

    @Override
    public void initAndValidate() throws Exception {
        if (m_rates.get() != null) {
            throw new Exception("the rates attribute should not be used. Use the individual rates rateAC, rateCG, etc, instead.");
        }

        m_frequencies = frequenciesInput.get();
        updateMatrix = true;
        m_nStates = m_frequencies.getFreqs().length;
        if (m_nStates != 4) {
            throw new Exception("Frequencies has wrong size. Expected 4, but got " + m_nStates);
        }

        eigenSystem = createEigenSystem();
        m_rateMatrix = new double[m_nStates][m_nStates];
        relativeRates = new double[m_nStates * (m_nStates - 1)];
        storedRelativeRates = new double[m_nStates * (m_nStates - 1)];

        m_rateAC = getParameter(m_rateACInput);
        m_rateAG = getParameter(m_rateAGInput);
        m_rateAT = getParameter(m_rateATInput);
        m_rateCG = getParameter(m_rateCGInput);
        m_rateCT = getParameter(m_rateCTInput);
        m_rateGT = getParameter(m_rateGTInput);
    }

    private RealParameter getParameter(Input<RealParameter> parameterInput) throws Exception {
        if (parameterInput.get() != null) {
            return parameterInput.get();
        }
        return new RealParameter("1.0");
    }

    @Override
    protected void setupRelativeRates() {
        relativeRates[0] = m_rateAC.getValue(); // A->C
        relativeRates[1] = m_rateAG.getValue(); // A->G
        relativeRates[2] = m_rateAT.getValue(); // A->T

        relativeRates[3] = m_rateAC.getValue(); // C->A
        relativeRates[4] = m_rateCG.getValue(); // C->G
        relativeRates[5] = m_rateCT.getValue(); // C->T

        relativeRates[6] = m_rateAG.getValue(); // G->A
        relativeRates[7] = m_rateCG.getValue(); // G->C
        relativeRates[8] = m_rateGT.getValue(); // G->T

        relativeRates[9] = m_rateAT.getValue(); // T->A
        relativeRates[10] = m_rateCT.getValue(); //T->C
        relativeRates[11] = m_rateGT.getValue(); //T->G
    }

    @Override
    public boolean canHandleDataType(DataType dataType) throws Exception {
        if (dataType instanceof Nucleotide) {
            return true;
        }
        throw new Exception("Can only handle nucleotide data");
    }
}
