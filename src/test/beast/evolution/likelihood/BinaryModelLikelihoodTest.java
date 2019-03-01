/**
 * 
 */
package test.beast.evolution.likelihood;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.junit.Test;

import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.tree.Tree;
import beast.util.TreeParser;
import junit.framework.TestCase;

/**
 * @author gereon
 *
 */
public class BinaryModelLikelihoodTest extends TestCase {

	@Test
	public void testGetTransitionProbabilities() {

		RealParameter f = new RealParameter(new Double[] { 0.5, 0.5 });

		Frequencies freqs = new Frequencies();
		freqs.initByName("frequencies", f, "estimate", false);

		SubstitutionModel.Base sub = new GeneralSubstitutionModel();
		sub.initByName("frequencies", freqs, "rates", new RealParameter(new Double[] { 0.9, 0.1 }));

		Tree tree = new TreeParser("(t1:2,(t2:1,t3:1):1):1;");
		Alignment data = new Alignment(
				Arrays.asList(
						new Sequence[] { new Sequence("t1", "0"), new Sequence("t2", "1"), new Sequence("t3", "0") }),
				"binary");

		TreeLikelihood likelihood = new TreeLikelihood();
		SiteModel siteModel = new SiteModel();
		siteModel.initByName("substModel", sub);

		likelihood.initByName("data", data, "tree", tree, "siteModel", siteModel);
		likelihood.calculateLogP();
		double[][] ps;

		double[][] expected = new double[][] {
			{ 1.0, 0.0 },
			{ 0.0, 1.0 },
			{ 1.0, 0.0 },
			{ 0.9516123464449933, 0.04838765355500671 },
			{ 0.576127908395163, 0.4238720916048369 }
		};
		for (int i : new int[] { 0, 1, 2, 3, 4 }) {
			ps = likelihood.getMarginalProbabilities(tree.getNode(i));
			double[] p = ps[0];
			assertArrayEquals(expected[i], p, 1e-10);
			System.out.println(Arrays.toString(p));
		}
	}
}