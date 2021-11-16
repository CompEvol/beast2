package test.beast.beast2vs1;


import test.beast.beast2vs1.trace.Expectation;

import java.util.ArrayList;
import java.util.List;

public class TaxonOrderTest extends TestFramework {

    private final String[] XML_FILES = new String[]{"testStarBeast2.xml"};

    public void testStarBeast() throws Exception {
        analyse(0);
    }

    @Override
    protected void setUp() throws Exception {
    	checkESS = false;
        super.setUp(XML_FILES);
    }

    // Note: some parameter names are hard-coded in XML, so no dot, e.g. "hky.kappa26"
    @Override
	protected List<Expectation> giveExpectations(int index_XML) throws Exception {
        List<Expectation> expList = new ArrayList<Expectation>();

        // all values below are from BEAST2/examples/testStarBeast.xml
        switch (index_XML) {
            case 0: // testStarBeast2.xml
                addExpIntoList(expList, "posterior",-2200.5722,0.6459);
                addExpIntoList(expList, "prior",312.7396,0.6466);
                addExpIntoList(expList, "speciesCoalescent",287.4159,0.7055);
                addExpIntoList(expList, "SpeciesTreePopSizePrior",70.6397,0.5031);
                addExpIntoList(expList, "tree.prior.26",106.1954,0.2071);
                addExpIntoList(expList, "tree.prior.29",110.5808,0.2108);
                addExpIntoList(expList, "SpeciesTreeDivergenceTimesPrior",29.5124,0.1372);
                addExpIntoList(expList, "likelihood",-2513.3118,0.1979);
                addExpIntoList(expList, "popMean",0.0019843,0.00007611);
                addExpIntoList(expList, "birthRate",213.1348,4.9152);
                addExpIntoList(expList, "hky.kappa26",4.4936,0.0744);
                addExpIntoList(expList, "hky.kappa29",4.0749,0.0588);
                /*
                addExpIntoList(expList, "popSize.1",0.0040487,0.00027109);
                addExpIntoList(expList, "popSize.2",0.0062535,0.00026144);
                addExpIntoList(expList, "popSize.3",0.0023299,0.00015953);
                addExpIntoList(expList, "popSize.4",0.0029275,0.00023455);
                addExpIntoList(expList, "popSize.5",0.0021888,0.00022018);
                addExpIntoList(expList, "popSize.6",0.0042746,0.00018535);
                addExpIntoList(expList, "popSize.7",0.004097,0.00035926);
                addExpIntoList(expList, "popSize.8",0.0041803,0.00022475);
                addExpIntoList(expList, "popSize.9",0.0036424,0.00016521);
                addExpIntoList(expList, "popSize.10",0.0037575,0.00032465);
                addExpIntoList(expList, "popSize.11",0.0035904,0.00017545);
                addExpIntoList(expList, "popSize.12",0.0036104,0.00020949);
                addExpIntoList(expList, "popSize.13",0.0040485,0.00019889);
                addExpIntoList(expList, "popSize.14",0.0035229,0.00017978);
                addExpIntoList(expList, "popSize.15",0.005072,0.00017682);
                */
                addExpIntoList(expList, "TreeHeightSP",0.0118,0.00025562);
                addExpIntoList(expList, "TreeHeight26",0.0266,0.00010397);
                addExpIntoList(expList, "TreeHeight29",0.0224,0.000077373);                
                break;

            default:
                throw new Exception("No such XML");
        }

        return expList;
    }


} // class TaxonOrderTest