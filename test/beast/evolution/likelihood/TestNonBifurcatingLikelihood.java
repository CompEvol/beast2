package beast.evolution.likelihood;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.likelihood.BeerLikelihoodCore;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.util.Randomizer;
import beast.util.TreeParser;
import junit.framework.TestCase;
import test.beast.BEASTTestCase;

public class TestNonBifurcatingLikelihood extends TestCase {
	@Test
	public void testUnaryTreeLikelihood() throws Exception {
		// Set up simple asymmetric binary model
		Sequence tip = new Sequence("tip", "011");

		Alignment data = new Alignment();
		data.initByName("sequence", tip, "dataType", "binary", "statecount", "2");

		assertEquals(1, data.getPatternWeight(0));
		assertEquals(2, data.getPatternWeight(1));

		TreeParser tree = new TreeParser();
		tree.initByName("taxa", data, "newick", "((tip:1):1):1;", "IsLabelledNewick", true);

		Frequencies freq = new Frequencies();
		freq.initByName("frequencies", "0.5 0.5");

		GeneralSubstitutionModel lossModel = new GeneralSubstitutionModel();
		lossModel.initByName("rates", "1e-23 1.0", "frequencies", freq);

		SiteModel siteModel = new SiteModel();
		siteModel.initByName("mutationRate", "1.0", "substModel", lossModel);

		System.setProperty("java.only", "true");
		TreeLikelihood likelihood = new TreeLikelihood();

		likelihood.initByName("data", data, "tree", tree, "siteModel", siteModel);
		double logP = likelihood.calculateLogP();

		// Check the likelihood calculation
		double[][] rateMatrix = lossModel.getRateMatrix();
		assertEquals(2, rateMatrix.length);
		// The diagonal entries are 1s...
		assertArrayEquals(new double[] { 1.0, 1.0 }, rateMatrix[0], BEASTTestCase.PRECISION);
		assertArrayEquals(new double[] { 0.0, 1.0 }, rateMatrix[1], BEASTTestCase.PRECISION);
		/*
		 * ... but will be normalized to minus rowsum, so -2 2 0 0 (the 2 is there give
		 * an average of 1 transition per unit time, based on the fixed frequencies of
		 * 0.5), which has eigenvalues 0 and -2
		 */
		assertArrayEquals(new double[] { 0.0, -2.0 }, lossModel.getEigenDecomposition(null).getEigenValues(),
				BEASTTestCase.PRECISION);
		/*
		 * with eigen vectorspaces spanned by eigenvectors [1, 1] (for eigenvalue 0) and
		 * [0, 1] (for eigenvalue -2).
		 */
		assertArrayEquals(new double[] { Math.sqrt(0.5), 0.0, Math.sqrt(0.5), Math.sqrt(2) },
				lossModel.getEigenDecomposition(null).getEigenVectors(), BEASTTestCase.PRECISION);

		/*
		 * The transition probabilities along an edge of length 1 are then
		 * 
		 * exp([[-2, 2], [0, 0]]) = [[1, 0], [1, 1]] * exp(diag[0, -2]) * [[1, 0], [1,
		 * 1]] ^ -1 = [[1, 0], [1, 1]] * diag[1, exp(-2)] * [[1, 0], [1, 1]] ^ -1 = [[1,
		 * 0], [1-1/e^2, 1/e^2]]
		 */
		double[] partials = new double[4];
		likelihood.getLikelihoodCore().getNodePartials(1, partials);
		/*
		 * For the first pattern (0), the likelihood of obtaining it when starting in
		 * state 0 is 1, because our 1→0 transition probability is negligible. The
		 * probability to obtain it when starting in state 1 is 1-p_{1→0}. Getting the
		 * second pattern (1) starting from 0 is impossible (likelihood 0), and the last
		 * entry is the transition probability p_{1→0}.
		 */
		assertArrayEquals(new double[] { 1.0, 1 - Math.exp(-2), 0.0, Math.exp(-2) }, partials, BEASTTestCase.PRECISION);

		/*
		 * Now node2, the root, sits on top of another length 1 branch above node1, and
		 * therefore should have the square of that previous case, and a square of the partials.
		 */
		likelihood.getLikelihoodCore().getNodePartials(2, partials);
		assertArrayEquals(new double[] { 1.0, 1 - Math.exp(-4), 0.0, Math.exp(-4) }, partials, BEASTTestCase.PRECISION);
	}

	@Test
	public void testLikelihoodBinaryAndGenericSS() throws Exception {
		int nrOfStates = 3;
		BeerLikelihoodCore core = new BeerLikelihoodCore(nrOfStates);
		core.initialize(1, 1, 1, false, true);

		int[] states1 = new int[] { 0 };
		double[] matrices1 = new double[] { 0.9, 0.0, 0.1, 0.5, 0.5, 0.0, 0.0, 0.0, 1.0 };
		int[] states2 = new int[] { 0 };
		double[] matrices2 = new double[] { 0.9, 0.0, 0.1, 0.5, 0.5, 0.0, 0.0, 0.0, 1.0 };
		double[] partials3 = new double[] { 0.0, 0.0, 0.0 };
		core.calculateStatesStatesPruning(states1, matrices1, states2, matrices2, partials3);
		double[] partials3new = new double[] { 0.0, 0.0, 0.0 };
		core.calculatePartialsPruning((List<double[]>) new ArrayList<double[]>(0),
				(List<double[]>) new ArrayList<double[]>(0), Arrays.asList(new int[][] { states1, states2 }),
				Arrays.asList(new double[][] { matrices1, matrices2 }), partials3new);
		assertArrayEquals(partials3, partials3new, BEASTTestCase.PRECISION);
		assertArrayEquals(new double[] { 0.81, 0.25, 0.0 }, partials3new, BEASTTestCase.PRECISION);
	}

