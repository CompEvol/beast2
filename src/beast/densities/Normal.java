package beast.densities;


import beast.core.Description;
import beast.core.Input;
import beast.core.Valuable;
import beast.math.distributions.Distribution;
import beast.math.distributions.NormalDistribution;

@Description("Normal distribution. f(x) = frac{1}{\\sqrt{2\\pi\\sigma^2}} e^{ -\\frac{(x-\\mu)^2}{2\\sigma^2} }")
public class Normal extends ParametricDistribution {
	public Input<Valuable> m_mean = new Input<Valuable>("mean","mean of the normal distribution, defaults to 0");
	public Input<Valuable> m_sigma = new Input<Valuable>("sigma","variance of the normal distribution, defaults to 1");
		
	double m_fMean;
	double m_fSigma;
	
	@Override
	public void initAndValidate() {
		if (m_mean.get() == null) {
			m_fMean = 0;
		} else {
			m_fMean = m_mean.get().getArrayValue();
		}
		if (m_sigma.get() == null) {
			m_fSigma = 1;
		} else {
			m_fSigma = m_sigma.get().getArrayValue();
		}
	}

    private static NormalDistribution distribution = new NormalDistribution(0,1);

    @Override
    public Distribution getDistribution() {
        initAndValidate();

        distribution.setMean(m_fMean);
        distribution.setSD(Math.sqrt(m_fSigma));
        return distribution;
    }
}
