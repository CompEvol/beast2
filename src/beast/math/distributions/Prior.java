package beast.math.distributions;

import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.State;
import beast.core.Valuable;
import beast.core.Input.Validate;

@Description("Produces prior (log) probability of value")
abstract public class Prior extends Distribution {
	public Input<Valuable> m_x = new Input<Valuable>("x","point at which the density is calculated", Validate.REQUIRED); 

	@Override
	public boolean requiresRecalculation() {
		// we only get here when a StateNode input has changed, so are guaranteed recalculation is required.
		try {
			initAndValidate();
			calculateLogP();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override public void sample(State state, Random random) {}
	@Override public List<String> getArguments() {return null;}
	@Override public List<String> getConditions() {return null;}
}