	@Test
	public void testLikelihoodBinaryAndGenericSS2Patterns() throws Exception {
		int nrOfStates = 3;
		BeerLikelihoodCore core = new BeerLikelihoodCore(nrOfStates);
		core.initialize(1, 2, 1, false, true);

		int[] states1 = new int[] { 0, 1 };
		double[] matrices1 = new double[] { 0.9, 0.0, 0.1, 0.5, 0.5, 0.0, 0.0, 0.0, 1.0 };
		int[] states2 = new int[] { 0, 0 };
		double[] matrices2 = new double[] { 0.9, 0.0, 0.1, 0.5, 0.5, 0.0, 0.0, 0.0, 1.0 };
		double[] partials3 = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
		core.calculateStatesStatesPruning(states1, matrices1, states2, matrices2, partials3);
		double[] partials3new = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
		core.calculatePartialsPruning((List<double[]>) new ArrayList<double[]>(0),
				(List<double[]>) new ArrayList<double[]>(0), Arrays.asList(new int[][] { states1, states2 }),
				Arrays.asList(new double[][] { matrices1, matrices2 }), partials3new);
		assertArrayEquals(partials3, partials3new, BEASTTestCase.PRECISION);
		assertArrayEquals(new double[] { 0.81, 0.25, 0.0, 0.0, 0.25, 0.0 }, partials3new, BEASTTestCase.PRECISION);
	}

	@Test
	public void testLikelihoodBinaryAndGenericPS() throws Exception {
		int nrOfStates = 3;
		BeerLikelihoodCore core = new BeerLikelihoodCore(nrOfStates);
		core.initialize(1, 1, 1, false, true);
		double r1 = Randomizer.nextDouble();
		double r2 = Randomizer.nextDouble();

		double[] partials1 = new double[] { r1, r2 * (1 - r1), (1 - r2) * (1 - r1) };
		double[] matrices1 = new double[] { 0.9, 0.0, 0.1, 0.5, 0.5, 0.0, 0.0, 0.0, 1.0 };
		int[] states2 = new int[] { 0 };
		double[] matrices2 = new double[] { 0.9, 0.0, 0.1, 0.5, 0.5, 0.0, 0.0, 0.0, 1.0 };
		double[] partials3 = new double[] { 0.0, 0.0, 0.0 };
		core.calculateStatesPartialsPruning(states2, matrices2, partials1, matrices1, partials3);
		double[] partials3new = new double[] { 0.0, 0.0, 0.0 };
		core.calculatePartialsPruning(Arrays.asList(new double[][] { partials1 }),
				Arrays.asList(new double[][] { matrices1 }), Arrays.asList(new int[][] { states2 }),
				Arrays.asList(new double[][] { matrices2 }), partials3new);
		assertArrayEquals(partials3, partials3new, BEASTTestCase.PRECISION);
		assertEquals((r1 * 0.9 + (1 - r2) * (1 - r1) * 0.1) * 0.9, partials3[0]);
	}

	@Test
	public void testLikelihoodBinaryAndGenericPP() throws Exception {
		int nrOfStates = 3;
		BeerLikelihoodCore core = new BeerLikelihoodCore(nrOfStates);
		core.initialize(1, 1, 1, false, true);
		double r1 = Randomizer.nextDouble();
		double r2 = Randomizer.nextDouble();
		double r3 = Randomizer.nextDouble();
		double r4 = Randomizer.nextDouble();

		double[] partials1 = new double[] { r1, r2 * (1 - r1), (1 - r2) * (1 - r1) };
		double[] matrices1 = new double[] { 0.9, 0.0, 0.1, 0.5, 0.5, 0.0, 0.0, 0.0, 1.0 };
		double[] partials2 = new double[] { r3, r4 * (1 - r3), (1 - r4) * (1 - r3) };
		double[] matrices2 = new double[] { 0.9, 0.0, 0.1, 0.5, 0.5, 0.0, 0.0, 0.0, 1.0 };
		double[] partials3 = new double[] { 0.0, 0.0, 0.0 };
		core.calculatePartialsPartialsPruning(partials1, matrices1, partials2, matrices2, partials3);
		double[] partials3new = new double[] { 0.0, 0.0, 0.0 };
		core.calculatePartialsPruning(Arrays.asList(new double[][] { partials1, partials2 }),
				Arrays.asList(new double[][] { matrices1, matrices2 }), (List<int[]>) new ArrayList<int[]>(0),
				(List<double[]>) new ArrayList<double[]>(0), partials3new);
		assertArrayEquals(partials3, partials3new, BEASTTestCase.PRECISION);
		assertEquals((r1 * 0.9 + (1 - r2) * (1 - r1) * 0.1) * (r3 * 0.9 + (1 - r4) * (1 - r3) * 0.1), partials3[0]);
	}
}
