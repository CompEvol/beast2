package test.beast.evolution.substmodel;

import org.junit.jupiter.api.Test;

import beast.base.evolution.substitutionmodel.BinaryCovarion;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;
import cern.colt.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BinaryCovarionModelTest  {
    final static double EPSILON = 1e-6;

	
    /**      
	 * test that equilibrium frequencies are
	 * [ p0 * f0, p1, * f0, p0 * f1, p1, * f1 ]
	 */
	@Test
	public void testEquilibriumFrequencies() {
		Randomizer.setSeed(120);
		doWithTufflySteel();

		for (int i = 0; i < 10; i++) {
			doWithEqualHFreqs("BEAST");
			doWithEqualHFreqs("REVERSIBLE");
		}
		doWithUnEqualHFreqs("REVERSIBLE");
	}
	
	private void doWithTufflySteel() {
	        Frequencies dummyFreqs = new Frequencies();
	        dummyFreqs.initByName("frequencies", "0.25 0.25 0.25 0.25", "estimate", false);
	        BinaryCovarion substModel;

	        double d = 0.05+Randomizer.nextDouble()*0.9;
	        RealParameter vfrequencies = new RealParameter(new Double[]{d, 1.0 - d});
	        RealParameter switchrate = new RealParameter(new Double[]{Randomizer.nextDouble(), Randomizer.nextDouble()});
	        
	        substModel = new BinaryCovarion();
	        substModel.initByName("frequencies", dummyFreqs, 
	        		"vfrequencies", vfrequencies, /* [p0, p1] */
	        		"alpha", "0.01",
	        		"switchRate", switchrate,
	        		//"eigenSystem", "beast.evolution.substitutionmodel.RobustEigenSystem",
	        		"mode", "TUFFLEYSTEEL");
	        
	        double [] matrix = new double[16];
	        substModel.getTransitionProbabilities(null, 1000, 0, 1.0, matrix);
        	double h0 = switchrate.getValue(1) / (switchrate.getValue(0) + switchrate.getValue(1));
        	double h1 = switchrate.getValue(0) / (switchrate.getValue(0) + switchrate.getValue(1));
	        double [] baseFreqs = new double[] {
	    	        vfrequencies.getValue(0) * h0,
	    	        vfrequencies.getValue(1) * h0,
	    	        vfrequencies.getValue(0) * h1,
	    	        vfrequencies.getValue(1) * h1
	        };
	        System.err.println("Expected: " + Arrays.toString(baseFreqs));
	        System.err.println("Calculat: " + Arrays.toString(matrix));
	        for (int j = 0; j < 4; j++) {
	        	assertEquals(baseFreqs[j], matrix[j], 1e-3);
	        }
		}

	private void doWithEqualHFreqs(String mode) {
        Frequencies dummyFreqs = new Frequencies();
        dummyFreqs.initByName("frequencies", "0.25 0.25 0.25 0.25", "estimate", false);
        BinaryCovarion substModel;

        RealParameter hfrequencies = new RealParameter(new Double[]{0.5, 0.5});
        double d = Randomizer.nextDouble();
        RealParameter vfrequencies = new RealParameter(new Double[]{d, 1.0 - d});
        
        substModel = new BinaryCovarion();
        substModel.initByName("frequencies", dummyFreqs, 
        		"hfrequencies", hfrequencies, /* [f0, f1] */
        		"vfrequencies", vfrequencies, /* [p0, p1] */
        		"alpha", "0.01",
        		"switchRate", "0.1",
        		"mode", mode);
        
        double [] matrix = new double[16];
        substModel.getTransitionProbabilities(null, 100, 0, 1.0, matrix);
        double EPSILON = 1e-10;
        assertEquals(vfrequencies.getValue(0) * hfrequencies.getValue(0), matrix[0], EPSILON);
        assertEquals(vfrequencies.getValue(1) * hfrequencies.getValue(0), matrix[1], EPSILON);
        assertEquals(vfrequencies.getValue(0) * hfrequencies.getValue(1), matrix[2], EPSILON);
        assertEquals(vfrequencies.getValue(1) * hfrequencies.getValue(1), matrix[3], EPSILON);
	}

	private void doWithUnEqualHFreqs(String mode) {
        Frequencies dummyFreqs = new Frequencies();
        dummyFreqs.initByName("frequencies", "0.25 0.25 0.25 0.25", "estimate", false);
        BinaryCovarion substModel;

        double d = 0.05+Randomizer.nextDouble()*0.9;
        RealParameter hfrequencies = new RealParameter(new Double[]{d, 1.0 - d});
        d = 0.05+Randomizer.nextDouble()*0.9;
        RealParameter vfrequencies = new RealParameter(new Double[]{d, 1.0 - d});
        
        substModel = new BinaryCovarion();
        substModel.initByName("frequencies", dummyFreqs, 
        		"hfrequencies", hfrequencies, /* [f0, f1] */
        		"vfrequencies", vfrequencies, /* [p0, p1] */
        		"alpha", "0.01",
        		"switchRate", "0.1",
        		//"eigenSystem", "beast.evolution.substitutionmodel.RobustEigenSystem",
        		"mode", mode);
        
        double [] matrix = new double[16];
        substModel.getTransitionProbabilities(null, 1000, 0, 1.0, matrix);
        double [] baseFreqs = new double[] {
	        (vfrequencies.getValue(0) * hfrequencies.getValue(0)),
	        (vfrequencies.getValue(1) * hfrequencies.getValue(0)),
	        (vfrequencies.getValue(0) * hfrequencies.getValue(1)),
	        (vfrequencies.getValue(1) * hfrequencies.getValue(1))
        };
        System.err.println("Expected: " + Arrays.toString(baseFreqs));
        System.err.println("Calculat: " + Arrays.toString(matrix));
        for (int j = 0; j < 4; j++) {
        	assertEquals(baseFreqs[j], matrix[j], 1e-3);
        }
	}
}
