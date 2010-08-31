package beast.math.distributions;

/**
 * chi-square distribution
 * (distribution of sum of squares of n N(0,1) random variables)
 * <p/>
 * (Parameter: n; mean: n; variance: 2*n)
 * <p/>
 * The chi-square distribution is a special case of the Gamma distribution
 * (shape parameter = n/2.0, scale = 2.0).
 *
 * @author Korbinian Strimmer
 * @author Joseph Heled
 */
public class ChiSquareDistribution extends GammaDistribution {
    /**
     * @param n sum of n normals
     */
    public ChiSquareDistribution(double n) {
        super(n / 2.0, 2.0);
    }

    public void setN(double n) {
        setShape(n/2.0);
        setScale(2.0);
    }
}
