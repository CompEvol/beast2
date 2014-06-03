package test.beast.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import beast.core.BEASTInterface;
import beast.core.BEASTObject;
import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import junit.framework.TestCase;

public class BEASTInterfaceTest extends TestCase {

	@Description("class that impements BEASTInterface but is not a BEASTObject")
	@Citation("this is a dummy citation")
	public class BEASTi implements BEASTInterface {
		public Input<String> msgInput = new Input<String>("value", "message for this BEASTi object", Validate.REQUIRED);
		public Input<BEASTi> beastiInput = new Input<BEASTi>("other", "link to another BEASTi object");
		String ID;
		Set<BEASTInterface> outputs = new HashSet<BEASTInterface>();

		@Override
		public String getID() {
			return ID;
		}

		@Override
		public void setID(String ID) {
			this.ID = ID;
		}

		@Override
		public Set getOutputs() {
			return outputs;
		}

		@Override
		public void initAndValidate() throws Exception {
			// nothting to do;
		}
	}
	
	
	@Test
	public void testBEASTi() throws Exception {
		BEASTi beasti = new BEASTi();
		
		Citation citation = BEASTObject.getCitation(beasti);
		assertEquals("this is a dummy citation", citation.value());

		BEASTObject.initByName(beasti, "value", "hello world");
		Input<String> input = (Input<String>) BEASTObject.getInput(beasti, "value");
		assertEquals("hello world", input.get());
		
		List<?> list = BEASTObject.listInputs(beasti);
		assertEquals(2, list.size());

		BEASTi beasti2 = new BEASTi();
		BEASTObject.initByName(beasti, "value", "hello world",
				"other", beasti2);
		
		BEASTi beasti3 = (BEASTi) BEASTObject.getInputValue(beasti, "other");
		assertEquals(beasti2, beasti3);
		
		assertEquals(1, beasti2.getOutputs().size());
	}
	
	
	
}
