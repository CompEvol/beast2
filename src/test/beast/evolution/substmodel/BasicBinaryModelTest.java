/**
 * 
 */
package test.beast.evolution.substmodel;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import beast.core.parameter.RealParameter;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.evolution.substitutionmodel.SubstitutionModel;
import junit.framework.TestCase;

/**
 * @author gereon
 *
 */
public class BasicBinaryModelTest extends TestCase {

    public interface Instance {
        Double[] getPi();
        Double [] getRates();
        double getDistance();
        double[] getExpectedResult();
    }

	protected Instance test1 = new Instance() {
		@Override
		public Double[] getPi() {
			return new Double[] { 0.1, 0.9 };
		}

		@Override
		public Double[] getRates() {
			return new Double[] { 1.0, 1.0 };
		}

		@Override
		public double getDistance() {
			return 100.0;
		}

		@Override
		public double[] getExpectedResult() {
			return new double[] { 0.1, 0.9, 0.1, 0.9 };
		}
	};

	Instance[] all = { test1 };

	@Test
	public void testGetTransitionProbabilities() {
		for (Instance test : all) {

			RealParameter f = new RealParameter(test.getPi());

			Frequencies freqs = new Frequencies();
			freqs.initByName("frequencies", f, "estimate", false);

			SubstitutionModel.Base sub = new GeneralSubstitutionModel();
			Double[] rates = test.getRates();
			sub.initByName("frequencies", freqs,
					"rates", new RealParameter(rates));

			double distance = test.getDistance();

			double[] mat = new double[2 * 2];
			sub.getTransitionProbabilities(null, distance, 0, 1, mat);
			final double[] result = test.getExpectedResult();

			for (int k = 0; k < mat.length; ++k) {
				System.out.format("%3d: %g (%g)\n", k, mat[k], result[k]);
			}
			assertArrayEquals(result, mat, 1e-10);
		}
	}
}
