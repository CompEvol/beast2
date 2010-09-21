package beast.evolution.operators;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.StateNode;
import beast.core.Input.Validate;
import beast.util.Randomizer;

@Description("This element represents an operator that scales two parameters in different directions. " +
		"Each operation involves selecting a scale uniformly at random between scaleFactor and 1/scaleFactor. " +
		"The up parameter is multiplied by this scale and the down parameter is divided by this scale.")
public class UpDownOperator extends Operator {

	public Input<Double> m_scaleFactor = new Input<Double>("scalefactor",
			"magnitude factor used for scaling", Validate.REQUIRED);
	public Input<List<StateNode>> m_up = new Input<List<StateNode>>("up",
			"zero or more items to scale upwards", new ArrayList<StateNode>());
	public Input<List<StateNode>> m_down = new Input<List<StateNode>>("down",
			"zero or more items to scale downwards", new ArrayList<StateNode>());

	double m_fScaleFactor;

	@Override
	public void initAndValidate() {
		m_fScaleFactor = m_scaleFactor.get();
	}

	/**
	 * override this for proposals,
	 * 
	 * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal
	 *         should not be accepted
	 **/
	@Override
	public final double proposal() {

		final double scale = (m_fScaleFactor + (Randomizer.nextDouble() * ((1.0 / m_fScaleFactor) - m_fScaleFactor)));
		int goingUp = 0, goingDown = 0;

		try {
			for (StateNode up : m_up.get()) {
				up = up.getCurrent();
				goingUp += up.scale(scale);
			}

			for (StateNode down : m_down.get()) {
				down = down.getCurrent();
				goingDown += down.scale(1.0 / scale);
			}
		} catch (Exception e) {
			// scale resulted in invalid StateNode, abort proposal
			return Double.NEGATIVE_INFINITY;
		}
		return (goingUp - goingDown - 2) * Math.log(scale);
	}

	/**
	 * automatic parameter tuning *
	 */
	@Override
	public void optimize(double logAlpha) {
		Double fDelta = calcDelta(logAlpha);
		fDelta += Math.log(1.0 / m_fScaleFactor - 1.0);
		m_fScaleFactor = 1.0 / (Math.exp(fDelta) + 1.0);
	}

} // class UpDownOperator
