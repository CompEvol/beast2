package test.beast.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import beast.base.evolution.Sum;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;


public class SumTest  {

	
	@Test
	public void testSum() {
		RealParameter p1 = new RealParameter("1.0 2.0");
		Sum sum = new Sum();
		
		// single argument sum
		sum.initByName("arg", p1);
		double v = sum.getArrayValue();
		assertEquals(3.0, v, 1e-10);
		
		// multiple argument sum
		sum = new Sum();
		RealParameter p2 = new RealParameter("2.0 2.5");
		sum.initByName("arg", p1, "arg", p2);
		v = sum.getArrayValue();
		assertEquals(7.5, v, 1e-10);

		// multiple same argument sum
		sum = new Sum();
		sum.initByName("arg", p1, "arg", p1);
		v = sum.getArrayValue();
		assertEquals(6.0, v, 1e-10);
		
		// sum of integers
		IntegerParameter p3 = new IntegerParameter("1 2 5");
		sum = new Sum();
		sum.initByName("arg", p3);
		v = sum.getArrayValue();
		assertEquals(8.0, v, 1e-10);

		// sum of boolean
		BooleanParameter p4 = new BooleanParameter("true false false true true");
		sum = new Sum();
		sum.initByName("arg", p4);
		v = sum.getArrayValue();
		assertEquals(3.0, v, 1e-10);

		// sum of booleans and integer
		sum = new Sum();
		sum.initByName("arg", p4, "arg", p3);
		v = sum.getArrayValue();
		assertEquals(11.0, v, 1e-10);

		// sum of booleans and real
		sum = new Sum();
		sum.initByName("arg", p1, "arg", p4);
		v = sum.getArrayValue();
		assertEquals(6.0, v, 1e-10);
	}
}
