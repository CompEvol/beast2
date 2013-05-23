package test.beast.util;

import beast.util.TreeParser;
import junit.framework.TestCase;
import org.junit.Test;

public class TreeParserTest extends TestCase {

    @Test
    public void testFullyLabelledWithIntegers() {

        String newick = "((0:1.0,1:1.0)4:1.0,(2:1.0,3:1.0)5:1.0)6:0.0;";

        try {
            TreeParser treeParser = new TreeParser(newick);

            assertEquals(newick.split(";")[0], treeParser.getRoot().toShortNewick(true));

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.  \
            assertTrue("Exception!", false);
        }


    }


}
