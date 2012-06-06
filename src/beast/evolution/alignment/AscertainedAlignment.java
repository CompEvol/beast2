package beast.evolution.alignment;

import java.util.HashSet;
import java.util.Set;

import beast.core.Description;
import beast.core.Input;

@Description("Alignemnt that allows ascertainment correction")
public class AscertainedAlignment extends Alignment {
    public Input<Integer> m_excludefrom = new Input<Integer>("excludefrom", "first site to condition on, default 0", 0);
    public Input<Integer> m_excludeto = new Input<Integer>("excludeto", "last site to condition on, default 0", 0);
    public Input<Integer> m_excludeevery = new Input<Integer>("excludeevery", "interval between sites to condition on (default 1)", 1);

// RRB: Note that all commented code is stuff to support inclusion-sites,
// so don't delete them.
//	public Input<Integer> m_includefrom = new Input<Integer>("includefrom","first site to condition on, default 0", 0);
//	public Input<Integer> m_includeto = new Input<Integer>("includeto","last site to condition on, default 0", 0);
//	public Input<Integer> m_includeevery = new Input<Integer>("includeevery","interval between sites to condition on (default 1)", 1);

    /**
     * indices of patterns that are excluded from the likelihood calculation
     * and used for ascertainment correction
     */
    Set<Integer> m_nExcludedPatterns;
//	List<Integer> m_nIncluded;

    @Override
    public void initAndValidate() throws Exception {
        super.initAndValidate();

        int iFrom = m_excludefrom.get();
        int iTo = m_excludeto.get();
        int iEvery = m_excludeevery.get();
        m_nExcludedPatterns = new HashSet<Integer>();
        for (int i = iFrom; i < iTo; i += iEvery) {
            int iPattern = m_nPatternIndex[i];
            // reduce weight, so it does not confuse the tree likelihood
            m_nWeight[iPattern] = 0;
            m_nExcludedPatterns.add(iPattern);
        }

//		iFrom = m_includefrom.get();
//		iTo = m_includeto.get();
//		iEvery = m_includeevery.get();
//		m_nIncluded = new ArrayList<Integer>();
//		for (int i = iFrom; i < iTo; i += iEvery) {
//			int iPattern = m_nPatternIndex[i];
//			// reduce weight, so it does not confuse the tree likelihood
//			m_nWeight[iPattern] = 0;
//			m_nIncluded.add(iPattern);
//		}
    } // initAndValidate

    public Set<Integer> getExcludedPatternIndices() {
        return m_nExcludedPatterns;
    }

    public int getExcludedPatternCount() {
        return m_nExcludedPatterns.size();
    }

//	public List<Integer> getIncludesIndices() {
//		return m_nIncluded;
//	}

    public double getAscertainmentCorrection(double[] patternLogProbs) {
        double excludeProb = 0, includeProb = 0, returnProb = 1.0;

//        for (int i = 0; i < m_nIncluded.size(); i++) {
//        	includeProb += Math.exp(patternLogProbs[m_nIncluded.get(i)]);
//        }

        for (int i : m_nExcludedPatterns) {
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
