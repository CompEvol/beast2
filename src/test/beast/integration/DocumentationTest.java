package test.beast.integration;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import org.junit.Test;

import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;
import beast.util.ClassDiscovery;

public class DocumentationTest extends TestCase {

	/** Check all plug-ins have a proper description so that
	 * everything is at least moderately well documented. **/
	@Test
	public void testDescriptions() {
		List<String> sPluginNames = ClassDiscovery.find(beast.core.Plugin.class, ClassDiscovery.IMPLEMENTATION_DIR);
		List<String> sUndocumentedPlugins = new ArrayList<String>();
		for (String sPlugin : sPluginNames) {
			try {
        		Class<?> pluginClass = Class.forName(sPlugin);
                Annotation[] classAnnotations = pluginClass.getAnnotations();
                boolean hasSatisfactoryDescription = false;
                for (Annotation annotation : classAnnotations) {
                	if (annotation instanceof Description) {
                		Description description = (Description) annotation;
                		String sDescription = description.value();
                		if (isProperDocString(sDescription)) {
                			hasSatisfactoryDescription = true;
                		}
                	}
                }
                if (!hasSatisfactoryDescription) {
                	sUndocumentedPlugins.add(sPlugin);
                }
        	} catch (Exception e) {
			}
		}
		assertTrue("No proper description for: " + sUndocumentedPlugins.toString(), sUndocumentedPlugins.size() == 0);
	} // testDescriptions

	/** Check all inputs of plug-ins have a proper tip text, again
	 * to facilitate proper documentation. **/
	@Test
	public void testInputTipText() {
		List<String> sPluginNames = ClassDiscovery.find(beast.core.Plugin.class, ClassDiscovery.IMPLEMENTATION_DIR);
		List<String> sUndocumentedInputs = new ArrayList<String>();
		for (String sPlugin : sPluginNames) {
			try {
        		Plugin plugin = (Plugin) Class.forName(sPlugin).newInstance();
        		List<Input<?>> inputs = plugin.listInputs();
                for (Input<?> input: inputs) {
           			boolean hasSatisfactoryDescription = false;
               		String sTipText = input.getTipText();
               		if (isProperDocString(sTipText)) {
               			hasSatisfactoryDescription = true;
                	}
                    if (!hasSatisfactoryDescription) {
                    	sUndocumentedInputs.add(sPlugin + ":" + input.getName());
                    }
                }
        	} catch (Exception e) {
			}
		}
	
		assertTrue("No proper input tip text for: " + sUndocumentedInputs.toString(), sUndocumentedInputs.size() == 0);
	} // testInputTipText

	
	// description of at least 15 chars and at least 4 words is satisfactory?!?
	// TODO: needs a bit more smarts to prevent as df a hsf jasd;fajasdf
	boolean isProperDocString(String sStr) {
		// check length
		if (sStr.length() < 15) {
			return false;
		}
		// count nr of words
		String [] sWords = sStr.split("\\s+");
		if (sWords.length < 4) {
			return false;
		}
		return true;
	} // isProperDocString
	
} // class DocumentationTest
