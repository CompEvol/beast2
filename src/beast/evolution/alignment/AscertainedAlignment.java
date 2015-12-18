package beast.evolution.alignment;

import java.util.HashSet;
import java.util.Set;

import beast.core.Description;
import beast.core.Input;



@Description("Alignemnt that allows ascertainment correction")
/**
 * This class has merged with Alignment
 * @deprecated use Alignment() instead setting isAscertainedInput to true.
 */
@Deprecated
public class AscertainedAlignment extends Alignment {
//    public Input<Integer> excludefromInput = new Input<>("excludefrom", "first site to condition on, default 0", 0);
//    public Input<Integer> excludetoInput = new Input<>("excludeto", "last site to condition on (but excluding this site), default 0", 0);
//    public Input<Integer> excludeeveryInput = new Input<>("excludeevery", "interval between sites to condition on (default 1)", 1);

// RRB: Note that all commented code is stuff to support inclusion-sites,
// so don't delete them.
//	public Input<Integer> m_includefrom = new Input<>("includefrom","first site to condition on, default 0", 0);
//	public Input<Integer> m_includeto = new Input<>("includeto","last site to condition on, default 0", 0);
//	public Input<Integer> m_includeevery = new Input<>("includeevery","interval between sites to condition on (default 1)", 1);

    /**
     * indices of patterns that are excluded from the likelihood calculation
     * and used for ascertainment correction
     */
    Set<Integer> excludedPatterns;
//	List<Integer> m_nIncluded;

    @Override
    public void initAndValidate() throws Exception {
        super.initAndValidate();

        int iFrom = excludefromInput.get();
        int iTo = excludetoInput.get();
        int iEvery = excludeeveryInput.get();
        excludedPatterns = new HashSet<>();
        for (int i = iFrom; i < iTo; i += iEvery) {
            int iPattern = patternIndex[i];
            // reduce weight, so it does not confuse the tree likelihood
            patternWeight[iPattern] = 0;
            excludedPatterns.add(iPattern);
        }

//		iFrom = m_includefrom.get();
//		iTo = m_includeto.get();
//		iEvery = m_includeevery.get();
//		m_nIncluded = new ArrayList<>();
//		for (int i = iFrom; i < iTo; i += iEvery) {
//			int iPattern = m_nPatternIndex[i];
//			// reduce weight, so it does not confuse the tree likelihood
//			m_nWeight[iPattern] = 0;
//			m_nIncluded.add(iPattern);
//		}
    } // initAndValidate

    public Set<Integer> getExcludedPatternIndices() {
        return excludedPatterns;
    }

    public int getExcludedPatternCount() {
        return excludedPatterns.size();
    }

//	public List<Integer> getIncludesIndices() {
//		return m_nIncluded;
//	}

    public double getAscertainmentCorrection(double[] patternLogProbs) {
        double excludeProb = 0, includeProb = 0, returnProb = 1.0;

//        for (int i = 0; i < m_nIncluded.size(); i++) {
//        	includeProb += Math.exp(patternLogProbs[m_nIncluded.get(i)]);
//        }

        for (int i : excludedPatterns) {
            excludeProb += Math.exp(patternLogProbs[i]);
        }

        if (includeProb == 0.0) {
            returnProb -= excludeProb;
        } else if (excludeProb == 0.0) {
            returnProb = includeProb;
        } else {
            returnProb = includeProb - excludeProb;
        }
        return Math.log(returnProb);
    } // getAscertainmentCorrection

} // class AscertainedAlignment
