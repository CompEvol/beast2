package test.beast.statistic;

import org.junit.Test;

import beast.core.parameter.RealParameter;
import beast.math.statistic.RPNcalculator;
import junit.framework.TestCase;

public class RPNCalculatorTest extends TestCase {

	
	@Test
	public void testRPNDivide() {
		RealParameter p1 = new RealParameter("2.5");
		p1.setID("p1");
		RealParameter p2 = new RealParameter("5.");
		p2.setID("p2");
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", p1, "parameter", p2, "expression", "p2 p1 /");
		
		double result = calculator.getArrayValue();
		assertEquals(2.0, result, 1e-16);
	}

	@Test
	public void testRPNDivideSwitchID() {
		RealParameter p1 = new RealParameter("2.5");
		p1.setID("p2");
		RealParameter p2 = new RealParameter("5.");
		p2.setID("p1");
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", p1, "parameter", p2, "expression", "p2 p1 /");
		
		double result = calculator.getArrayValue();
		assertEquals(0.5, result, 1e-16);
	}

	@Test
	public void testRPNDivideMultiDim() {
		RealParameter p1 = new RealParameter("2.5 1.0 5.");
		p1.setID("p1");
		RealParameter p2 = new RealParameter("5. 1.0 2.5");
		p2.setID("p2");
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", p1, "parameter", p2, "expression", "p2 p1 /");
		
		double result = calculator.getArrayValue();
		assertEquals(2.0, result, 1e-16);
		result = calculator.getArrayValue(1);
		assertEquals(1.0, result, 1e-16);
		result = calculator.getArrayValue(2);
		assertEquals(0.5, result, 1e-16);
	}
	
	@Test
	public void testRPNDivideSpaceInID() {
		RealParameter p1 = new RealParameter("2.5");
		p1.setID("p 1");
		RealParameter p2 = new RealParameter("5.");
		p2.setID("p 2");
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", p1, "parameter", p2, "expression", "x2 x1 /", "argnames", "x1,x2");
		
		double result = calculator.getArrayValue();
		assertEquals(2.0, result, 1e-16);
	}


	@Test
	public void testRPNDivideSpaceInIDMultiDim() {
		RealParameter p1 = new RealParameter("2.5 1.0 5.");
		p1.setID("p 1");
		RealParameter p2 = new RealParameter("5. 1.0 2.5");
		p2.setID("p 2");
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", p1, "parameter", p2, "expression", "x2 x1 /", "argnames", "x1,x2");
		
		double result = calculator.getArrayValue();
		assertEquals(2.0, result, 1e-16);
		result = calculator.getArrayValue(1);
		assertEquals(1.0, result, 1e-16);
		result = calculator.getArrayValue(2);
		assertEquals(0.5, result, 1e-16);
	}
}
