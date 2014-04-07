package test.beast.app;

import java.io.File;
import java.io.PrintStream;

import org.junit.Test;

import test.beast.integration.ExampleXmlParsingTest;

import beast.app.beauti.Beauti;
import beast.app.beauti.BeautiDoc;



import junit.framework.TestCase;

public class BeautiTest extends TestCase {
	{
		ExampleXmlParsingTest.setUpTestDir();
	}
	
    String sFile = "test/tmp123x666.xml";
    String sTemplateFile = "test/template123x666.xml";

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


    // test that a dataset can be merged with a simple template
    String template = "<beast version='2.0'       namespace='beast.evolution.alignment:beast.core:beast.evolution.tree.coalescent:beast.core.util:beast.evolution.nuc:beast.evolution.operators:beast.evolution.sitemodel:beast.evolution.substitutionmodel:beast.evolution.likelihood'>\n" +
    		"<data id='data' dataType='nucleotide'>\n" +
    		"    <sequence taxon='human'>\n" +
    		"        AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGGAGCTTAAACCCCCTTATTTCTACTAGGACTATGAGAATCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAATACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTG-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGACCAATGGGACTTAAACCCACAAACACTTAGTTAACAGCTAAGCACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCGGAGCTTGGTAAAAAGAGGCCTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGGCCTCCATGACTTTTTCAAAAGGTATTAGAAAAACCATTTCATAACTTTGTCAAAGTTAAATTATAGGCT-AAATCCTATATATCTTA-CACTGTAAAGCTAACTTAGCATTAACCTTTTAAGTTAAAGATTAAGAGAACCAACACCTCTTTACAGTGA\n" +
    		"    </sequence>\n" +
    		"   <sequence taxon='chimp'>\n" +
    		"        AGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTCAAATCCCCTTATTTCTACTAGGACTATAAGAATCGAACTCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTAAGCACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCTCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AACCCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCGACACCTCTTTACAGTGA\n" +
    		"   </sequence>\n" +
    		"</data>\n" +
    		"    <input spec='HKY' id='hky'>\n" +
    		"        <kappa idref='hky.kappa'/>\n" +
    		"        <frequencies id='freqs' spec='Frequencies'>\n" +
    		"            <data idref='data'/>\n" +
    		"        </frequencies>\n" +
    		"    </input>\n" +
    		"    <input spec='SiteModel' id='siteModel' gammaCategoryCount='1'>\n" +
    		"        <substModel idref='hky'/>\n" +
    		"    </input>\n" +
    		"    <input spec='TreeLikelihood' id='treeLikelihood'>\n" +
    		"        <data idref='data'/>\n" +
    		"        <tree idref='tree'/>\n" +
    		"        <siteModel idref='siteModel'/>\n" +
    		"    </input>\n" +
    		"    <parameter id='hky.kappa' value='1.0' lower='0.0'/>\n" +
    		"    <tree spec='beast.evolution.tree.RandomTree' id='tree' taxa='@data'>\n" +
    		"        <populationModel spec='ConstantPopulation'>\n" +
    		"		<popSize spec='parameter.RealParameter' value='1'/>\n" +
    		"	</populationModel>\n" +
    		"    </tree>\n" +
    		"    <run spec='MCMC' id='mcmc' chainLength='10000000'>\n" +
    		"	<distribution spec='CompoundDistribution' id='posterior'>\n" +
    		"        	<distribution id='likelihood' idref='treeLikelihood'/>\n" +
    		"	</distribution>\n" +
    		"        <operator id='kappaScaler' spec='ScaleOperator' scaleFactor='0.5' weight='1' parameter='@hky.kappa'/>\n" +
    		"        <operator id='treeScaler' spec='ScaleOperator' scaleFactor='0.5' weight='1' tree='@tree'/>\n" +
    		"        <operator spec='Uniform' weight='10' tree='@tree'/>\n" +
    		"        <operator spec='SubtreeSlide' weight='5' gaussian='true' size='1.0' tree='@tree'/>\n" +
    		"        <operator id='narrow' spec='Exchange' isNarrow='true' weight='1' tree='@tree'/>\n" +
    		"        <operator id='wide' spec='Exchange' isNarrow='false' weight='1' tree='@tree'/>\n" +
    		"        <operator spec='WilsonBalding' weight='1' tree='@tree'/>\n" +
    		"        <logger logEvery='10000' fileName='test.$(seed).log'>\n" +
    		"	        <model idref='likelihood'/>\n" +
    		"            <log idref='likelihood'/>\n" +
    		"            <log idref='hky.kappa'/>\n" +
    		"            <log spec='beast.evolution.tree.TreeHeightLogger' tree='@tree'/>\n" +
    		"        </logger>\n" +
    		"        <logger logEvery='10000' fileName='test.$(seed).trees'>\n" +
    		"            <log idref='tree'/>\n" +
    		"        </logger>\n" +
    		"        <logger logEvery='10000'>\n" +
    		"	        <model idref='likelihood'/>\n" +
    		"            <log idref='likelihood'/>\n" +
    		"    	    <ESS spec='ESS' name='log' arg='@likelihood'/>\n" +
    		"            <log idref='hky.kappa'/>\n" +
    		"    	    <ESS spec='ESS' name='log' arg='@hky.kappa'/>\n" +
    		"        </logger>\n" +
    		"    </run>\n" +
    		"</beast>";
    @Test
    public void testCustomBatchMode() {
        BeautiDoc doc = new BeautiDoc();
        try {
        	PrintStream out = new PrintStream(sTemplateFile);
        	out.print(template);
        	out.close();
            doc.processTemplate(sTemplateFile);
        } catch (Exception e) {
            assertEquals(true, false);
        }
        // ignore test if no X11 display available
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            File f = new File(sFile);
            if (f.exists()) {
                f.delete();
            }
            Beauti.main(("-template " + sTemplateFile + " -nex examples/nexus/anolis.nex -out " + sFile + " -exitaction writexml").split(" "));
            f = new File(sFile);
            assertEquals(f.exists() && f.length() > 0, true);
        }
    }

}
