package beast.base.evolution.substitutionmodel;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.tree.Node;

@Description("Mutation Death substitution model, can be used as Stochastic Dollo model.")
public class MutationDeathModel extends SubstitutionModel.Base {

    final public Input<Function> delParameter = new Input<>("deathprob", "rate of death, used to calculate death probability", Validate.REQUIRED);
    // mutation rate is already provided in SiteModel, so no need to duplicate it here
    //public Input<RealParameter> mutationRate = new Input<>("mu", "mutation rate, default 1");
    final public Input<SubstitutionModel.Base> CTMCModelInput = new Input<>("substmodel", "CTMC Model for the life states, so should have " +
            "a state-space one less than this model. If not specified, ...");
    // TODO: figure out the end of the last sentence

    /**
     * transition matrix for live states *
     */
    protected double[] trMatrix;
    /**
     * number of states *
     */
    int nrOfStates;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        double[] freqs = getFrequencies();
        nrOfStates = freqs.length;
        trMatrix = new double[(nrOfStates - 1) * (nrOfStates - 1)];
        if (CTMCModelInput.get() != null) {
            if (CTMCModelInput.get().frequenciesInput.get().freqs.length != nrOfStates - 1) {
                throw new IllegalArgumentException("substmodel does not have the correct state space: should be " + (nrOfStates - 1));
            }
        }
    }

    @Override
    public EigenDecomposition getEigenDecomposition(Node node) {
        return null;
    }

    @Override
    public void getTransitionProbabilities(Node node, double startTime, double endTime, double rate, double[] matrix) {
        double distance = (startTime - endTime) * rate;
        int i, j;
        // assuming that expected number of changes in CTMCModel is 1 per unit time
        // we are contributing s*deathRate number of changes per unit of time
        double deathProb = Math.exp(-distance * delParameter.get().getArrayValue());
        double mutationR = 2;
//        if (mutationRate.get() != null) {
//            mutationR *= mutationRate.get().getValue();
//        }
        double freqs[] = getFrequencies();

        for (i = 0; i < freqs.length - 1; ++i) {
            mutationR *= freqs[i];
        }
        SubstitutionModel.Base CTMCModel = CTMCModelInput.get();
        if (CTMCModel != null) {
            CTMCModel.getTransitionProbabilities(node, startTime, endTime, mutationR * rate, trMatrix);
        } else {
            trMatrix[0] = 1.0;
        }

        for (i = 0; i < nrOfStates - 1; ++i) {
            for (j = 0; j < nrOfStates - 1; j++) {
                matrix[i * (nrOfStates) + j] = trMatrix[i * (nrOfStates - 1) + j] * deathProb;
            }
            matrix[i * (nrOfStates) + j] = (1.0 - deathProb);
        }

        for (j = 0; j < nrOfStates - 1; ++j) {
            matrix[nrOfStates * (nrOfStates - 1) + j] = 0.0;
        }

        matrix[nrOfStates * nrOfStates - 1] = 1.0;
    } // getTransitionProbabilities

    /**
     * CalculationNode implementation *
     */
    @Override
    protected boolean requiresRecalculation() {
        // we only get here if delParameter or mutationRate is dirty
        return true;
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
    	if (CTMCModelInput.get() == null) {
    		return dataType.getStateCount() == 2;
    	} else {
    		int states = CTMCModelInput.get().nrOfStates;
    		return dataType.getStateCount() == states + 1;
    	}
    }

    @Override
    public boolean canReturnComplexDiagonalization() {
    	return true;
    }
} // class MutationDeathModel
