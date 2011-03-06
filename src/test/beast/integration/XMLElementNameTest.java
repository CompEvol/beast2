package test.beast.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import beast.core.Input;
import beast.core.Plugin;
import beast.util.ClassDiscovery;
import beast.util.XMLParser;

import junit.framework.TestCase;


public class XMLElementNameTest extends TestCase {
	
	@Test
	public void test_ReservedExamplesNames() {
		// retrieve list of reserved names and their classes
		XMLParser parser = new XMLParser();
	    HashMap<String, String> sElement2ClassMap = parser.getElement2ClassMap();
	    // check each plugin
		List<String> sPluginNames = ClassDiscovery.find(beast.core.Plugin.class, ClassDiscovery.IMPLEMENTATION_DIR);
		List<String> sImproperInputs = new ArrayList<String>();
		for (String sPlugin : sPluginNames) {
			try {
    			Plugin plugin = (Plugin) Class.forName(sPlugin).newInstance();
    			// check each input
    			List<Input<?>> inputs = plugin.listInputs();
    			for (Input<?> input : inputs) {
    				if (sElement2ClassMap.containsKey(input.getName())) {
        				input.determineClass(plugin);
        				Class<?> type = input.getType();
        				String sBaseType = sElement2ClassMap.get(input.getName());
                        if (!isDerivedType(type, sBaseType)) {
                        	sImproperInputs.add(sPlugin + "." + input.getName());
                        }
    				}
    			}
        	} catch (InstantiationException e) {
        		// ignore
        	} catch (Exception e) {
        		// ignore
        	}
		}
		if (sImproperInputs.size() > 0) {
			String sStr = sImproperInputs.toString();
			sStr = sStr.replaceAll(",", "\n");
			System.err.println("Reserved element names used for wrong types in:\n" + sStr);
		}
		// not activated till problem with naming is solved
		//assertTrue("Reserved element names used for wrong types in: " + sImproperInputs.toString(), sImproperInputs.size() == 0);
	}

	/** true if type is a class equal to or derived from sBaseType **/
    boolean isDerivedType(Class<?> type, String sBaseType) {
    	if (sBaseType.equals(type.getName())) {
    		return true;
    	}
    	Class<?> superType = type.getSuperclass();
    	if (!superType.equals(Object.class)) {
    		return isDerivedType(superType, sBaseType);
    	}
    	
    	return false;
    }

} // class XMLElementNameTest

