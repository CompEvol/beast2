package beast.math.distributions;


import java.util.List;
import java.util.Random;

import org.apache.commons.math.util.MathUtils;

import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.core.Valuable;
import beast.core.parameter.RealParameter;

@Description("Poisson distribution, used as prior  f(k; lambda)=\\frac{lambda^k e^{-lambda}}{k!}  " +
		"If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
		"separate independent component.")
public class Poisson extends Prior {
	public Input<RealParameter> m_lambda = new Input<RealParameter>("lambda","rate parameter, defaults to 1"); 
		
	double m_fLambda;
	
	@Override
	public void initAndValidate() {
		if (m_lambda.get() == null) {
			m_fLambda = 1;
		} else {
			m_fLambda = m_lambda.get().getValue();
			if (m_fLambda < 0) {
				System.err.println("Poisson::Lambda should be positive not "+m_fLambda+". Assigning default value.");
				m_fLambda = 1;
			}
		}
	}

	@Override
	public double calculateLogP() {
		logP = 0;
		Valuable pX = m_x.get();
		for (int i = 0; i < pX.getDimension(); i++) {
			int fX = (int) pX.getArrayValue(i);
			logP += -m_fLambda * fX - MathUtils.factorialLog(fX);
		}
		logP += Math.log(m_fLambda) * pX.getDimension();
		return logP;
	}
	
	@Override
	public boolean requiresRecalculation() {
		// we only get here when a StateNode input has changed, so are guaranteed recalculation is required.
		initAndValidate();
		calculateLogP();
		return true;
	}
	
	@Override public void sample(State state, Random random) {}
	@Override public List<String> getArguments() {return null;}
	@Override public List<String> getConditions() {return null;}

} // class Poisson
