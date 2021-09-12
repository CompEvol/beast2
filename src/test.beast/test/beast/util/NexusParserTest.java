package test.beast.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import beast.base.parser.NexusParser;
import junit.framework.TestCase;

public class NexusParserTest extends TestCase {

    @Test
    public void testThatNexusExamplesParse() {
        try {
            String dirName = System.getProperty("user.dir") + "/examples/nexus";
            System.out.println("Test Nexus Examples in " + dirName);
            File exampleDir = new File(dirName);
            String[] exampleFiles = exampleDir.list(new FilenameFilter() {
                @Override
				public boolean accept(File dir, String name) {
                    return name.endsWith(".nex") || name.endsWith(".nxs") ;
                }
            });

            List<String> failedFiles = new ArrayList<>();
            for (String fileName : exampleFiles) {
                System.out.println("Processing " + fileName);
                NexusParser parser = new NexusParser();
                try {
                    parser.parseFile(new File(dirName + "/" + fileName));
                } catch (Exception e) {
                    System.out.println("ExampleNexusParsing::Failed for " + fileName
                            + ": " + e.getMessage());
                    failedFiles.add(fileName);
                }
                System.out.println("Done " + fileName);
            }
            if (failedFiles.size() > 0) {
                System.out.println("\ntest_ThatNexusExamplesParse::Failed for : " + failedFiles.toString());
            } else {
                System.out.println("\ntest_ThatNexusExamplesParse::Success");
            }
            assertTrue(failedFiles.toString(), failedFiles.size() == 0);
        } catch (Exception e) {
            System.out.println("exception thrown ");
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testTranslateBlock() {

        String nexusTreeWithTranslateBlock  = "#NEXUS\n" +
                "\n" +
                "Begin trees;\n" +
                "\tTranslate\n" +
                "\t\t1 ID1,\n" +
                "\t\t2 ID0,\n" +
                "\t\t3 ID4,\n" +
                "\t\t4 ID2,\n" +
                "\t\t5 ID3,\n" +
                "\t\t6 ID5,\n" +
                "\t\t7 ID6\n" +
                "\t\t;\n" +
                "tree TREE1  = [&R] (((1[&height_95%_HPD={0.0,2.220446049250313E-16},height_median=0.0,height=3.885354014841496E-17,height_range={0.0,8.881784197001252E-16}]:0.3500782084890231,2[&height_95%_HPD={0.0,2.220446049250313E-16},height_median=0.0,height=3.885336674542856E-17,height_range={0.0,8.881784197001252E-16}]:0.3500782084890231)[&height_95%_HPD={0.07176417767165222,0.32897302210795143},height_median=0.1721780016900536,height=0.18167779468916231,posterior=0.9992003198720512,height_range={0.019500497296338903,0.472878180264112}]:0.9248218821607495,(3[&height_95%_HPD={0.0,2.220446049250313E-16},height_median=0.0,height=3.9830145767837334E-17,height_range={0.0,4.440892098500626E-16}]:0.9953937953591572,(4[&height_95%_HPD={0.0,2.220446049250313E-16},height_median=0.0,height=4.332577657073782E-17,height_range={0.0,8.881784197001252E-16}]:0.2885313407858645,5[&height_95%_HPD={0.0,2.220446049250313E-16},height_median=0.0,height=4.332594997372422E-17,height_range={0.0,8.881784197001252E-16}]:0.2885313407858645)[&height_95%_HPD={0.03428997737185302,0.29384644242980307},height_median=0.13917660355071027,height=0.15063132242919447,posterior=0.9992003198720512,height_range={0.012135234424637736,0.43470259067266337}]:0.7068624545732927)[&height_95%_HPD={0.4703860181701143,1.2401041356897795},height_median=0.8841915264598103,height=0.8799641568544527,posterior=0.9668132746901239,height_range={0.2376833426394811,1.5008735971013971}]:0.2795062952906153)[&height_95%_HPD={0.871170115451148,1.736574411464855},height_median=1.3388341576137588,height=1.3419155689487607,posterior=0.8912435025989605,height_range={0.609339147487161,2.0082114277758554}]:0.1115505866359714,(6[&height_95%_HPD={0.0,2.220446049250313E-16},height_median=0.0,height=3.805449918706939E-17,height_range={0.0,4.440892098500626E-16}]:0.1143648180955704,7[&height_95%_HPD={0.0,2.220446049250313E-16},height_median=0.0,height=3.805449918706939E-17,height_range={0.0,4.440892098500626E-16}]:0.1143648180955704)[&height_95%_HPD={0.07189918035503462,0.3145415291508802},height_median=0.1807116511737813,height=0.18641149700061227,posterior=0.9996001599360256,height_range={0.0042543872372436675,0.4254547656747345}]:1.2720858591901736)[&height_95%_HPD={1.2498874674331548,2.2880779508436415},height_median=1.7581890486365315,height=1.7391129102773757,posterior=1.0,height_range={0.003684245207410762,2.42575160443681}];\n" +
                "End;\n";


        NexusParser parser = new NexusParser();
        try {

            Set<String> taxa = new TreeSet<>();
            taxa.add("ID0");
            taxa.add("ID1");
            taxa.add("ID2");
            taxa.add("ID3");
            taxa.add("ID4");
            taxa.add("ID5");
            taxa.add("ID6");

            parser.parseFile("testTranslateBlock", new StringReader(nexusTreeWithTranslateBlock));

            assertEquals(1, parser.trees.size());

            assertNotNull(parser.trees.get(0));

            assertEquals(7, parser.trees.get(0).getTaxaNames().length);

            for (String taxaName : parser.trees.get(0).getTaxaNames()) {
                assertNotNull(taxaName);
            }

            assertTrue(taxa.containsAll(Arrays.asList(parser.trees.get(0).getTaxaNames())));

        } catch (Exception e) {
            e.printStackTrace();

            assertTrue(false);
        }
    }


    
    @Test
    public void testTranslateBlock2() {

        String nexusTreeWithTranslateBlock  = "#NEXUS\n" +
                "\n" +
                "Begin trees;\n" +
                "\tTranslate\n" +
                "\t\t1 C,\n" +
                "\t\t2 B,\n" +
                "\t\t3 A\n" +
                "\t\t;\n" +
                "tree TREE1  = [&R] (1:10,(3:30,2:20):10);\n" +
                "End;\n";


        NexusParser parser = new NexusParser();
        try {

            List<String> taxa = new ArrayList<>();
            taxa.add("2");
            taxa.add("0");
            taxa.add("1");

            parser.parseFile("testTranslateBlock", new StringReader(nexusTreeWithTranslateBlock));

            assertEquals(1, parser.trees.size());

            assertNotNull(parser.trees.get(0));
            
            String t = parser.trees.get(0).getRoot().toNewick();
            System.out.println(t);
            assertEquals("(C:10.0,(B:20.0,A:30.0):10.0):0.0", t);
            


        } catch (Exception e) {
            e.printStackTrace();

            assertTrue(false);
        }
    }
    
    
    @Test
    public void testAssumptionsParse() {
        try {
            String fileName = System.getProperty("user.dir") + "/examples/nexus/Primates.nex";
            NexusParser parser = new NexusParser();
            parser.parseFile(new File(fileName));
            assertEquals(2, parser.filteredAlignments.size());
        } catch (Exception e) {
            System.out.println("exception thrown ");
            System.out.println(e.getMessage());
        }
    }
}
