package beast.math.distributions;

import beast.math.GammaFunction;
import org.apache.commons.math.distribution.GammaDistributionImpl;

/**
 * @author Korbinian Strimmer
 * @author Gerton Lunter
 * @author Joseph Heled
 *         Date: 1/09/2010
 */
public class GammaDistribution implements Distribution {
    private double shape;
    private double scale;

    public GammaDistribution(double shape, double scale) {
        this.shape = shape;
        this.scale = scale;
    }

    public double getShape() {
        return shape;
    }

    public void setShape(double value) {
        shape = value;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double value) {
        scale = value;
    }

    @Override
    public double pdf(double x) {
        return Math.exp(logPdf(x));
    }

    @Override
    public double logPdf(double x) {
        // AR - changed this to return -ve inf instead of throwing an
        // exception... This makes things
        // much easier when using this to calculate log likelihoods.
        // if (x < 0) throw new IllegalArgumentException();
        if (x < 0)
            return Double.NEGATIVE_INFINITY;

        if (x == 0) {
            if (shape == 1.0)
                return Math.log(1.0 / scale);
            else
                return Double.NEGATIVE_INFINITY;
        }
        if (shape == 1.0) {
            return (-x / scale) - Math.log(scale);
        }
        if (shape == 0.0)  // uninformative
            return -Math.log(x);

        return ((shape - 1.0) * Math.log(x / scale) - x / scale -
                GammaFunction.lnGamma(shape)) - Math.log(scale);
    }

    @Override
    public double cdf(double x) {
        return GammaFunction.incompleteGammaP(shape, x / scale);
    }

    @Override
    public double quantile(double y) {
        return 0.5 * scale * pointChi2(y, 2.0 * shape);
    }

    @Override
    public double mean() {
        return scale * shape;
    }

    @Override
    public double variance() {
        return scale * scale * shape;
    }

    @Override
    public org.apache.commons.math.distribution.GammaDistribution getProbabilityDensity() {
        return new GammaDistributionImpl(shape, scale);
    }


    // magic code from BEAST 1
    private static double pointChi2(double prob, double v) {
        // Returns z so that Prob{x<z}=prob where x is Chi2 distributed with df = v
        // RATNEST FORTRAN by
        // Best DJ & Roberts DE (1975) The percentage points of the
        // Chi2 distribution. Applied Statistics 24: 385-388. (AS91)

        final double e = 0.5e-6, aa = 0.6931471805, p = prob;
        double ch, a, q, p1, p2, t, x, b, s1, s2, s3, s4, s5, s6;
        double epsi = .01;
        if( p < 0.000002 || p > 1 - 0.000002)  {
            epsi = .000001;
        }
        // if (p < 0.000002 || p > 0.999998 || v <= 0) {
        //      throw new IllegalArgumentException("Arguments out of range p" + p + " v " + v);
        //  }
        double g = GammaFunction.lnGamma(v / 2);
        double xx = v / 2;
        double c = xx - 1;
        if (v < -1.24 * Math.log(p)) {
            ch = Math.pow((p * xx * Math.exp(g + xx * aa)), 1 / xx);
            if (ch - e < 0) {
                return ch;
            }
        } else {
            if (v > 0.32) {
                x = new NormalDistribution(0, 1).quantile(p);
                p1 = 0.222222 / v;
                ch = v * Math.pow((x * Math.sqrt(p1) + 1 - p1), 3.0);
                if (ch > 2.2 * v + 6) {
                    ch = -2 * (Math.log(1 - p) - c * Math.log(.5 * ch) + g);
                }
            } else {
                ch = 0.4;
                a = Math.log(1 - p);

                do {
                    q = ch;
                    p1 = 1 + ch * (4.67 + ch);
                    p2 = ch * (6.73 + ch * (6.66 + ch));
                    t = -0.5 + (4.67 + 2 * ch) / p1
                            - (6.73 + ch * (13.32 + 3 * ch)) / p2;
                    ch -= (1 - Math.exp(a + g + .5 * ch + c * aa) * p2 / p1)
                            / t;
                } while (Math.abs(q / ch - 1) - epsi > 0);
            }
        }
        do {
            q = ch;
            p1 = 0.5 * ch;
            if ((t = GammaFunction.incompleteGammaP(xx, p1, g)) < 0) {
                throw new IllegalArgumentException("Arguments out of range: t < 0");
            }
            p2 = p - t;
            t = p2 * Math.exp(xx * aa + g + p1 - c * Math.log(ch));
            b = t / ch;
            a = 0.5 * t - b * c;

            s1 = (210 + a * (140 + a * (105 + a * (84 + a * (70 + 60 * a))))) / 420;
            s2 = (420 + a * (735 + a * (966 + a * (1141 + 1278 * a)))) / 2520;
            s3 = (210 + a * (462 + a * (707 + 932 * a))) / 2520;
            s4 = (252 + a * (672 + 1182 * a) + c * (294 + a * (889 + 1740 * a))) / 5040;
            s5 = (84 + 264 * a + c * (175 + 606 * a)) / 2520;
            s6 = (120 + c * (346 + 127 * c)) / 5040;
            ch += t
                    * (1 + 0.5 * t * s1 - b
                    * c
                    * (s1 - b
                    * (s2 - b
                    * (s3 - b
                    * (s4 - b * (s5 - b * s6))))));
        } while (Math.abs(q / ch - 1) > e);

        return (ch);
    }
}
