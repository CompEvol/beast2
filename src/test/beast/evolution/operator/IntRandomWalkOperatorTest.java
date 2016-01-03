package test.beast.evolution.operator;


import java.util.Arrays;

import org.junit.Test;

import beast.core.State;
import beast.core.parameter.IntegerParameter;
import beast.evolution.operators.IntRandomWalkOperator;
import junit.framework.TestCase;

public class IntRandomWalkOperatorTest extends TestCase {

    @Test
    public void testIntRandomWalkDistribution3x10() {
    	instantiate(3, 10, 1,new Integer[]{1,2,3,6,10});
    }

    @Test
    public void testIntRandomWalkDistribution3x6() {
    	instantiate(3, 6, 1,new Integer[]{1,2,3,6});
    }

    @Test
    public void testIntRandomWalkDistribution6x10() {
    	instantiate(6, 10, 1,new Integer[]{1,2,3,6,10});
    }

    public void instantiate(int dimension, int upper, int runs, Integer[]windowSizes) {
    	for (int r = 0; r < runs; r++) {
    	for (Integer windowSize : windowSizes) {
	    	try {
	    	    int[][] count = new int[dimension][upper + 1];
		        Integer [] init = new Integer[dimension];
		        Arrays.fill(init, 0);
		        IntegerParameter parameter = new IntegerParameter(init);
		        parameter.setLower(0);
		        parameter.setUpper(upper);
	
	    		State state = new State();
	    		state.initByName("stateNode", parameter);
	    		state.initialise();
	    		
		        IntRandomWalkOperator operator = new IntRandomWalkOperator();
		        operator.initByName("parameter", parameter, "windowSize", windowSize, "weight", 1.0);
	
		        for (int i = 0; i < 1000000 * (upper + 1); i++) {
		            operator.proposal();
		            Integer [] values = parameter.getValues();
		            for (int k = 0; k < values.length; k++) {
		                int j = values[k];
		                count[k][j] += 1; 
		            }
		        }
		        System.out.print("Distribution lower = 0, upper = " + upper +" windowSize = " + windowSize);
		        for (int j = 0; j < count.length; j++) {
			        //System.out.println("x[" +j + "] = " + Arrays.toString(count[j]));
				}
		
		        int sum = 0;
		        for (int i = 0; i < dimension; i++) {
		        	for (int k = 0; k < count[i].length; k++) {
		        		sum += Math.abs(count[i][k] - 1000000);
					}
		        }
		        System.out.println(" Average deviation: " + sum/(dimension * (upper + 1)));
		        assertTrue("average deviation (" + sum/(dimension * (upper + 1)) + ") exceeds 10000", sum/(dimension * (upper + 1)) < 10000); 
	    	} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	}
    }


}
