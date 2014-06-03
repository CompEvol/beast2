package test.beast.core;

import java.util.ArrayList;
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
		
		String description = BEASTObject.getDescription(beasti);
		assertEquals("class that impements BEASTInterface but is not a BEASTObject", description);
		
		List<BEASTInterface> predecessors = new ArrayList<BEASTInterface>();
		BEASTObject.getPredecessors(beasti2, predecessors);
		assertEquals(1, predecessors.size());
		BEASTObject.getPredecessors(beasti, predecessors);
		assertEquals(2, predecessors.size());
		assertEquals(beasti, predecessors.get(1));
		assertEquals(beasti2, predecessors.get(0));
		
		description = BEASTObject.getTipText(beasti, "other");
		assertEquals("link to another BEASTi object", description);
		
		boolean b = BEASTObject.isPrimitive(beasti, "value");
		assertEquals(true, b);
		b = BEASTObject.isPrimitive(beasti, "other");
		assertEquals(false, b);
		
		List<BEASTInterface> plugins = BEASTObject.listActivePlugins(beasti);
		assertEquals(1, plugins.size());
		assertEquals(beasti2, plugins.get(0));
		plugins = BEASTObject.listActivePlugins(beasti2);
		assertEquals(0, plugins.size());
		
		
		BEASTObject.validateInputs(beasti);

		try {
			BEASTObject.validateInputs(beasti2);
			assertEquals(true, false); // should never get here
		} catch (Throwable t) {
			// lucky to be here
		}
		
		
		BEASTObject.setInputValue(beasti2, "value", "Goodbye!");
		String msg = (String) BEASTObject.getInputValue(beasti2, "value");
		assertEquals("Goodbye!", msg);
		
	}
	
	
	
}
