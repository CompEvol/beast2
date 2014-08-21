package test.beast.util;

import beast.util.TreeParser;
import junit.framework.TestCase;
import org.junit.Test;


public class TreeParserTest extends TestCase {

    @Test
    public void testFullyLabelledWithIntegers() {

        String newick = "((0:1.0,1:1.0)4:1.0,(2:1.0,3:1.0)5:1.0)6:0.0;";

        try {

            boolean isLabeled = false;

            TreeParser treeParser = new TreeParser(newick, false, false, isLabeled, 0);
            treeParser.offsetInput.setValue(0, treeParser);

            assertEquals(newick.split(";")[0], treeParser.getRoot().toShortNewick(true));

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.  \
            assertTrue("Exception!", false);
        }


    }

    @Test
    public void testOnlyLeafLabels() throws Exception {

        String newick = "((A:1.0,B:1.0):1.0,(C:1.0,D:1.0):1.0):0.0;";

//        try {

            boolean isLabeled = false;

            TreeParser treeParser = new TreeParser(newick, false, false, isLabeled, 1);
        System.out.println("adfgad");
            assertEquals(newick.split(";")[0], treeParser.getRoot().toNewick());

//        } catch (Exception e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.  \
//            assertTrue("Exception!", false);
//        }


    }

    @Test
    public void testSomeInternalNodesLabelled() {

        String newick = "((A:1.0,B:1.0)E:1.0,(C:1.0,D:1.0):1.0):0.0;";

        try {

            boolean isLabeled = true;

            TreeParser treeParser = new TreeParser(newick, false, false, isLabeled, 1);

            assertEquals(newick.split(";")[0], treeParser.getRoot().toNewick());

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.  \
            assertTrue("Exception!", false);
        }


    }


}
