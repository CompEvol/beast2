package test.beast.integration;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.util.AddOnManager;
import junit.framework.TestCase;



public class DocumentationTest extends TestCase {

	
	{
		System.setProperty("beast.is.junit.testing", "true");
	}
	
    /**
     * Check all plug-ins have a proper description so that
     * everything is at least moderately well documented. *
     */
    @Test
    public void testDescriptions() {
        final List<String> pluginNames = AddOnManager.find(beast.core.BEASTObject.class, AddOnManager.IMPLEMENTATION_DIR);
        final List<String> undocumentedPlugins = new ArrayList<String>();
        for (final String beastObjectName : pluginNames) {
            try {
                final Class<?> pluginClass = Class.forName(beastObjectName);
                final Annotation[] classAnnotations = pluginClass.getAnnotations();
                boolean hasSatisfactoryDescription = false;
                for (final Annotation annotation : classAnnotations) {
                    if (annotation instanceof Description) {
                        final Description description = (Description) annotation;
                        final String descriptionString = description.value();
                        if (isProperDocString(descriptionString)) {
                            hasSatisfactoryDescription = true;
                        }
                    }
                }
                if (!hasSatisfactoryDescription) {
                    undocumentedPlugins.add(beastObjectName);
                }
            } catch (Exception e) {
            }
        }
        assertTrue("No proper description for: " + undocumentedPlugins.toString(), undocumentedPlugins.size() == 0);
    } // testDescriptions

    /**
     * Check all inputs of plug-ins have a proper tip text, again
     * to facilitate proper documentation. *
     */
    @Test
    public void testInputTipText() {
        final List<String> pluginNames = AddOnManager.find(beast.core.BEASTObject.class, AddOnManager.IMPLEMENTATION_DIR);
        final List<String> undocumentedInputs = new ArrayList<String>();
        for (final String beastObjectName : pluginNames) {
            try {
                final BEASTObject beastObject = (BEASTObject) Class.forName(beastObjectName).newInstance();
                final List<Input<?>> inputs = beastObject.listInputs();
                for (final Input<?> input : inputs) {
                    boolean hasSatisfactoryDescription = false;
                    final String tipText = input.getTipText();
                    if (isProperDocString(tipText)) {
                        hasSatisfactoryDescription = true;
                    }
                    if (!hasSatisfactoryDescription) {
                        undocumentedInputs.add(beastObjectName + ":" + input.getName());
                    }
                }
            } catch (Exception e) {
            }
        }

        assertTrue("No proper input tip text (at least " + N_WORDS + " words and " + N_CHARS + " characters) for: "
                + undocumentedInputs.toString(), undocumentedInputs.size() == 0);
    } // testInputTipText


    /**
     * run DocMaker. This can pick up incorrectly initialised inputs of lists
     * and some other initialisation stuff *
     */
    @Test
    public void test_DocMaker() throws Exception {
        // this code runs just fine stand alone, but not in ant. TODO: figure out why
//		String [] args = {"."};
//		DocMaker b = new DocMaker(args);
//		b.generateDocs();
//		// clean up
//		String [] files = new File(".").list();
//		for (String fileName : files) {
//			if (fileName.endsWith(".html")) {
//				new File(fileName).delete();
//			}
//		}
    } // test_DocMaker


    private final int N_WORDS = 4;
    private final int N_CHARS = 15;

    // description of at least 15 chars and at least 4 words is satisfactory?!?
    // TODO: needs a bit more smarts to prevent as df a hsf jasd;fajasdf
    boolean isProperDocString(final String str) {
        // check length
        if (str.length() < N_CHARS) {
            return false;
        }
        // count nr of words
        final String[] words = str.split("\\s+");
        if (words.length < N_WORDS) {
            return false;
        }
        return true;
    } // isProperDocString

} // class DocumentationTest
