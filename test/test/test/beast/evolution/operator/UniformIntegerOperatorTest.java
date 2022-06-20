package test.beast.evolution.operator;

import java.util.Arrays;

import beast.base.inference.State;
import beast.base.inference.operator.UniformOperator;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UniformIntegerOperatorTest  {
    private final int dimension = 3;
    private int[][] count;

//    public static Test suite() {
//        return new TestSuite(UniformIntegerOperatorTest.class);
//    }

    public void testParameterBound() {
    	try {
	        count = new int[dimension][4]; // 4 vaules {0, 1, 2, 3}
	        RealParameter parameter = new RealParameter(new Double[]{1.0, 0.0, 2.0});
	        parameter.setLower(0.0);
	        parameter.setUpper(3.0);

    		State state = new State();
    		state.initByName("stateNode", parameter);
    		state.initialise();
    		
	        UniformOperator uniformOperator = new UniformOperator();
	        uniformOperator.initByName("parameter", parameter, "howMany", 3, "weight", 1.0);

	        for (int i = 0; i < 400; i++) {
	            uniformOperator.proposal();
	            Double [] values = parameter.getValues();
	            for (int k = 0; k < values.length; k++) {
	                int j = (int)(double) values[k];
	                count[k][j] += 1; 
	            }
	        }
	        System.out.println("Discretized real distributions lower = 0.0, upper = 3.0");
	        for (int j = 0; j < count.length; j++) {
		        System.out.println("x[" +j + "] = " + Arrays.toString(count[j]));
			}
	
	        assertTrue((count[0][0] > 0) && (count[0][1] > 0) && (count[0][2] > 0) && (count[0][3] == 0), "Expected count[0][0-2] > 0 && count[0][3] == 0");
	        assertTrue((count[1][0] > 0) && (count[1][1] > 0) && (count[1][2] > 0) && (count[1][3] == 0), "Expected count[1][0-2] > 0 && count[1][3] == 0");
	        assertTrue((count[2][0] > 0) && (count[2][1] > 0) && (count[2][2] > 0) && (count[2][3] == 0), "Expected count[2][0-2] > 0 && count[2][3] == 0");
    	} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public void testIntegerParameterBound() {
    	try {
	        count = new int[dimension][4]; // 4 vaules {0, 1, 2, 3}
	        IntegerParameter parameter = new IntegerParameter(new Integer[]{1, 0, 3});
	        parameter.setLower(0);
	        parameter.setUpper(3);

    		State state = new State();
    		state.initByName("stateNode", parameter);
    		state.initialise();
    		
	        UniformOperator uniformOperator = new UniformOperator();
	        uniformOperator.initByName("parameter", parameter, "howMany", 3, "weight", 1.0);
	       
	        for (int i = 0; i < 400; i++) {
	            uniformOperator.proposal();
	            Integer [] values = parameter.getValues();
	            for (int k = 0; k < values.length; k++) {
	                int j = values[k];
	                count[k][j] += 1; 
	            }
	        }

	        System.out.println("Integer distributions lower = 0, upper = 3");
	        for (int j = 0; j < count.length; j++) {
		        System.out.println("x[" +j + "] = " + Arrays.toString(count[j]));
			}
	
	        assertTrue(count[0][0] > 0 && count[0][1] > 0 && count[0][2] > 0 && count[0][3] > 0, "Expected count[0][0-3] > 0");
	        assertTrue(count[1][0] > 0 && count[1][1] > 0 && count[1][2] > 0 && count[1][3] > 0, "Expected count[1][0-3] > 0");
	        assertTrue(count[2][0] > 0 && count[2][1] > 0 && count[2][2] > 0 && count[2][3] > 0, "Expected count[2][0-3] > 0");
    	} catch (Exception e) {
			e.printStackTrace();
		}
    }

}
