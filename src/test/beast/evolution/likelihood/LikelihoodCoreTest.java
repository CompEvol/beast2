/**
 * 
 */
package test.beast.evolution.likelihood;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.junit.Test;

import beast.evolution.likelihood.BeerLikelihoodCore;
import junit.framework.TestCase;

/**
 * @author gereon
 *
 */
public class LikelihoodCoreTest extends TestCase {

	@Test
	public void testDifferentCalculationMethods() {
		double[] fromFlexibleImplementation;
		double[] fromBinaryImplementation;
		
		BeerLikelihoodCore lc = new BeerLikelihoodCore(3);
		lc.initialize(7, 5, 1, false, true);
		
		lc.setNodeStates(0, new int[] {0, 1, 0, 1, 4});
		lc.setNodeMatrix(0, 0, new double[] {
				1, 0.5, 0,
				0, 0.5, 0,
				0, 0, 1});
		
		lc.setNodeStates(1, new int[] {0, 0, 1, 1, 4});
		lc.setNodeMatrix(1, 0, new double[] {
				1, 0.5, 0,
				0, 0.5, 0,
				0, 0, 1});
		
		lc.createNodePartials(2);
		lc.calculatePartials(new int[] {0, 1}, 2);
		fromFlexibleImplementation = lc.getNodePartials(2, 10.0);
		lc.calculatePartials(0, 1, 2);
		fromBinaryImplementation = lc.getNodePartials(2, 10.0);
		assertArrayEquals(fromBinaryImplementation, fromFlexibleImplementation, 1e-10);
		lc.setNodeMatrix(2, 0, new double[] {
				0.4, 0.0, 0.0,
				0.3, 0.7, 0.0,
				0.3, 0.3, 1.0});
		
		lc.setNodeStates(3, new int[] {2, 2, 2, 2, 2});
		lc.setNodeMatrix(3, 0, new double[] {
				0.4, 0.0, 0.0,
				0.3, 0.7, 0.0,
				0.3, 0.3, 1.0});
		
		lc.setNodeStates(4, new int[] {2, 2, 2, 2, 2});
		lc.setNodeMatrix(4, 0, new double[] {
				0.4, 0.0, 0.0,
				0.3, 0.7, 0.0,
				0.3, 0.3, 1.0});
		
		lc.createNodePartials(5);
		lc.calculatePartials(new int[] {3, 4}, 5);
		fromFlexibleImplementation = lc.getNodePartials(5, 10.0);
		lc.calculatePartials(3, 4, 5);
		fromBinaryImplementation = lc.getNodePartials(5, 10.0);
		assertArrayEquals(fromBinaryImplementation, fromFlexibleImplementation, 1e-10);
		
		lc.setNodeMatrix(5, 0, new double[] {
				0.4, 0.0, 0.0,
				0.3, 0.7, 0.2,
				0.3, 0.3, 0.8});
		
		lc.createNodePartials(6);
		lc.calculatePartials(new int[] {2, 5}, 6);
		fromFlexibleImplementation = lc.getNodePartials(6, 10.0);
		lc.calculatePartials(2, 5, 6);
		fromBinaryImplementation = lc.getNodePartials(6, 10.0);
		assertArrayEquals(fromBinaryImplementation, fromFlexibleImplementation, 1e-10);
	}

}
