package test.beast.evolution.alignment;


import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import beast.app.seqgen.SimulatedAlignment;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Taxon;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.substitutionmodel.HKY;
import beast.evolution.tree.RandomTree;
import beast.evolution.tree.TraitSet;
import beast.evolution.tree.Tree;
import beast.evolution.tree.coalescent.ConstantPopulation;
import beast.inference.parameter.RealParameter;
import junit.framework.TestCase;

public class UnorderedAlignmentsTest extends TestCase {

    static public TaxonSet getTaxa() throws Exception {
        return new TaxonSet(IntStream.range(65, 81).mapToObj(i -> ((Character) (char) i).toString()).map((java.util.function.Function<String, Taxon>) (id) -> {
            try {
                return new Taxon(id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()));
    }

    static public TraitSet getDates(TaxonSet taxa) throws Exception {
        TraitSet timeTrait = new TraitSet();
        String trait = String.join(",", (Iterable<String>) IntStream.range(0, 16).mapToObj(i -> taxa.getTaxonId(i) + "=" + i / 3.0)::iterator);
        timeTrait.initByName(
                "traitname", "date-forward",
                "taxa", taxa,
                "value", trait);
        return timeTrait;
    }

    static public Tree getTree(TaxonSet taxa) throws Exception {
        Tree tree = new RandomTree();
        TraitSet dates = getDates(taxa);
        ConstantPopulation constant = new ConstantPopulation();
        constant.initByName("popSize", new RealParameter("5.0"));
        tree.initByName(
                "taxonset", taxa,
                "populationModel", constant,
                "trait", dates);
        return tree;
    }

    static public SiteModel getSiteModel() throws Exception {
        Frequencies frequencies = new Frequencies();
        frequencies.initByName("frequencies", new RealParameter("0.25 0.25 0.25 0.25"));
        HKY hky = new HKY();
        hky.initByName("kappa", new RealParameter("1.0"), "frequencies", frequencies);
        SiteModel siteModel = new SiteModel();
        siteModel.initByName("mutationRate", new RealParameter("0.005"), "substModel", hky);
        return siteModel;
    }

    static public Alignment getAlignment(TaxonSet taxa, Tree tree, SiteModel siteModel) throws Exception {
        Alignment dummy = new Alignment();
        Object [] args = new String[2 * taxa.getTaxonCount() + 2];
        args[args.length - 2] = "dataType";
        args[args.length - 1] = "nucleotide";
        for (int i = 0; i < taxa.getTaxonCount(); ++i) {
            args[2*i] = taxa.getTaxonId(i);
            args[2*i+1] = "N";
        }
        dummy.initByName(args);
        SimulatedAlignment data = new SimulatedAlignment();
        data.initByName("data", dummy, "tree", tree, "siteModel", siteModel);
        return data;
    }

    @Test
    public void testUnorderedAlignment() throws Exception {
        TaxonSet taxa = getTaxa();
        Tree tree = getTree(taxa);
        SiteModel siteModel = getSiteModel();
        double logP = 0.0;
        double shuffledLogP = 0.0;
        for (int i = 0; i < 3; ++i) {
            Alignment data = getAlignment(taxa, tree, siteModel);

            // First calculate in order
            TreeLikelihood likelihood = new TreeLikelihood();
            likelihood.initByName("data", data, "tree", tree, "siteModel", siteModel);
            logP += likelihood.calculateLogP();

            // Now calculate again, with shuffled taxon order
            Collections.shuffle(data.sequenceInput.get());
            likelihood = new TreeLikelihood();
            likelihood.initByName("data", data, "tree", tree, "siteModel", siteModel);
            shuffledLogP += likelihood.calculateLogP();
        }
        assertEquals(logP, shuffledLogP, 1E-9);
    }

}
