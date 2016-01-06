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

    /**
     * Check all plug-ins have a proper description so that
     * everything is at least moderately well documented. *
     */
    @Test
    public void testDescriptions() {
        final List<String> sPluginNames = AddOnManager.find(beast.core.BEASTObject.class, AddOnManager.IMPLEMENTATION_DIR);
        final List<String> sUndocumentedPlugins = new ArrayList<String>();
        for (final String beastObjectName : sPluginNames) {
            try {
                final Class<?> pluginClass = Class.forName(beastObjectName);
                final Annotation[] classAnnotations = pluginClass.getAnnotations();
                boolean hasSatisfactoryDescription = false;
                for (final Annotation annotation : classAnnotations) {
                    if (annotation instanceof Description) {
                        final Description description = (Description) annotation;
                        final String sDescription = description.value();
                        if (isProperDocString(sDescription)) {
                            hasSatisfactoryDescription = true;
                        }
                    }
                }
                if (!hasSatisfactoryDescription) {
                    sUndocumentedPlugins.add(beastObjectName);
                }
            } catch (Exception e) {
            }
        }
        assertTrue("No proper description for: " + sUndocumentedPlugins.toString(), sUndocumentedPlugins.size() == 0);
    } // testDescriptions

    /**
     * Check all inputs of plug-ins have a proper tip text, again
     * to facilitate proper documentation. *
     */
    @Test
    public void testInputTipText() {
        final List<String> sPluginNames = AddOnManager.find(beast.core.BEASTObject.class, AddOnManager.IMPLEMENTATION_DIR);
        final List<String> sUndocumentedInputs = new ArrayList<String>();
        for (final String beastObjectName : sPluginNames) {
            try {
                final BEASTObject beastObject = (BEASTObject) Class.forName(beastObjectName).newInstance();
                final List<Input<?>> inputs = beastObject.listInputs();
                for (final Input<?> input : inputs) {
                    boolean hasSatisfactoryDescription = false;
                    final String sTipText = input.getTipText();
                    if (isProperDocString(sTipText)) {
                        hasSatisfactoryDescription = true;
                    }
                    if (!hasSatisfactoryDescription) {
                        sUndocumentedInputs.add(beastObjectName + ":" + input.getName());
                    }
                }
            } catch (Exception e) {
            }
        }

        assertTrue("No proper input tip text (at least " + N_WORDS + " words and " + N_CHARS + " characters) for: "
                + sUndocumentedInputs.toString(), sUndocumentedInputs.size() == 0);
    } // testInputTipText


    /**
     * run DocMaker. This can pick up incorrectly initialised inputs of lists
     * and some other initialisation stuff *
     */
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
    boolean isProperDocString(final String sStr) {
        // check length
        if (sStr.length() < N_CHARS) {
            return false;
        }
        // count nr of words
        final String[] sWords = sStr.split("\\s+");
        if (sWords.length < N_WORDS) {
            return false;
        }
        return true;
    } // isProperDocString

} // class DocumentationTest
