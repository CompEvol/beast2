package test.beast.evolution.operator;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import beast.core.parameter.RealParameter;
import beast.evolution.operators.CompoundParameterHelper;
import junit.framework.TestCase;

public class CompoundParameterHelperTest extends TestCase {

	
	@Test
	public void testCompoundParameterHelper() {
		RealParameter p1 = new RealParameter("1.0 2.0");
		RealParameter p2 = new RealParameter("3.0 4.0 5.0");
		List<RealParameter> list = new ArrayList<>();
		list.add(p1);
		list.add(p2);
		
		CompoundParameterHelper<Double> cph = new CompoundParameterHelper(list);
		
		// prints 5
		System.out.println("Dim = " + cph.getDimension());
		// results in java.lang.ArrayIndexOutOfBoundsException: 2		
		for (int i = 0; i < cph.getDimension(); i++) {
			System.out.println("value[" + i + "] = " +cph.getValue(i));
		}
		
		assertEquals(5, cph.getDimension());
		for (int i = 0; i < 4; i++) {
			assertEquals(i+1.0, cph.getValue(i), 1e-15);
		}
	}
}
