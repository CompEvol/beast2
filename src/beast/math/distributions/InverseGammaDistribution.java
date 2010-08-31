package beast.math.distributions;

import beast.math.GammaFunction;
import beast.math.distributions.Distribution;


/**
 * inverse gamma distribution.
 * <p/>
 * (Parameters: shape, scale; mean: ??; variance: ??)
 *
 * @author Joseph Heled
 * @version $Id$
 */
public class InverseGammaDistribution implements Distribution {

    private double shape, scale;

    //private  double factor;
    private  double logFactor;

    public InverseGammaDistribution(double shape, double scale) {
        this.shape = shape;
        this.scale = scale;
        setLocals();
    }

    private void setLocals() {
        //this.factor = Math.pow(scale, shape) / Math.exp(GammaFunction.lnGamma(shape));
        this.logFactor = shape * Math.log(scale) - GammaFunction.lnGamma(shape);

    }
    public double getShape() {
        return shape;
    }

    public void setShape(double value) {
        shape = value;
        setLocals();
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double value) {
        scale = value;
        setLocals();
    }

    public double pdf(double x) {
        return Math.exp(logPdf(x));
    }

    public double logPdf(double x) {
        if (x <= 0)
            return Double.NEGATIVE_INFINITY;

        return logFactor - (scale/x + (shape+1)*Math.log(x));
    }

    public double cdf(double x) {
        return GammaFunction.incompleteGammaQ(shape, scale/x);
    }

    public double quantile(double y) {
        // this is what R thinks
        final GammaDistribution g = new GammaDistribution(shape, scale);
        return 1/g.quantile(1-y);
    }

    public double mean() {
        if( shape > 1 ) {
            return scale / (shape - 1);
        }
        return Double.POSITIVE_INFINITY;
    }

    public double variance() {
        if( shape > 2 ) {
            return scale*scale / ((shape - 1)*(scale-1)*(scale-2));
        }
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public org.apache.commons.math.distribution.Distribution getProbabilityDensity() {
        return null;  // no such thing yet in common math - should implement ...
    }
}
