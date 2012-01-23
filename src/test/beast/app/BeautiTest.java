package test.beast.app;

import java.io.File;

import org.junit.Test;

import beast.app.beauti.Beauti;
import beast.app.beauti.BeautiDoc;

import junit.framework.TestCase;

public class BeautiTest extends TestCase {
    String sFile = "tmp123x666.xml";

    @Test
    // test that beauti can merge an alignment with a template and write out a file
    // this requires that the standard template can be read
    public void testStandarBatchMode() {
        BeautiDoc doc = new BeautiDoc();
        try {
            doc.processTemplate("templates/Standard.xml");
        } catch (Exception e) {
            assertEquals(true, false);
        }

        // ignore test if no X11 display available
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            File f = new File(sFile);
            if (f.exists()) {
                f.delete();
            }
            Beauti.main(("-template templates/Standard.xml -nex examples/nexus/dna.nex -out " + sFile + " -exitaction writexml").split(" "));
            f = new File(sFile);
            assertEquals(f.exists() && f.length() > 0, true);
        }
    }

    @Test
    // as testStandarBatchMode() but for the *Beast template
    public void testStarBeastBatchMode() {
        BeautiDoc doc = new BeautiDoc();
        try {
            doc.processTemplate("templates/StarBeast.xml");
        } catch (Exception e) {
            assertEquals(true, false);
        }
        // ignore test if no X11 display available
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            File f = new File(sFile);
            if (f.exists()) {
                f.delete();
            }
            Beauti.main(("-template templates/StarBeast.xml -nex examples/nexus/26.nex -nex examples/nexus/29.nex -out " + sFile + " -exitaction writexml").split(" "));
            f = new File(sFile);
            assertEquals(f.exists() && f.length() > 0, true);
        }
    }
}
