package test.beast.core.parameter;

import org.junit.Test;

import beast.core.parameter.RealParameter;
import junit.framework.TestCase;

public class ParameterTest extends TestCase {

	@Test
	public void testParamter() throws Exception {
		RealParameter parameter = new RealParameter();
		parameter.initByName("value","1.27 1.9");
		assertEquals(parameter.getDimension(), 2);
		parameter.setDimension(5);
		assertEquals(parameter.getDimension(), 5);
		assertEquals(parameter.getValue(0), parameter.getValue(2));
		assertEquals(parameter.getValue(0), parameter.getValue(4));
		assertEquals(parameter.getValue(1), parameter.getValue(3));
		assertNotSame(parameter.getValue(0), parameter.getValue(1));
		try {
			parameter.setValue(2,2.0); // this will throw an exception
			assertNotSame(parameter.getValue(0), parameter.getValue(2));
		} catch (Exception e) {
			// setValue is not allowed for StateNode not in State
		}
		Double [] x = {1.0, 2.0, 3.0, 2.0, 4.0, 5.5};
		parameter = new RealParameter(x);
		assertEquals(parameter.getDimension(), 6);
	}
}
