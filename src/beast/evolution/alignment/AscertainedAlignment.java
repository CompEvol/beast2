package beast.evolution.alignment;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;

@Description("Alignemnt that allows ascertainment correction")
public class AscertainedAlignment extends Alignment {
	public Input<Integer> m_from = new Input<Integer>("from","first site to condition on", Validate.REQUIRED);
	public Input<Integer> m_to = new Input<Integer>("to","last site to condition on", Validate.REQUIRED);
	public Input<Integer> m_every = new Input<Integer>("every","interval between sites to condition on (default 1)", 1);
	
	/** indices of patterns that are excluded from the likelihood calculation
	 * and used for ascertainment correction
	 */
	List<Integer> m_nExcluded;
	
	@Override
	public void initAndValidate() throws Exception {
		super.initAndValidate();

		int iFrom = m_from.get();
		int iTo = m_to.get();
		int iEvery = m_every.get();
		m_nExcluded = new ArrayList<Integer>();
		
		for (int i = iFrom; i < iTo; i += iEvery) {
			int iPattern = m_nPatternIndex[i];
			// reduce weight, so it does not confuse the tree likelihood
			m_nWeight[i]--;
			m_nExcluded.add(iPattern);
		}
	} // initAndValidate

	
	
    public double getAscertainmentCorrection(double[] patternLogProbs) {
        double excludeProb = 0, includeProb = 0, returnProb = 1.0;

//        int[] includeIndices = getIncludePatternIndices();
//        int[] excludeIndices = getExcludePatternIndices();
//        for (int i = 0; i < getIncludePatternCount(); i++) {
//            int index = includeIndices[i];
//            includeProb += Math.exp(patternLogProbs[index]);
//        }
        for (int i = 0; i < m_nPatterns.length; i++) {
        	// excluded sites have weight reduced by 1
        	includeProb += m_nWeight[i] * Math.exp(patternLogProbs[i]);
        }
        
//        for (int j = 0; j < getExcludePatternCount(); j++) {
//            int index = excludeIndices[j];
//            excludeProb += Math.exp(patternLogProbs[index]);
//        }
        for (int i = 0; i < m_nExcluded.size(); i++) {
        	excludeProb += Math.exp(patternLogProbs[m_nExcluded.get(i)]);
        }
//        if (includeProb == 0.0) {
//            returnProb -= excludeProb;
//        } else if (excludeProb == 0.0) {
//            returnProb = includeProb;
//        } else {
//            returnProb = includeProb - excludeProb;
//        }
        returnProb = includeProb - excludeProb;
        return Math.log(returnProb);
    } // getAscertainmentCorrection

} // class AscertainedAlignment
