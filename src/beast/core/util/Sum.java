package beast.core.util;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.core.Input.Validate;

@Description("calculates sum of a valuable")
public class Sum extends CalculationNode implements Valuable {
	public Input<Valuable> m_value = new Input<Valuable>("arg", "argument to be summed", Validate.REQUIRED);

	boolean m_bRecompute = true;
	double m_fSum = 0;
	double m_fStoredSum = 0;
	
	@Override
	public void initAndValidate() {}
	
	@Override
	public int getDimension() {
		return 1;
	}

	@Override
	public double getArrayValue() {
		if (m_bRecompute) {
			compute();
		}
		return m_fSum;
	}
	
	/** do the actual work, and reset flag **/
	void compute() {
		m_fSum = 0;
		final Valuable v = m_value.get();
		for (int i = 0; i < v.getDimension(); i++) {
			m_fSum += v.getArrayValue(i);
		}
		m_bRecompute = false;
	}
	
	@Override
	public double getArrayValue(int iDim) {
		if (iDim == 0) {return getArrayValue();}
		return Double.NaN;
	}

	/** CalculationNode methods **/
	@Override
	public void store() {
		m_fStoredSum = m_fSum;
		super.store();
	}
	@Override
	public void restore() {
		m_fSum = m_fStoredSum;
		super.restore();
	}
	@Override
	public boolean requiresRecalculation() {
		m_bRecompute = true;
		return true;
	}

} // class Sum
