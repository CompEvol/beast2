package beast.math.distributions;

import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.Valuable;
import beast.core.Input.Validate;

@Description("Produces prior (log) probability of value x." +
		"If x is multidimensional, the components of x are assumed to be independent, " +
		"so the sum of log probabilities of all elements of x is returned as the prior.")
abstract public class Prior extends Distribution {
	public Input<Valuable> m_x = new Input<Valuable>("x","point at which the density is calculated", Validate.REQUIRED); 
	public Input<ParametricDistribution> m_distInput = new Input<ParametricDistribution>("distribution","distribution used to calculate prior, e.g. normal, beta, gamma.", Validate.REQUIRED); 

	/** shadows m_distInput **/
	ParametricDistribution m_dist;
	
	@Override 
	public void initAndValidate() {
		m_dist = m_distInput.get();
	}

	@Override
	public double calculateLogP() throws Exception {
		Valuable pX = m_x.get();
		logP = m_dist.calcLogP(pX);
		return logP;
	}
	
	@Override
	public boolean requiresRecalculation() {
		return true;
	}

	@Override public void sample(State state, Random random) {}
	@Override public List<String> getArguments() {return null;}
	@Override public List<String> getConditions() {return null;}
}
