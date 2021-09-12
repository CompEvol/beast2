package test.beast.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import beast.base.core.BEASTInterface;
import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import junit.framework.TestCase;

public class BEASTInterfaceTest extends TestCase {

	@Description("class that impements BEASTInterface but is not a BEASTObject")
	@Citation("this is a dummy citation")
	public class BEASTi implements BEASTInterface {
		final public Input<String> msgInput = new Input<>("value", "message for this BEASTi object", Validate.REQUIRED);
		final public Input<BEASTi> beastiInput = new Input<>("other", "link to another BEASTi object");
		String ID;
		Set<BEASTInterface> outputs = new HashSet<BEASTInterface>();
		Map<String, Input<?>> inputs;

		@Override
		public String getID() {
			return ID;
		}

		@Override
		public void setID(String ID) {
			this.ID = ID;
		}

		@Override
		public Set<BEASTInterface> getOutputs() {
			return outputs;
		}

		@Override
		public void initAndValidate() {
			// nothing to do;
		}

		@Override
		public Map<String, Input<?>> getInputs() {
			if (inputs == null) {
				inputs = new HashMap<>();
				inputs.put("value", msgInput);
				inputs.put("other", beastiInput);
			}
			return inputs;
		}
	}
	
	@Description("class that extends BEASTi with multiple citations")
	@Citation("this is another dummy citation")
	@Citation("and yet another dummy citation")
	public class BEASTi2 extends BEASTi {
		
	}
	
	@Test
	public void testBEASTi() throws Exception {
		BEASTi beasti = new BEASTi();
		
		System.err.println("test getCitation");
		Citation citation = beasti.getCitation();
		assertEquals("this is a dummy citation", citation.value());

		citation = beasti.getCitationList().get(0);
		assertEquals("this is a dummy citation", citation.value());
		
		BEASTi beasti02 = new BEASTi2();
		List<Citation> citations = beasti02.getCitationList();
		assertEquals(3, citations.size());


		System.err.println("test initByName");
		beasti.initByName("value", "hello world");
		Input<?> input = beasti.getInput("value");
		assertEquals("hello world", input.get());
		
		System.err.println("test listInputs");
		List<?> list = beasti.listInputs();
		assertEquals(2, list.size());

		System.err.println("test initByName");
		BEASTi beasti2 = new BEASTi();
		beasti.initByName("value", "hello world",
				"other", beasti2);
		
		System.err.println("test getInputValue");
		BEASTi beasti3 = (BEASTi) beasti.getInputValue("other");
		assertEquals(beasti2, beasti3);
		
		System.err.println("test getOutputs");
		assertEquals(1, beasti2.getOutputs().size());
		
		String description = beasti.getDescription();
		assertEquals("class that impements BEASTInterface but is not a BEASTObject", description);
		
		List<BEASTInterface> predecessors = new ArrayList<BEASTInterface>();
		beasti2.getPredecessors(predecessors);
		assertEquals(1, predecessors.size());
		beasti.getPredecessors(predecessors);
		assertEquals(2, predecessors.size());
		assertEquals(beasti, predecessors.get(1));
		assertEquals(beasti2, predecessors.get(0));
		
		description = beasti.getTipText("other");
		assertEquals("link to another BEASTi object", description);
		
		boolean b = beasti.isPrimitive("value");
		assertEquals(true, b);
		b = beasti.isPrimitive("other");
		assertEquals(false, b);
		
		List<BEASTInterface> beastObjbects = beasti.listActiveBEASTObjects();
		assertEquals(1, beastObjbects.size());
		assertEquals(beasti2, beastObjbects.get(0));
		beastObjbects = beasti2.listActiveBEASTObjects();
		assertEquals(0, beastObjbects.size());
		
		
		beasti.validateInputs();

		try {
			beasti2.validateInputs();
			assertEquals(true, false); // should never get here
		} catch (Throwable t) {
			// lucky to be here
		}
		
		
		beasti2.setInputValue("value", "Goodbye!");
		String msg = (String) beasti2.getInputValue("value");
		assertEquals("Goodbye!", msg);
	
		
		
	}
	
	@Test
	public void testInputType() {
		BEASTi o = new BEASTi();
		// no input type set yet
		assertEquals(o.msgInput.getType(), null);
		assertEquals(o.beastiInput.getType(), null);
		o.determindClassOfInputs();
		// all input types are set
		assertEquals(o.msgInput.getType(), String.class);
		assertEquals(o.beastiInput.getType(), BEASTi.class);

		o = new BEASTi();
		// set input type of msgInput but not beastiInput
		o.setInputValue("value", "Hello world");
		assertEquals(o.msgInput.getType(), String.class);
		assertEquals(o.beastiInput.getType(), null);
		o.determindClassOfInputs();		
		// all input types are set
		assertEquals(o.msgInput.getType(), String.class);
		assertEquals(o.beastiInput.getType(), BEASTi.class);

		// should not crash because input type is set
		o.msgInput.set("Bye");
		
		o = new BEASTi();
		try {
			// should crash because input type is not set yet
			o.msgInput.set("Bye"); 
			throw new RuntimeException("Should not get here");
		} catch (IllegalArgumentException e) {
			// ok
			System.err.println("OK");
		}
		
		
	}
	
	
}
