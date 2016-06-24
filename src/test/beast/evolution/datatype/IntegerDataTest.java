package test.beast.evolution.datatype;

import org.junit.Test;

import beast.evolution.datatype.IntegerData;
import beast.util.Randomizer;
import junit.framework.TestCase;;

public class IntegerDataTest extends TestCase {

	
	@Test
	public void testIntegerData() {
		IntegerData datatype = new IntegerData();
		assertEquals("?", datatype.getCode(-1));
		assertEquals("0", datatype.getCode(0));
		assertEquals("1", datatype.getCode(1));
		assertEquals("10", datatype.getCode(10));
		assertEquals("123", datatype.getCode(123));
		Randomizer.setSeed(127);
		for (int i = 0; i < 100; i++) {
			int state = Randomizer.nextInt(100000000);
			int x = state;
	    	String str = "";
	    	while (state > 0) {
	    		str = (char)('0' + state%10) + str;
	    		state /= 10;
	    	}
			assertEquals(str, datatype.getCode(x));
		}
	}
}
