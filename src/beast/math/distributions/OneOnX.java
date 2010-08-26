package beast.math.distributions;



import beast.core.Description;
import beast.core.Valuable;

@Description("OneOnX distribution, used as prior.  f(x) = C/x for some normalizing constant C. " +
		"If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
		"separate independent component.")
public class OneOnX extends Prior {
		
	
	@Override
	public void initAndValidate() {
	}

	// log of the constant 1/sqrt(2.PI)
	final static double C = -Math.log(2.0*Math.PI)/2.0;

	@Override
	public double calculateLogP() {
		Valuable pX = m_x.get();
		logP = 0;
		for (int i = 0; i < pX.getDimension(); i++) {
			logP -= Math.log(pX.getArrayValue(i));
		}
		return logP;
	}
	

} // class OneOnX
