package test.beast.util;


import java.util.ArrayList;
import java.util.List;

import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.sitemodel.SiteModel;
import beast.base.evolution.substitutionmodel.JukesCantor;
import beast.base.evolution.tree.ClusterTree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import test.beast.BEASTTestCase;


public class ClusterTreeTest  {

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
        
        String expectedNewick = "((((human:0.01903085702575253,(chimp:0.008560512208575313,bonobo:0.008560512208575313):0.010470344817177218):0.007962255880644985,gorilla:0.026993112906397516):0.019197419394211015,orangutan:0.04619053230060853):0.007214240662738673,siamang:0.053404772963347204):0.0";
        String actualNewick = tree.getRoot().toNewick();
        assertEquals(expectedNewick, actualNewick);
        
        // select 3 sequences
        List<Sequence> seqs = new ArrayList<Sequence>();
        seqs.addAll(alignment.sequenceInput.get());
        List<Sequence> newseqs = alignment.sequenceInput.get();
        newseqs.clear();
        newseqs.add(Sequence.getSequenceByTaxon("bonobo", seqs));
        newseqs.add(Sequence.getSequenceByTaxon("chimp", seqs));
        newseqs.add(Sequence.getSequenceByTaxon("human", seqs));
        alignment.initAndValidate();
        tree = new ClusterTree();
        tree.initByName(
                "clusterType", "upgma",
                "taxa", alignment);
        expectedNewick = "((bonobo:0.008560512208575313,chimp:0.008560512208575313):0.010470344817177218,human:0.01903085702575253):0.0";
        
        System.err.println("Seqs:");
        for (Sequence s : seqs) {
        	System.err.println(s.taxonInput.get());
        }
        System.err.println("Newseqs:");
        for (Sequence s : newseqs) {
        	System.err.println(s.taxonInput.get());
        }
        
        actualNewick = tree.getRoot().toNewick();
        assertEquals(expectedNewick, actualNewick);

        // same sequences in different order
        newseqs.clear();
        newseqs.add(Sequence.getSequenceByTaxon("human", seqs));
        newseqs.add(Sequence.getSequenceByTaxon("chimp", seqs));
        newseqs.add(Sequence.getSequenceByTaxon("bonobo", seqs));
        alignment.initAndValidate();
        tree = new ClusterTree();
        tree.initByName(
                "clusterType", "upgma",
                "taxa", alignment);
        actualNewick = tree.getRoot().toNewick();
        expectedNewick = "(human:0.01903085702575253,(chimp:0.008560512208575313,bonobo:0.008560512208575313):0.010470344817177218):0.0";
        assertEquals(expectedNewick, actualNewick);
    }
}
