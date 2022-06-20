package test.beast.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;

import beast.base.core.BEASTObject;
import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.*;
import test.beast.core.InputTest.BEASTi;


public class InputTest  {

	@Description("class that impements BEASTInterface but is not a BEASTObject")
	@Citation("this is a dummy citation")
	public class BEASTi extends BEASTObject {
		final public Input<String> msgInput = new Input<>("value", "message for this BEASTi object", Validate.OPTIONAL);
		final public Input<String> msg2Input = new Input<>("value2", "message2 for this BEASTi object", Validate.OPTIONAL);
		final public Input<Integer> intValue = new Input<>("intValue", "Integer message for this BEASTi object", Validate.OPTIONAL);
		final public Input<Long> longValue = new Input<>("longValue", "Long message for this BEASTi object", Validate.OPTIONAL);
		final public Input<Double> doubleValue = new Input<>("doubleValue", "Double message2 for this BEASTi object", Validate.OPTIONAL);

		@Override
		public void initAndValidate() {
			// nothing to do;
		}
	}

	@Test
	public void testOptionalInput() {
		// following should pass without error message
		Input<String> msgInput = new Input<>("value", "message for this BEASTi object", Validate.OPTIONAL);
		msgInput.validate();
		
		// following should pass without error message
		BEASTi o = new BEASTi();
		o.initByName("value","ok");

	}

	@Test
	public void testScienticNotation() {
		BEASTi o = new BEASTi();
		o.initByName("intValue", "5e5");
		o.initByName("longValue", "5e5");
		o.initByName("doubleValue", "5e5");

		Assert.assertEquals(500000, (int) o.intValue.get());
		Assert.assertEquals(500000, (long) o.longValue.get());
		Assert.assertEquals(500000.0, (double) o.doubleValue.get(), 1);
	}

	@Test
	public void testScientificNotationDecimalFailure() {
		BEASTi o = new BEASTi();
		try {
			o.initByName("intValue", "1e-1");
		} catch (RuntimeException e) {
			assertEquals("Failed to set the string value to '1e-1' for beastobject id=null", e.getMessage());
			return;
		}
		assertEquals("Should not get here", null);
	}

	@Test
	public void testScientificNotationIntTooLarge() {
		BEASTi o = new BEASTi();
		try {
			o.initByName("intValue", Integer.MAX_VALUE + "0" );
		} catch (RuntimeException e) {
			assertEquals("Failed to set the string value to '" + Integer.MAX_VALUE + "0' for beastobject id=null", e.getMessage());
			return;
		}
		assertEquals("Should not get here", null);
	}

}
