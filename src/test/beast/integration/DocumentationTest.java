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
	
		assertTrue("No proper input tip text (at least " + N_WORDS + " words and " + N_CHARS + " characters) for: "
                + sUndocumentedInputs.toString(), sUndocumentedInputs.size() == 0);
	} // testInputTipText


	/** run DocMaker. This can pick up incorrectly initialised inputs of lists 
	 * and some other initialisation stuff **/
	@Test
	public void test_DocMaker() throws Exception {
		// this code runs just fine stand alone, but not in ant. TODO: figure out why
//		String [] sArgs = {"."};
//		DocMaker b = new DocMaker(sArgs);
//		b.generateDocs();
//		// clean up
//		String [] sFiles = new File(".").list();
//		for (String sFile : sFiles) {
//			if (sFile.endsWith(".html")) {
//				new File(sFile).delete();
//			}
//		}
	} // test_DocMaker

	
	
	private final int N_WORDS = 4;
    private final int N_CHARS = 15;

	// description of at least 15 chars and at least 4 words is satisfactory?!?
	// TODO: needs a bit more smarts to prevent as df a hsf jasd;fajasdf
	boolean isProperDocString(String sStr) {
		// check length
		if (sStr.length() < N_CHARS ) {
			return false;
		}
		// count nr of words
		String [] sWords = sStr.split("\\s+");
		if (sWords.length < N_WORDS ) {
			return false;
		}
		return true;
	} // isProperDocString
	
} // class DocumentationTest
