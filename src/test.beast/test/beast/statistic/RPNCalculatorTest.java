package test.beast.statistic;

import org.junit.Test;

import beast.base.core.BEASTInterface;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.inference.util.RPNcalculator;
import beast.base.parser.XMLParser;
import beast.base.parser.XMLParserException;
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


	@Test
	public void testRPNMultiplyBoolean() {
		BooleanParameter p1 = new BooleanParameter("false true false true");
		p1.setID("p1");
		RealParameter p2 = new RealParameter("5. 6. 7. 8.");
		p2.setID("p2");
		
		RPNcalculator calculator = new RPNcalculator();
		calculator.initByName("parameter", p1, "parameter", p2, "expression", "p2 p1 *");
		
		double [] result = calculator.getDoubleValues();
		assertEquals(0.0, result[0], 1e-16);
		assertEquals(6.0, result[1], 1e-16);
		assertEquals(0.0, result[2], 1e-16);
		assertEquals(8.0, result[3], 1e-16);
	}

	@Test
	public void testRPNXMLParser() throws XMLParserException {
		XMLParser parser = new XMLParser();
		String xml = 
				"<parameter id='p1' spec='beast.evolution.parameter.BooleanParameter' value='0 1 0 1'/>\n" +
			    "<parameter id='p2' value='5. 6. 7. 8.'/>\n" +
			    "<calculator id='calculator' spec='beast.inference.util.RPNcalculator' expression='p1 p2 *'>\n" +
			    "    <parameter idref='p1'/>\n" +
			    "    <parameter idref='p2'/>\n" +
			    "</calculator>"
		;
				
		BEASTInterface o = parser.parseBareFragment(xml, true);
		RPNcalculator calculator = (RPNcalculator) o;

		double [] result = calculator.getDoubleValues();
		assertEquals(0.0, result[0], 1e-16);
		assertEquals(6.0, result[1], 1e-16);
		assertEquals(0.0, result[2], 1e-16);
		assertEquals(8.0, result[3], 1e-16);
	}


}


