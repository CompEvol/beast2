package test.beast.core;

import org.junit.Test;

import beast.base.core.BEASTObject;
import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.*;
import junit.framework.TestCase;

public class InputTest extends TestCase {

	@Description("class that impements BEASTInterface but is not a BEASTObject")
	@Citation("this is a dummy citation")
	public class BEASTi extends BEASTObject {
		final public Input<String> msgInput = new Input<>("value", "message for this BEASTi object", Validate.OPTIONAL);
		final public Input<String> msg2Input = new Input<>("value2", "message2 for this BEASTi object", Validate.OPTIONAL);

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
	
}
