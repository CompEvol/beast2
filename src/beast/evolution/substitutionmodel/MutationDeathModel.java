package beast.evolution.substitutionmodel;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;

@Description("Mutation Death substitution model, can be used as Stochastic Dollo model.")
public class MutationDeathModel extends SubstitutionModel.Base {

    public Input<RealParameter> delParameter = new Input<RealParameter>("deathprob", "rate of death, used to calculate death probability", Validate.REQUIRED);
    public Input<RealParameter> mutationRate = new Input<RealParameter>("mu", "mutation rate, default 1");
    
    protected double[] trMatrix;
    int stateCount;
    
    @Override
    public void initAndValidate() {
		double [] freqs = getFrequencies();
		stateCount = freqs.length;
        trMatrix = new double[(stateCount - 1) * (stateCount - 1)];
    }
    
    @Override
	public EigenDecomposition getEigenDecomposition() {
		return null;
	}

	@Override
    //public void getTransitionProbabilities(double distance, double[] matrix) {
    public void getTransitionProbabilities(Node node, double fStartTime, double fEndTime, double fRate, double[] matrix) {
      	double distance = (fStartTime - fEndTime) * fRate;
        int i, j;
        // assuming that expected number of changes in CTMCModel is 1 per unit time
        // we are contributing s*deathRate number of changes per unit of time
        double deathProb = Math.exp(-distance * delParameter.get().getValue());
        double mutationR = 2;
        if (mutationRate.get() != null) {
            mutationR *= mutationRate.get().getValue();
        }
        double freqs[] = getFrequencies();

        for (i = 0; i < freqs.length - 1; ++i) {
            mutationR *= freqs[i];
        }
//        if (CTMCModel != null) {
//            CTMCModel.getTransitionProbabilities(mutationR * distance, trMatrix);
//        } else {
            trMatrix[0] = 1.0;
//        }

        for (i = 0; i < stateCount - 1; ++i) {
            for (j = 0; j < stateCount - 1; j++) {
                matrix[i * (stateCount) + j] = trMatrix[i * (stateCount - 1) + j] * deathProb;
            }
            matrix[i * (stateCount) + j] = (1.0 - deathProb);
        }

        for (j = 0; j < stateCount - 1; ++j) {
            matrix[stateCount * (stateCount - 1) + j] = 0.0;
        }

        matrix[stateCount * stateCount - 1] = 1.0;
    } // getTransitionProbabilities
	
	@Override
	protected boolean requiresRecalculation() {
	   	// we only get here if delParameter or mutationRate is dirty
	   	return true;
	}
} // class MutationDeathModel
