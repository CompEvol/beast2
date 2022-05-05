package test.beast.core;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import beast.core.*;
import beast.core.Input.Validate;
import org.junit.rules.ExpectedException;

public class InputTest {

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

	@Rule
	public final ExpectedException exception = ExpectedException.none();
	@Test
	public void testScientificNotationDecimalFailure() {

		exception.expect(java.lang.RuntimeException.class);
		exception.expectMessage("Failed to set the string value to '1e-1' for beastobject id=null");

		BEASTi o = new BEASTi();
		o.initByName("intValue", "1e-1");
	}

	@Test
	public void testScientificNotationIntTooLarge() {
		exception.expect(java.lang.RuntimeException.class);
		exception.expectMessage("Failed to set the string value to '" + Integer.MAX_VALUE + "0' for beastobject id=null");

		BEASTi o = new BEASTi();
		o.initByName("intValue", Integer.MAX_VALUE + "0" );
	}
}
