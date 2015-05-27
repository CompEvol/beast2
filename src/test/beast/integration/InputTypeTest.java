package test.beast.integration;


import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import beast.core.Input;
import beast.core.BEASTObject;
import beast.util.AddOnManager;
import junit.framework.TestCase;

public class InputTypeTest  extends TestCase {
	
	
	/* Test that the type of an input can be determined,
	 * If not, the programmer has to manually initialise the type of the input 
	 * (most easily done through a constructor of Input) */
	@Test
	public void testInputTypeCanBeSet() throws Exception {
        List<String> sPluginNames = AddOnManager.find(beast.core.BEASTObject.class, AddOnManager.IMPLEMENTATION_DIR);
        List<String> failingInputs = new ArrayList<String>();
        
        sPluginNames.clear();
        sPluginNames.add("beast.evolution.alignment.PrunedAlignment");
        for (String sPlugin : sPluginNames) {
            try {
                BEASTObject plugin = (BEASTObject) Class.forName(sPlugin).newInstance();
                List<Input<?>> inputs = plugin.listInputs();
                for (Input<?> input : inputs) {
            		try {
            			// TODO: handle case where this is an input of type List that has not been initialised
            			// because currently Input.determineClass calls System.exit()
	                	if (input.getType() == null) {
                			input.determineClass(plugin);
                			if (input.getType() == null) {
                    			failingInputs.add(sPlugin + ":" + input.getName());
                			}
	                	}
            		} catch (Exception e2) {
            			failingInputs.add(sPlugin + ":" + input.getName() + " " + e2.getMessage());
            		}
                }
            } catch (InstantiationException e) {
            	// ignore
            } catch (Exception e) {
            	// ignore
            }
        }
                
        assertTrue("Type of input could not be set for these inputs (probably requires to be set by using the appropriate constructure of Input): "
                + failingInputs.toString(), failingInputs.size() == 0);
	}

}
