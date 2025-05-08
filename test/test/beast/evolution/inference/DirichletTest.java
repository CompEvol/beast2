package test.beast.evolution.inference;

import beast.base.inference.distribution.Dirichlet;
import beast.base.inference.parameter.RealParameter;
import org.apache.commons.math.special.Gamma;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class DirichletTest {
    @Test
    void normalisedTest() {
        Dirichlet d = new Dirichlet();

        Double[] alpha = new Double[]{1.0, 1.0, 1.0, 1.0, 1.0};
        RealParameter a = new RealParameter(alpha);
        d.alphaInput.setValue(a, d );
        d.initAndValidate();

        int n = alpha.length;

        // Valid Dirichlet vector: sum to 1
        Double[] x = new Double[]{0.2, 0.2, 0.2, 0.2, 0.2};
        RealParameter p = new RealParameter(x);
        double f0 = d.calcLogP(p);

        // Compute expected log density
        double sumAlpha = 0.0;
        for (int i = 0; i < n; i++) {
            sumAlpha += alpha[i];
        }

        double logGammaSumAlpha = Gamma.logGamma(sumAlpha);

        double sumLogGammaAlpha = 0.0;
        for (int i = 0; i < n; i++) {
            sumLogGammaAlpha += Gamma.logGamma(alpha[i]);
        }

        double sumLogX = 0.0;
        for (int i = 0; i < n; i++) {
            sumLogX += (alpha[i] - 1.0) * Math.log(x[i]);
        }

        double exp = logGammaSumAlpha - sumLogGammaAlpha + sumLogX;

        assertEquals(exp, f0, 1e-6);
    }

    @Test
    void notNormalisedTest() {
        Dirichlet d = new Dirichlet();

        Double[] alpha = new Double[]{1.0, 1.0, 1.0, 1.0, 1.0};
        RealParameter a = new RealParameter(alpha);
        d.alphaInput.setValue(a, d );
        d.initAndValidate();

        int n = alpha.length;

        // Define x whose sum is sumX (not 1)
        Double[] x = new Double[]{1.0, 1.0, 1.0, 1.0, 1.0}; // sum = 5 (sumX=5)
        RealParameter p = new RealParameter(x);
        double f0 = d.calcLogP(p);

        // Compute sumX = sum(x)
        double sumX = 0.0;
        for (int i = 0; i < n; i++) {
            sumX += x[i];
        }

        // Compute standard log density for x_normalised
        double sumAlpha = 0.0;
        for (int i = 0; i < n; i++) {
            sumAlpha += alpha[i];
        }

        double logGammaSumAlpha = Gamma.logGamma(sumAlpha);

        double sumLogGammaAlpha = 0.0;
        for (int i = 0; i < n; i++) {
            sumLogGammaAlpha += Gamma.logGamma(alpha[i]);
        }

        // Normalised x (so xi / sumX)
        double sumLogX = 0.0;
        for (int i = 0; i < n; i++) {
            sumLogX += (alpha[i] - 1.0) * Math.log(x[i] / sumX);
        }

        double log_density_standard = logGammaSumAlpha - sumLogGammaAlpha + sumLogX;

        // Apply Jacobian correction: -(n-1) * log(sumX)
        double log_density_scaled = log_density_standard - (n - 1) * Math.log(sumX);

        assertEquals(log_density_scaled, f0, 1e-6);
    }
}
