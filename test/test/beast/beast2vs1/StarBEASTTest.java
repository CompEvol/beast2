package test.beast.beast2vs1;


import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import test.beast.beast2vs1.trace.Expectation;

/**
 * Due to different naming, *BEAST 2 speciesCoalescent != *BEAST 1 species.coalescent
 * <p/>
 * beast 2 has:
 * speciesCoalescent	             : the total multispecies coalescent and pop sizes (sum of the next 3)
 * SpeciesTreePopSizePrior          : prior on population sizes parameters
 * tree.prior.26	                 : multispecies coalescent  per specific tree (26)
 * tree.prior.29	                 : multispecies coalescent  per specific tree (29)
 * SpeciesTreeDivergenceTimesPrior  : overall tree prior (Yule, BD etc)
 * <p/>
 * beast 1 has:
 * species.coalescent               : the total multispecies coalescent
 * species.popSizesLikelihood       : prior on population sizes parameters
 * speciation.likelihood           : overall tree prior (Yule, BD etc)
 *
 * @author Walter Xie
 */
public class StarBEASTTest extends TestFramework {

    String[] XML_FILES = new String[]{"testStarBEASTConstant.xml", "testStarBEASTLinear.xml",
            "testStarBEASTLinearConstRoot.xml"};//"testStarBEAST.xml" };

    public void testStarBEASTConstant() throws Exception {
        analyse(0);
    }

    public void testStarBEASTLinear() throws Exception {
        analyse(1);
    }

    public void testStarBEASTLinearConstRoot() throws Exception {
        analyse(2);
    }

    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp(XML_FILES);
    }

    @Override
	protected List<Expectation> giveExpectations(int index_XML) throws Exception {
        List<Expectation> expList = new ArrayList<Expectation>();

        // all values below are from BEAST 1.6.2
        switch (index_XML) {
            case 0: // testStarBEASTConstant.xml
                // BEAST 1 testStarBEASTConstant.xml
                addExpIntoList(expList, "posterior", -2211.8038, 0.3671);
                addExpIntoList(expList, "prior", 301.5082, 0.3341);
                addExpIntoList(expList, "popMean", 3.9767E-3, 5.2107E-5);
                addExpIntoList(expList, "birthRate", 210.9752, 2.63);
                addExpIntoList(expList, "hky.kappa26", 4.3779, 5.395E-2);
                addExpIntoList(expList, "hky.kappa29", 4.088, 5.169E-2);
                addExpIntoList(expList, "genetree.priors", 216.6122, 0.2504);
                addExpIntoList(expList, "SpeciesTreePopSizePrior", 60.2194, 0.1877);
                addExpIntoList(expList, "SpeciesTreeDivergenceTimesPrior", 29.5342, 6.1007E-2);
                addExpIntoList(expList, "TreeHeightSP", 1.1798E-2, 1.151E-4);
                addExpIntoList(expList, "TreeHeight26", 2.6466E-2, 6.3184E-5);
                addExpIntoList(expList, "TreeHeight29", 2.2388E-2, 4.9647E-5);
                addExpIntoList(expList, "likelihood", -2513.312, 0.1439);
                addExpIntoList(expList, "treelikelihood.26", -1266.818, 0.117);
                addExpIntoList(expList, "treelikelihood.29", -1246.494, 9.6752E-2);
                break;

            case 1: // testStarBEASTLinear.xml
                // BEAST 1 testStarBEASTLinear.xml
                addExpIntoList(expList, "posterior", -2173.5261, 0.4644);
                addExpIntoList(expList, "prior", 339.8849, 0.4886);
                addExpIntoList(expList, "popMean", 2.1388E-3, 2.4177E-5);
                addExpIntoList(expList, "birthRate", 208.8798, 2.5617);
                addExpIntoList(expList, "hky.kappa26", 4.4586, 6.9657E-2);
                addExpIntoList(expList, "hky.kappa29", 4.193, 7.2237E-2);
                addExpIntoList(expList, "genetree.priors", 212.531, 0.2891);
                addExpIntoList(expList, "SpeciesTreePopSizePrior", 102.1531, 0.2645);
                addExpIntoList(expList, "SpeciesTreeDivergenceTimesPrior", 29.5246, 7.3365E-2);
                addExpIntoList(expList, "TreeHeightSP", 1.1882E-2, 1.2088E-4);
                addExpIntoList(expList, "TreeHeight26", 2.5563E-2, 5.2973E-5);
                addExpIntoList(expList, "TreeHeight29", 2.2529E-2, 4.4067E-5);
                addExpIntoList(expList, "likelihood", -2513.411, 0.1569);
                addExpIntoList(expList, "treelikelihood.26", -1267.0724, 0.1138);
                addExpIntoList(expList, "treelikelihood.29", -1246.3386, 0.1219);
                break;

            case 2: // testStarBEASTLinearConstRoot.xml
                // BEAST 1 testStarBEASTLinearConstRoot.xml
                addExpIntoList(expList, "posterior", -2177.0138, 0.4211);
                addExpIntoList(expList, "prior", 336.7861, 0.4327);
                addExpIntoList(expList, "popMean", 2.0327E-3, 2.2285E-5);
                addExpIntoList(expList, "birthRate", 211.7118, 2.6749);
                addExpIntoList(expList, "hky.kappa26", 4.4708, 5.7879E-2);
                addExpIntoList(expList, "hky.kappa29", 4.0384, 5.5666E-2);
                addExpIntoList(expList, "genetree.priors", 212.5333, 0.2737);
                addExpIntoList(expList, "SpeciesTreePopSizePrior", 98.9183, 0.2323);
                addExpIntoList(expList, "SpeciesTreeDivergenceTimesPrior", 29.5698, 6.714E-2);
                addExpIntoList(expList, "TreeHeightSP", 1.1574E-2, 9.1978E-5);
                addExpIntoList(expList, "TreeHeight26", 2.6479E-2, 6.2548E-5);
                addExpIntoList(expList, "TreeHeight29", 2.2444E-2, 5.1901E-5);
                addExpIntoList(expList, "likelihood", -2513.7999, 0.1439);
                addExpIntoList(expList, "treelikelihood.26", -1267.2603, 0.1345);
                addExpIntoList(expList, "treelikelihood.29", -1246.5396, 0.1015);
                break;

            // case 0: // testStarBEAST.xml
            // // BEAST 1 testStarBEAST.xml
            // addExpIntoList(expList, "posterior", -1884.6966, 6.3796E-2);
            // addExpIntoList(expList, "prior", -68.0023, 2.114E-2);
            // addExpIntoList(expList, "tree.height", 6.3129E-2, 6.5853E-5);
            // addExpIntoList(expList, "mrcatime(human,chimp)", 2.0326E-2,
            // 3.5906E-5);
            // addExpIntoList(expList, "popSize", 9.7862E-2, 6.2387E-4);
            // addExpIntoList(expList, "hky.kappa", 25.8288, 0.1962);
            // addExpIntoList(expList, "hky.frequencies1", 0.3262, 5.9501E-4);
            // addExpIntoList(expList, "hky.frequencies2", 0.2569, 5.0647E-4);
            // addExpIntoList(expList, "hky.frequencies3", 0.1552, 4.4638E-4);
            // addExpIntoList(expList, "hky.frequencies4", 0.2617, 5.1085E-4);
            // addExpIntoList(expList, "likelihood", -1816.6943, 5.8444E-2);
            // addExpIntoList(expList, "coalescent", 7.2378, 9.1912E-3);
            // break;

            default:
                throw new Exception("No such XML");
        }

        return expList;
    }

} // class ResumeTest