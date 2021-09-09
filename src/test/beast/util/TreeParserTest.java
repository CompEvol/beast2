package test.beast.util;

import org.junit.Assert;
import org.junit.Test;

import beast.evolution.tree.TreeParser;

public class TreeParserTest {

    @Test
    public void testFullyLabelledWithIntegers() {

        String newick = "((0:1.0,1:1.0)4:1.0,(2:1.0,3:1.0)5:1.0)6:0.0;";

        try {

            boolean isLabeled = false;

            TreeParser treeParser = new TreeParser(newick, false, false, isLabeled, 0);
            treeParser.offsetInput.setValue(0, treeParser);

            Assert.assertEquals(newick.split(";")[0], treeParser.getRoot().toShortNewick(true));

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.  \
            Assert.assertTrue("Exception!", false);
        }

    }

    @Test
    public void testOnlyLeafLabels() throws Exception {

        String newick = "((A:1.0,B:1.0):1.0,(C:1.0,D:1.0):1.0):0.0;";

        boolean isLabeled = true;

        TreeParser treeParser = new TreeParser(newick, false, false, isLabeled, 1);
        System.out.println("adfgad");
        Assert.assertEquals(newick.split(";")[0], treeParser.getRoot().toNewick());

    }

    @Test
    public void testOnlyLeafLabels2() throws Exception {

        String newick = "((D:5.0,C:4.0):6.0,(A:1.0,B:2.0):3.0):0.0;";

        TreeParser treeParser = new TreeParser();
        treeParser.initByName("IsLabelledNewick", true, 
        		"newick", newick,
        		"adjustTipHeights", false);
        
        String newick2 = treeParser.getRoot().toNewick();
        
        Assert.assertEquals(newick.replaceAll(";", ""), newick2);
    }

    @Test
    public void testSomeInternalNodesLabelled() {

        String newick = "((A:1.0,B:1.0)E:1.0,(C:1.0,D:1.0):1.0):0.0;";

        try {

            boolean isLabeled = true;

            TreeParser treeParser = new TreeParser(newick, false, false, isLabeled, 1);

            Assert.assertEquals(newick.split(";")[0], treeParser.getRoot().toNewick());

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.  \
            Assert.assertTrue("Exception!", false);
        }


    }


    @Test
    public void testDuplicates() throws Exception {

        String newick = "((A:1.0,B:1.0):1.0,(C:1.0,A:1.0):1.0):0.0;";

        boolean exceptionRaised = false;

        try {
            boolean isLabeled = true;
            TreeParser treeParser = new TreeParser(newick, false, false, isLabeled, 1);
            System.out.println(treeParser.getRoot().toNewick());
        } catch (RuntimeException e) {
            e.printStackTrace();
            exceptionRaised = true;
        }

        Assert.assertTrue(exceptionRaised);
    }

    @Test
    public void testBinarization() throws Exception {

        String newick = "((A:1.0,B:1.0,C:1.0):1.0,(D:1.0,E:1.0,F:1.0,G:1.0):1.0):0.0;";
        String binaryNewick = "((A:1.0,(B:1.0,C:1.0):0.0):1.0,(D:1.0,(E:1.0,(F:1.0,G:1.0):0.0):0.0):1.0):0.0;";

        boolean isLabeled = true;

        TreeParser treeParser = new TreeParser(newick, false, false, isLabeled, 1);
        Assert.assertEquals(binaryNewick.split(";")[0], treeParser.getRoot().toNewick());

    }

    @Test
    public void testMultifurcations() throws Exception {

        String newick = "((A:1.0,B:1.0,C:1.0):1.0,(D:1.0,E:1.0,F:1.0,G:1.0):1.0):0.0;";

        boolean isLabeled = true;

        TreeParser treeParser = new TreeParser(newick, false, false, isLabeled, 1, false);
        Assert.assertEquals(newick.split(";")[0], treeParser.getRoot().toNewick());

    }

    @Test
    public void testMultipleMetadata() throws Exception {

        String newick = "((A:1.0,B[&key=2.0,rate=3,type='OK']:1.0):1.0,(C:1.0,D:1.0):1.0):0.0;";

        boolean isLabeled = true;

        TreeParser treeParser = new TreeParser(newick, false, false, isLabeled, 1);
        Assert.assertTrue(treeParser.getNode(1).getMetaData("key").equals(2.0));
        Assert.assertTrue(treeParser.getNode(1).getMetaData("rate").equals(3.0));
        Assert.assertTrue(treeParser.getNode(1).getMetaData("type").equals("OK"));
    }

    @Test
    public void testVectorMetadata() throws Exception {

        String newick = "((A:1.0,B[&key={1,2,3}]:1.0):1.0,(C:1.0,D:1.0):1.0):0.0;";

        boolean isLabeled = true;

        TreeParser treeParser = new TreeParser(newick, false, false, isLabeled, 1);
        Assert.assertTrue((treeParser.getNode(1).getMetaData("key") instanceof Double[])
                && ((Double[])(treeParser.getNode(1).getMetaData("key"))).length == 3);

    }

    @Test
    public void testNodeLengthMetadata() throws Exception {

        String newick = "((A:1.0,B[&key=42]:[&key=2.5]1.0):1.0,(C:1.0,D:1.0):1.0):0.0;";

        boolean isLabeled = true;

        TreeParser treeParser = new TreeParser(newick, false, false, isLabeled, 1);
        Assert.assertTrue(treeParser.getNode(1).getLengthMetaData("key").equals(2.5));
        Assert.assertTrue(treeParser.getNode(1).getMetaData("key").equals(42.0));
    }

    @Test
    public void testInternalNodeLabels() throws Exception {

        String newick = "((xmr),((knw)ctm));";

        boolean isLabeled = true;

        TreeParser treeParser = new TreeParser(newick,false, true, isLabeled, 0, false);

        Assert.assertTrue(treeParser.getNode(0).getID().equals("knw"));
        Assert.assertTrue(treeParser.getNode(1).getID().equals("xmr"));
        Assert.assertTrue(treeParser.getNode(0).getParent().getID().equals("ctm"));
        Assert.assertTrue(treeParser.getNode(1).getParent().getID() == null);
    }
}
