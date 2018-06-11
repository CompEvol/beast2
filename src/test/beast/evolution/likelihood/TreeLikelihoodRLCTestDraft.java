package test.beast.evolution.likelihood;


import beast.core.parameter.BooleanParameter;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.branchratemodel.RandomLocalClockModel;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.substitutionmodel.HKY;
import beast.evolution.tree.Node;
import beast.util.Randomizer;
import beast.util.TreeParser;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.List;

/**
 * Sharp spikes in posterior trace random local clock
 * https://github.com/CompEvol/beast2/issues/785
 */
public class TreeLikelihoodRLCTestDraft extends TestCase {

    public TreeLikelihoodRLCTestDraft() {
        super();
    }

    protected TreeLikelihood javaTreeLikelihood() {
        System.setProperty("java.only","true");
        return new TreeLikelihood();
    }

    protected TreeLikelihood beagleTreeLikelihood() {
        System.setProperty("java.only","false");
        return new TreeLikelihood();
    }

    /**
     *
     */
    @Test
    public void testBeagleRLCLikelihood() throws Exception {

        Sequence seq1 = new Sequence("taxon1", "GTCGGTCAGTCA");
        Sequence seq2 = new Sequence("taxon2", "TCAGTTAGTCAG");
        Sequence seq3 = new Sequence("taxon3", "CAGTCAGTCAGT");

        Alignment alignment = new Alignment();
        alignment.initByName("sequence", seq1,
                "sequence", seq2,
                "sequence", seq3,
                "dataType", "nucleotide");

        // The start point is 6780000 from Tim's XML using seed 5010
        TreeParser tree = new TreeParser();
        // ((2:1.0812703780105475,1:0.08127037801054748)3:0.957396117780122,0:0.0386664957906695)4:0.0
        String treeSting = "((taxon3:1.0812703780105475,taxon2:0.08127037801054748)3:0.957396117780122,taxon1:0.0386664957906695)4:0.0";
        tree.initByName("taxa", alignment, "newick", treeSting, "IsLabelledNewick", true);

        // subst model
        RealParameter f = new RealParameter(new Double[]{0.25002332020722356,0.249757221213698,0.24989440866005042,0.250325049919028});
        Frequencies freqs = new Frequencies();
        freqs.initByName("frequencies", f, "estimate", false);

        HKY hky = new HKY();
        hky.initByName("kappa", "0.8582028379159838", "frequencies", freqs);

        SiteModel siteModel = new SiteModel();
        siteModel.initByName("gammaCategoryCount", 1, "substModel", hky);

        // RLC
        BooleanParameter indicators = new BooleanParameter(new Boolean[]{false, false, false, false});
        RealParameter clockRates = new RealParameter(new Double[]{3.9950381793702756E-8,2.0619003298766432E-5,3.3259368905282205E-18,1.006998588099822E-12});
        RandomLocalClockModel rLC = new RandomLocalClockModel();
        rLC.initByName("clock.rate", "2.435178917243108E-6", "indicators", indicators,
                "rates", clockRates, "tree", tree);

        Randomizer.setSeed(5010);

        int diff = 0;
        double scaleFactor = 0.5;
        for (int i=0; i<10; i++) {
            System.out.println("sample = " + i);

            TreeLikelihood likelihoodJava = javaTreeLikelihood();
            likelihoodJava.initByName("data", alignment, "tree", tree, "siteModel", siteModel, "branchRateModel", rLC);
            double logLJava = likelihoodJava.calculateLogP();

            System.out.println("Java Tree Likelihood = " + logLJava);

            TreeLikelihood likelihoodBeagle = beagleTreeLikelihood();
            likelihoodBeagle.initByName("data", alignment, "tree", tree, "siteModel", siteModel, "branchRateModel", rLC);
            double logLBeagle = likelihoodBeagle.calculateLogP();

            System.out.println("Beagle Tree Likelihood = " + logLBeagle);

            if ((logLJava - logLBeagle) != 0) diff++;

            System.out.println("Java - Beagle = " + (logLJava - logLBeagle) + "\n");

            // all nodes
            List<Node> allNodes = tree.getRoot().getAllChildNodesAndSelf();
            for (int n=0; n<allNodes.size(); n++) {
                System.out.println(allNodes.get(n).getHeight());
            }

            double scale = scaleFactor + (Randomizer.nextDouble() * ((1.0 / scaleFactor) - scaleFactor));
            System.out.println("scale for next sample = " + scale);

            // change root height
//            for (int n=0; n<allNodes.size(); n++) {
            allNodes.get(0).setHeight(allNodes.get(0).getHeight() * scale);
            System.out.println("root height for next sample = " + allNodes.get(0).getHeight());
//            }

            System.out.println("\n");
        }

        System.out.println("Difference = " + diff + "\n");
    }


} // class TreeLikelihoodTest
