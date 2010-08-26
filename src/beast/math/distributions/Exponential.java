package beast.math.distributions;



import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.core.parameter.RealParameter;

@Description("Exponential distribution, used as prior  f(x;\\lambda) = \\lambda e^{-\\lambda x}, if x >= 0 " +
		"If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
		"separate independent component.")
public class Exponential extends Prior {
	public Input<RealParameter> m_lambda = new Input<RealParameter>("lambda","rate parameter, defaults to 1"); 
		
	double m_fLambda;
	
	
	
	@Override
	public void initAndValidate() {
		if (m_lambda.get() == null) {
			m_fLambda = 1;
		} else {
			m_fLambda = m_lambda.get().getValue();
			if (m_fLambda < 0) {
				System.err.println("Exponential::Lambda should be positive not "+m_fLambda+". Assigning default value.");
				m_fLambda = 1;
			}
		}
	}


	@Override
	public double calculateLogP() {
		logP = 0;
		Valuable pX = m_x.get();
		for (int i = 0; i < pX.getDimension(); i++) {
			double fX = pX.getArrayValue(i);
			logP += -m_fLambda * fX;
		}
		logP += Math.log(m_fLambda) * pX.getDimension();
		return logP;
	}
	
} // class Normal
