package test.beast.util;

import beast.evolution.alignment.Alignment;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.JukesCantor;
import beast.util.ClusterTree;

import junit.framework.TestCase;
import test.beast.BEASTTestCase;


public class ClusterTreeTest extends TestCase {

    public void testUPGMA() throws Exception {
        Alignment alignment = BEASTTestCase.getAlignment();

        JukesCantor JC = new JukesCantor();
        JC.initAndValidate();

        SiteModel siteModel = new SiteModel();
        siteModel.initByName("substModel", JC);
        
        ClusterTree tree = new ClusterTree();
        tree.initByName(
                "clusterType", "upgma",
                "taxa", alignment);
        
        String treeTrueNewick = "((((0:0.01903085702575253,(1:0.008560512208575313,2:0.008560512208575313)6:0.010470344817177218)7:0.007962255880644985,3:0.026993112906397516)8:0.019197419394211015,4:0.04619053230060853)9:0.007214240662738673,5:0.053404772963347204)10:0.0";
        assertEquals(tree.toString(), treeTrueNewick);
    }

}
