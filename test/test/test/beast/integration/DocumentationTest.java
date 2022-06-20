package test.beast.integration;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;



public class DocumentationTest  {

	
	{
		System.setProperty("beast.is.junit.testing", "true");
	}
	
    /**
     * Check all plug-ins have a proper description so that
     * everything is at least moderately well documented. *
     */
    @Test
    public void testDescriptions() {
        final Set<String> pluginNames = PackageManager.listServices("beast.base.core.BEASTInterface");
        final List<String> undocumentedPlugins = new ArrayList<String>();
        for (final String beastObjectName : pluginNames) {
            try {
                final Class<?> pluginClass = BEASTClassLoader.forName(beastObjectName);
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
            } catch (Throwable e) {
            }
        }
        assertTrue(undocumentedPlugins.size() == 0, "No proper description for: " + undocumentedPlugins.toString());
    } // testDescriptions

    /**
     * Check all inputs of plug-ins have a proper tip text, again
     * to facilitate proper documentation. *
     */
    @Test
    public void testInputTipText() {
        final Set<String> pluginNames = PackageManager.listServices("beast.base.core.BEASTInterface");
        final List<String> undocumentedInputs = new ArrayList<String>();
        for (final String beastObjectName : pluginNames) {
            try {
                final BEASTObject beastObject = (BEASTObject) BEASTClassLoader.forName(beastObjectName).newInstance();
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
            } catch (Throwable e) {
            }
        }

        assertTrue(undocumentedInputs.size() == 0, "No proper input tip text (at least " + N_WORDS + " words and " + N_CHARS + " characters) for: "
                + undocumentedInputs.toString());
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
