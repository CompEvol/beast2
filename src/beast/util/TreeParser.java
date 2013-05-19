/*
* File TreeParser.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is part of BEAST2.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/
package beast.util;

import beast.core.Description;
import beast.core.Input;
import beast.core.StateNode;
import beast.core.StateNodeInitialiser;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

@Description("Create beast.tree by parsing from a specification of a beast.tree in Newick format " +
        "(includes parsing of any meta data in the Newick string).")
public class TreeParser extends Tree implements StateNodeInitialiser {
    /**
     * default beast.tree branch length, used when that info is not in the Newick beast.tree
     */
    final static double DEFAULT_LENGTH = 0.001f;

    /**
     * labels of leafs *
     */
    List<String> m_sLabels = null;
    /**
     * for memory saving, set to true *
     */
    boolean m_bSurpressMetadata = false;

    /**
     * if there is no translate block. This solves issues where the taxa labels are numbers e.g. in generated beast.tree data *
     */
    public Input<Boolean> m_bIsLabelledNewick = new Input<Boolean>("IsLabelledNewick", "Is the newick tree labelled? Default=false.", false);


    public Input<Alignment> m_oData = new Input<Alignment>("taxa", "Specifies the list of taxa represented by leaves in the beast.tree");
    public Input<String> m_oNewick = new Input<String>("newick", "initial beast.tree represented in newick format");// not required, Beauti may need this for example
    public Input<String> m_oNodeType = new Input<String>("nodetype", "type of the nodes in the beast.tree", Node.class.getName());
    public Input<Integer> m_nOffset = new Input<Integer>("offset", "offset if numbers are used for taxa (offset=the lowest taxa number) default=1", 1);
    public Input<Double> m_nThreshold = new Input<Double>("threshold", "threshold under which node heights (derived from lengths) are set to zero. Default=0.", 0.0);
    public Input<Boolean> m_bAllowSingleChild = new Input<Boolean>("singlechild", "flag to indicate that single child nodes are allowed. Default=true.", true);
    public Input<Boolean> adjustTipHeightsWhenMissingDateTraitsInput = new Input<Boolean>("adjustTipHeights", "flag to indicate if tipHeights shall be adjusted when date traits missing. Default=true.", true);
	public Input<Double> scale = new Input<Double>("scale", "scale used to multiply internal node heights during parsing." +
			"Useful for importing starting from external programs, for instance, RaxML tree rooted using Path-o-gen.", 1.0);


    boolean createUnrecognizedTaxa = false;

    // if true and no date traits available then tips heights will be adjusted to zero.
    private boolean adjustTipHeightsWhenMissingDateTraits; // = true;

    /**
     * op
     * assure the class behaves properly, even when inputs are not specified *
     */
    @Override
    public void initAndValidate() throws Exception {

        adjustTipHeightsWhenMissingDateTraits = adjustTipHeightsWhenMissingDateTraitsInput.get();

        if (m_oData.get() != null) {
            m_sLabels = m_oData.get().getTaxaNames();
        } else if (m_taxonset.get() != null) {
            m_sLabels = m_taxonset.get().asStringList();
        } else {
        	if (m_bIsLabelledNewick.get()) {
        		m_sLabels = new ArrayList<String>();
        		createUnrecognizedTaxa = true;
        	} else {
        		if (m_initial.get() != null) {
            		// try to pick up taxa from initial tree
        			Tree tree = m_initial.get();
        	        if (tree.m_taxonset.get() != null) {
        	            m_sLabels = tree.m_taxonset.get().asStringList();
        	        } else {
            			// m_sLabels = null;
        	        }        			
        		} else {
        			// m_sLabels = null;
        		}
        	}
//            m_bIsLabelledNewick = false;
        }
        String sNewick = m_oNewick.get();
        if (sNewick == null || sNewick.equals("")) {
            // can happen while initalising Beauti
            Node dummy = new Node();
            setRoot(dummy);
        } else {
            setRoot(parseNewick(m_oNewick.get()));
        }

        super.initAndValidate();
        if (m_initial.get() != null && m_initial.get().m_trait.get() != null) {
            adjustTreeToNodeHeights(root, m_initial.get().m_trait.get());
        } else if (m_trait.get() == null && adjustTipHeightsWhenMissingDateTraits) {
        	// all nodes should be at zero height if no date-trait is available
        	for (int i = 0; i < getLeafNodeCount(); i++) {
        		getNode(i).setHeight(0);
        	}
        }
        initStateNodes();
    } // init

    /**
     * used to make sure all taxa only occur once in the tree *
     */
    List<Boolean> m_bTaxonIndexInUse = new ArrayList<Boolean>();

    public TreeParser() {
    }

    public TreeParser(Alignment alignment, String newick) throws Exception {
        m_oData.setValue(alignment, this);
        m_oNewick.setValue(newick, this);
        initAndValidate();
    }

    /**
     * Create a tree from the given newick format
     * @param taxaNames a list of taxa names to use, or null.
     *                  If null then IsLabelledNewick will be set to true
     * @param newick the newick of the tree
     * @param offset the offset to map node numbers in newick format to indices in taxaNames.
     *               so, name(node with nodeNumber) = taxaNames[nodeNumber-offset]
     * @param adjustTipHeightsWhenMissingDateTraits true if tip heights should be adjusted to zero
     * @throws Exception
     */
    public TreeParser(List<String> taxaNames,
                      String newick,
                      int offset,
                      boolean adjustTipHeightsWhenMissingDateTraits) throws Exception {

        if (taxaNames == null) {
            m_bIsLabelledNewick.setValue(true,this);
        } else {
            m_taxonset.setValue(new TaxonSet(TaxonSet.createTaxonList(taxaNames)), this);
        }
        m_oNewick.setValue(newick, this);
    	m_nOffset.setValue(offset, this);
        adjustTipHeightsWhenMissingDateTraitsInput.setValue(adjustTipHeightsWhenMissingDateTraits, this);
        initAndValidate();
    }

    /**
     * Parses newick format. The default does not adjust heights and allows single child nodes.
     * Modifications of the input should be deliberately made by calling e.g. new TreeParser(newick, true, false).
     * @param newick a string representing a tree in newick format
     */
    public TreeParser(String newick) throws Exception {
        this(newick, false, true);
    }

    /**
     * @param newick a string representing a tree in newick format
     * @param adjustTipHeights true if the tip heights should be adjusted to 0 (i.e. contemporaneous) after reading in tree.
     * @param allowSingleChildNodes true if internal nodes with single children are allowed
     * @throws Exception
     */
    public TreeParser(String newick,
                      boolean adjustTipHeights,
                      boolean allowSingleChildNodes) throws Exception {

        m_oNewick.setValue(newick, this);
        m_bIsLabelledNewick.setValue(true, this);
        adjustTipHeightsWhenMissingDateTraitsInput.setValue(adjustTipHeights, this);
        m_bAllowSingleChild.setValue(allowSingleChildNodes,this);

        initAndValidate();
    }

//    public static void main(String[] args) { //main for testing
//
//        try {
//            TreeParser tree = new TreeParser(testTree);
//            System.out.println(tree);
//            System.out.println(tree.m_sLabels.size() + ", " + tree.m_sLabels);
//            System.out.println(tree.getTaxaNames().length + ", " + tree.getTaxaNames()[0]);
//            System.out.println();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
//    static String testTree = "(((('NZAC03012359|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|H3|MT-CO1':0.11216300000000001,(((('NZAC03012667|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|F2|MT-CO1':0.0,'NZAC03013095|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|D9|MT-CO1':0.0):0.0,'NZAC03012950|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|A8|MT-CO1':0.0):0.0,'NZAC03011111|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|B4|MT-CO1':0.0):0.0,'NZAC03014260|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|A1|MT-CO1':0.0):0.12040600000000001):0.014102999999999977,(('NZAC03012426|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|G8|MT-CO1':0.09209299999999998,('NZAC03013750|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|G2|MT-CO1':0.007547999999999971,'NZAC03014351|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|D4|MT-CO1':0.007893999999999984):0.09560400000000002):0.049522999999999984,'NZAC03012238|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|C4|MT-CO1':0.177608):0.006543999999999994):0.01739200000000002,'NZAC03013231|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|D4|MT-CO1':0.423986):0.01804,('NZAC03012254|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|G5|MT-CO1':0.128029,'NZAC03011059|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|G4|MT-CO1':0.127996):0.02707100000000001,(('NZAC03013355|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|G9|MT-CO1':0.07901399999999997,'NZAC03013244|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|A5|MT-CO1':0.09918099999999996):0.10010300000000005,(((('NZAC03009031|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|E1|MT-CO1':0.14106599999999997,((('NZAC03012228|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|C3|MT-CO1':0.093645,'NZAC03014073|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|C12|MT-CO1':0.097693):8.170000000000122E-4,(('NZAC03012253|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|F5|MT-CO1':0.08330199999999999,('NZAC03012380|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|B5|MT-CO1':0.0,'NZAC03012376|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|A5|MT-CO1':0.0):0.06699699999999997):0.02536000000000005,('NZAC03013804|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|H3|MT-CO1':0.0,'NZAC03013834|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|H4|MT-CO1':0.0):0.08147400000000002):0.0063489999999999935):0.003231999999999957,('NZAC03013852|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|D5|MT-CO1':0.08582100000000001,'NZAC03014345|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|C4|MT-CO1':0.09151500000000001):0.00925699999999996):0.010525000000000007):0.031189000000000022,(((('NZAC03010168|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|C7|MT-CO1':0.05872199999999997,'NZAC03009287|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|F3|MT-CO1':0.08092999999999997):0.012261999999999995,('NZAC03009332|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|B5|MT-CO1':0.08432600000000001,('NZAC03010334|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|D12|MT-CO1':0.0,'NZAC03009276|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|A3|MT-CO1':0.005701000000000012):0.09344400000000003):0.01625099999999996):0.044520000000000004,'NZAC03011947|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|G2|MT-CO1':0.345071):0.03065300000000004,((('NZAC03012310|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|D1|MT-CO1':0.16491299999999998,('NZAC03012605|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|A4|MT-CO1':0.08055899999999999,'NZAC03013878|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|B6|MT-CO1':0.08354600000000001):0.085001):0.018401,('NZAC03012555|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|D1|MT-CO1':0.011548000000000003,'NZAC03013094|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|C9|MT-CO1':0.005415000000000003):0.125029):0.010989000000000027,(((((((((('NZAC03011987|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|A10|MT-CO1':0.0,('NZAC03012016|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|F10|MT-CO1':0.0,('NZAC03012173|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-17|H2|MT-CO1':0.0019540000000000113,'NZAC03011953|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|E9|MT-CO1':0.0):0.002728000000000008):0.0024089999999999945):0.0012079999999999869,'NZAC03012050|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|C11|MT-CO1':0.0):6.059999999999954E-4,'NZAC03011981|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|H9|MT-CO1':0.0):3.039999999999987E-4,'NZAC03012171|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-17|G2|MT-CO1':0.0):1.5200000000001324E-4,'NZAC03012164|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-17|E2|MT-CO1':0.0):7.599999999999274E-5,'NZAC03012158|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-17|C2|MT-CO1':0.0):3.800000000001025E-5,'NZAC03012034|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|H10|MT-CO1':0.0):1.8999999999991246E-5,'NZAC03012106|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-17|C1|MT-CO1':0.0):1.0000000000010001E-5,'NZAC03012089|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|B12|MT-CO1':0.0):5.0000000000050004E-6,'NZAC03012117|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-17|D1|MT-CO1':0.0):0.12981600000000001):0.028577999999999992):0.006129999999999969):0.016545000000000032,(((((('NZAC03012286|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|A9|MT-CO1':0.0,'NZAC03012194|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|B1|MT-CO1':0.0):0.0,'NZAC03012338|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|E2|MT-CO1':0.0):0.09558,(('NZAC03012241|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|F4|MT-CO1':8.039999999999992E-4,'NZAC03011945|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|B3|MT-CO1':0.00687299999999999):0.06524799999999997,'NZAC03012330|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|C1|MT-CO1':0.058145999999999975):0.014699000000000018):0.013813999999999993,'NZAC03014280|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|D2|MT-CO1':0.113504):0.06012300000000004,(('NZAC03012358|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|G3|MT-CO1':0.0038960000000000106,'NZAC03011238|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|D6|MT-CO1':0.005392999999999981):0.198658,'NZAC03013267|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|G5|MT-CO1':0.171306):0.013004000000000016):0.01643899999999998,'NZAC03012909|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|G7|MT-CO1':0.155277):0.011274000000000006):0.01327600000000001,(((('NZAC03010362|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-19|D2|MT-CO1':0.16177600000000003,'NZAC03011232|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|B6|MT-CO1':0.18351300000000004):0.02027899999999999,('NZAC03012215|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|E2|MT-CO1':0.15269199999999997,((('NZAC03013298|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-17|A3|MT-CO1':0.17274,('NZAC03014218|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|C12|MT-CO1':0.0,'NZAC03014137|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|G9|MT-CO1':2.3200000000000998E-4):0.12890400000000002):0.032730999999999955,('NZAC03011802|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|F8|MT-CO1':0.0020729999999999915,('NZAC03012933|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|C7|MT-CO1':0.0021869999999999945,'NZAC03011154|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|H4|MT-CO1':0.0025049999999999795):0.0025519999999999987):0.14743499999999998):0.026594000000000007,'NZAC03014301|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|B3|MT-CO1':0.168613):0.01700299999999999):0.007302000000000031):0.008569999999999967,'NZAC03012198|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|D1|MT-CO1':0.18230499999999997):0.011642000000000041,((((((('NZAC03010176|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|B8|MT-CO1':0.0,'NZAC03013333|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|G8|MT-CO1':0.0):7.030000000000092E-4,'NZAC03010177|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|C8|MT-CO1':0.0):5.120000000000124E-4,'NZAC03009319|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|E4|MT-CO1':0.0):0.003019999999999995,'NZAC03014427|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-23|H11|MT-CO1':0.0017590000000000106):0.129718,('NZAC03010210|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|D10|MT-CO1':0.049259,(('NZAC03013085|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|G8|MT-CO1':0.0,'NZAC03011167|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|E5|MT-CO1':0.0):0.0015259999999999996,'NZAC03014266|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|D1|MT-CO1':0.004609000000000002):0.049093):0.09873399999999999):0.018019000000000007,('NZAC03012131|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-17|E1|MT-CO1':0.0,'NZAC03012154|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-17|A2|MT-CO1':0.005147000000000013):0.14382):0.013355999999999979,((((((((((('NZAC03010343|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-19|A1|MT-CO1':0.0,(('NZAC03010174|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|A8|MT-CO1':0.0,'NZAC03009334|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|C5|MT-CO1':0.006817999999999991):0.0034190000000000054,'NZAC03013169|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|G10|MT-CO1':0.0):0.004296999999999995):1.9799999999997597E-4,'NZAC03010964|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|E4|MT-CO1':0.0):4.129999999999967E-4,'NZAC03012115|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|E7|MT-CO1':0.0011300000000000199):0.0011789999999999856,('NZAC03012275|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|H7|MT-CO1':0.0,'NZAC03012370|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|F4|MT-CO1':0.0):3.4800000000001496E-4):0.16735900000000004,(('NZAC03011807|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|G8|MT-CO1':0.0,'NZAC03013025|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|G8|MT-CO1':0.004936999999999997):0.007545999999999997,'NZAC03010930|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|F3|MT-CO1':0.0):0.122033):0.013731999999999966,('NZAC03013230|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|C4|MT-CO1':0.0,'NZAC03013283|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|F6|MT-CO1':4.499999999998949E-5):0.11528299999999997):0.010806000000000038,((('NZAC03010182|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|G8|MT-CO1':0.041830000000000034,'NZAC03011249|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|A7|MT-CO1':0.04228900000000002):0.056564999999999976,'NZAC03012939|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|E7|MT-CO1':0.09093600000000002):0.041181999999999996,('NZAC03010368|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-19|H2|MT-CO1':0.055578000000000016,((('NZAC03010149|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|H5|MT-CO1':0.0016569999999999918,'NZAC03013036|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|H8|MT-CO1':0.0):2.630000000000132E-4,'NZAC03013377|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|D11|MT-CO1':0.0013410000000000089):2.9000000000001247E-4,'NZAC03010926|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|E3|MT-CO1':0.0):0.07314999999999999):0.04238799999999998):0.011478000000000044):0.017640999999999962,(((('NZAC03011721|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|A8|MT-CO1':0.015501999999999988,'NZAC03013117|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|A10|MT-CO1':0.019067):0.09332099999999999,'NZAC03013801|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|F3|MT-CO1':0.10981799999999997):0.014884000000000008,(((('NZAC03011351|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|F6|MT-CO1':0.003799999999999998,'NZAC03011054|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|F2|MT-CO1':0.005922000000000011):0.0032000000000000084,('NZAC03014196|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|B11|MT-CO1':0.0,'NZAC03014276|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|A2|MT-CO1':0.0):0.004094000000000014):0.0010950000000000126,'NZAC03011027|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|A2|MT-CO1':0.003704000000000013):0.0018869999999999998,'NZAC03014429|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-23|A12|MT-CO1':0.004849999999999993):0.1149):0.024144999999999972,(('NZAC03013061|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|E8|MT-CO1':0.0012770000000000004,'NZAC03014149|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|D10|MT-CO1':2.479999999999982E-4):0.0018400000000000083,'NZAC03011092|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|B3|MT-CO1':0.0044399999999999995):0.15363199999999996):0.015442000000000011):0.01119500000000001,'NZAC03012422|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|F8|MT-CO1':0.183659):0.0050180000000000224,((('NZAC03012224|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|A3|MT-CO1':0.0021110000000000295,'NZAC03012331|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|C2|MT-CO1':0.0024919999999999942):5.350000000000077E-4,'NZAC03012318|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|A2|MT-CO1':0.0025189999999999935):0.003151999999999988,('NZAC03012420|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|D8|MT-CO1':5.979999999999874E-4,'NZAC03012197|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|C1|MT-CO1':0.0):0.009204999999999963):0.11979200000000001):0.0027469999999999994,((('NZAC03009330|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|A5|MT-CO1':0.0071309999999999985,('NZAC03011204|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|B5|MT-CO1':0.0062470000000000026,'NZAC03012975|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|D8|MT-CO1':0.0014220000000000343):0.0012459999999999694):0.113952,'NZAC03012349|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|H2|MT-CO1':0.15557699999999997):0.008891000000000038,('NZAC03013153|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|A9|MT-CO1':0.16244800000000004,(((('NZAC03012417|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|A8|MT-CO1':0.0,'NZAC03012243|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|G4|MT-CO1':0.0):0.0,'NZAC03012251|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|D5|MT-CO1':0.0):0.0,'NZAC03012211|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|B2|MT-CO1':0.0):0.13448999999999997,((((('NZAC03014294|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|A3|MT-CO1':0.0,'NZAC03014293|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|H2|MT-CO1':0.029642):0.09561999999999998,'NZAC03012374|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|H4|MT-CO1':0.11816099999999999):0.013826000000000005,'NZAC03013860|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|F5|MT-CO1':0.12102099999999999):0.0037100000000000466,('NZAC03010173|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|H7|MT-CO1':0.09543399999999996,(((('NZAC03011812|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|A9|MT-CO1':0.0017300000000000093,'NZAC03013892|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|G6|MT-CO1':0.0):8.680000000000077E-4,'NZAC03014354|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|E4|MT-CO1':0.0):4.3599999999999195E-4,'NZAC03014311|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|G3|MT-CO1':0.0):2.1899999999999697E-4,'NZAC03014281|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|E2|MT-CO1':0.0):0.11648399999999998):0.01120600000000005):0.0025359999999999827,(((((((('NZAC03010163|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|G6|MT-CO1':0.001939000000000024,'NZAC03012353|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|C3|MT-CO1':0.0):9.739999999999749E-4,'NZAC03011895|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|C9|MT-CO1':0.0):4.890000000000172E-4,'NZAC03011712|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|G7|MT-CO1':0.0):2.4599999999996847E-4,'NZAC03012127|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|F7|MT-CO1':0.0):0.09990299999999996,'NZAC03013115|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|H9|MT-CO1':0.15074399999999996):0.009619000000000044,('NZAC03013297|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|B7|MT-CO1':0.11542499999999997,('NZAC03013328|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|C8|MT-CO1':0.10509100000000005,'NZAC03009328|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|G4|MT-CO1':0.10278100000000001):0.006167999999999951):0.002955000000000041):0.008792999999999995,((((((('NZAC03010181|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|F8|MT-CO1':0.0,('NZAC03010170|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|E7|MT-CO1':0.0,'NZAC03010146|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|F5|MT-CO1':0.0020940000000000125):0.0013110000000000066):0.0018559999999999965,'NZAC03010150|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|A6|MT-CO1':0.0):0.0010940000000000116,'NZAC03010351|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-19|E1|MT-CO1':0.0):0.08161899999999997,((((('NZAC03010178|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|D8|MT-CO1':0.0,'NZAC03012482|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|C2|MT-CO1':0.0017830000000000068):0.0014139999999999986,(('NZAC03011886|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|B7|MT-CO1':0.0,'NZAC03013147|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|E10|MT-CO1':1.4100000000000223E-4):3.9400000000000546E-4,'NZAC03013392|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|C11|MT-CO1':0.0028759999999999897):4.929999999999934E-4):8.600000000000274E-5,'NZAC03012844|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|H1|MT-CO1':0.0):1.4800000000000924E-4,'NZAC03011220|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|D5|MT-CO1':0.0014230000000000076):4.029999999999867E-4,'NZAC03013367|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|F10|MT-CO1':0.002592000000000011):0.07329499999999997):0.005746000000000029,((('NZAC03010328|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|B12|MT-CO1':0.0,'NZAC03013282|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|E6|MT-CO1':1.6099999999999448E-4):0.0021760000000000113,(('NZAC03013186|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|D3|MT-CO1':0.0026349999999999985,(('NZAC03011143|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|C4|MT-CO1':0.0,('NZAC03011033|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|B2|MT-CO1':0.0,'NZAC03013103|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|F9|MT-CO1':0.002901999999999988):0.00137000000000001):6.919999999999982E-4,'NZAC03014146|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|C10|MT-CO1':0.0):4.3699999999999295E-4):4.4500000000000095E-4,'NZAC03011146|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|D4|MT-CO1':0.0010759999999999936):9.939999999999949E-4):0.0018309999999999993,'NZAC03014426|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-23|G11|MT-CO1':0.0013140000000000096):0.07735999999999998):0.051375000000000004,(('NZAC03010386|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-19|D4|MT-CO1':0.11603399999999997,((('NZAC03012509|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|A2|MT-CO1':0.08158100000000001,'NZAC03012336|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|E1|MT-CO1':0.081708):0.005876999999999966,((('NZAC03013097|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|E9|MT-CO1':6.059999999999954E-4,(('NZAC03014432|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-23|D12|MT-CO1':0.0,'NZAC03011089|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|A3|MT-CO1':0.0):4.8599999999998644E-4,('NZAC03013715|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|E1|MT-CO1':0.0,'NZAC03013691|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|A1|MT-CO1':9.949999999999959E-4):0.0010809999999999986):9.409999999999974E-4):5.330000000000057E-4,'NZAC03014330|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|B4|MT-CO1':0.0025409999999999877):0.0034540000000000126,(('NZAC03013284|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|G6|MT-CO1':0.0,'NZAC03013214|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|A4|MT-CO1':0.0):0.0018209999999999893,'NZAC03013335|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|H7|MT-CO1':0.0):0.008313999999999988):0.08793599999999996):0.007221000000000033,('NZAC03012696|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|G1|MT-CO1':0.10106800000000005,'NZAC03010384|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-19|B4|MT-CO1':0.11638700000000002):0.003025999999999973):0.014973999999999987):0.01153700000000002,((('NZAC03009030|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|D1|MT-CO1':0.0022919999999999885,(('NZAC03014278|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|C2|MT-CO1':0.003795999999999994,'NZAC03011106|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|G3|MT-CO1':0.0027369999999999894):0.0033130000000000104,((('NZAC03011162|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|D5|MT-CO1':0.0,'NZAC03011160|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|C5|MT-CO1':0.0):0.0,'NZAC03014197|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|C11|MT-CO1':0.0):0.0014119999999999966,'NZAC03011104|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|F3|MT-CO1':1.1300000000000199E-4):0.00861300000000001):7.719999999999949E-4):4.4900000000000495E-4,'NZAC03013331|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|F8|MT-CO1':0.0014299999999999868):0.11035699999999998,('NZAC03010988|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|E1|MT-CO1':0.0013189999999999868,'NZAC03014256|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|F12|MT-CO1':0.0):0.138247):0.004064000000000012):0.005641000000000007):0.006732000000000016,'NZAC03010996|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|G1|MT-CO1':0.13615800000000003):0.006255999999999984):0.005620999999999987,((((((('NZAC03010354|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-19|G1|MT-CO1':0.085394,'NZAC03010968|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|F4|MT-CO1':0.07331500000000002):0.024316999999999978,(('NZAC03010172|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|G7|MT-CO1':0.06541700000000003,'NZAC03012212|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|C2|MT-CO1':0.06896700000000003):0.030362999999999973,('NZAC03012408|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|B7|MT-CO1':0.09000299999999997,'NZAC03012009|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|D10|MT-CO1':0.07677799999999996):0.019033000000000022):0.03786499999999998):0.0013620000000000299,('NZAC03011301|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|A6|MT-CO1':0.05481200000000003,'NZAC03013233|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|F4|MT-CO1':0.07845700000000003):0.03765299999999999):0.002567999999999959,'NZAC03012895|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|F3|MT-CO1':0.084955):0.008826,(('NZAC03012970|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|B8|MT-CO1':6.299999999997974E-5,'NZAC03013159|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|F10|MT-CO1':0.0029980000000000007):0.095194,('NZAC03012415|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|G7|MT-CO1':0.102824,(('NZAC03013273|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|A6|MT-CO1':0.0014899999999999913,'NZAC03013403|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|B12|MT-CO1':3.500000000000725E-5):0.003039999999999987,'NZAC03013004|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|B3|MT-CO1':0.0015630000000000088):0.124478):0.01949200000000001):0.003451999999999955):0.007426000000000044,((((('NZAC03013931|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|D8|MT-CO1':2.6399999999998647E-4,'NZAC03013954|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|A9|MT-CO1':0.0):0.00247,'NZAC03013742|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|E2|MT-CO1':7.359999999999867E-4):0.00429199999999999,((((((((('NZAC03014047|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|F11|MT-CO1':0.0,'NZAC03014314|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-23|H9|MT-CO1':0.0):0.0,'NZAC03014322|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-23|D10|MT-CO1':0.0):0.0,'NZAC03014315|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-23|A10|MT-CO1':0.0):0.0,'NZAC03014319|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-23|C10|MT-CO1':0.0):0.0,'NZAC03014024|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|F10|MT-CO1':0.0):0.0,'NZAC03013761|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|B3|MT-CO1':0.0):0.0,'NZAC03013898|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|A7|MT-CO1':0.0):0.0,'NZAC03013703|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|B1|MT-CO1':0.0):9.700000000001374E-5,'NZAC03013765|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|D3|MT-CO1':0.0014270000000000116):0.005252000000000007):0.102213,((((((((((((('NZAC03012260|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|A6|MT-CO1':0.0,'NZAC03012403|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|F6|MT-CO1':0.0):0.0,'NZAC03012220|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|G2|MT-CO1':0.0):0.0,'NZAC03012401|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|E6|MT-CO1':0.0):0.0,'NZAC03012351|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|B3|MT-CO1':0.0):0.0,'NZAC03012282|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|D8|MT-CO1':0.0):0.0,'NZAC03012240|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|E4|MT-CO1':0.0):0.0,'NZAC03012208|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|H1|MT-CO1':0.0):0.0,'NZAC03012225|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|B3|MT-CO1':0.0):1.0399999999999299E-4,'NZAC03012405|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|H6|MT-CO1':0.0):5.1999999999996493E-5,'NZAC03012285|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|E8|MT-CO1':0.0):2.5999999999998247E-5,'NZAC03012419|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|C8|MT-CO1':0.0):9.349999999999914E-4,('NZAC03012409|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|C7|MT-CO1':0.0,'NZAC03012247|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|B5|MT-CO1':0.0):6.170000000000064E-4):0.0067010000000000125,'NZAC03012365|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|B4|MT-CO1':0.0017610000000000126):0.11451099999999997):0.007703000000000015,((((('NZAC03013358|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|A10|MT-CO1':3.7499999999998646E-4,'NZAC03012479|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|A2|MT-CO1':0.0):0.0011589999999999934,(('NZAC03013376|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|C11|MT-CO1':0.0,'NZAC03012344|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|H1|MT-CO1':0.0):0.0,'NZAC03013235|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|G4|MT-CO1':0.0):3.740000000000132E-4):0.009378999999999998,'NZAC03013254|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|B5|MT-CO1':0.0):0.024609999999999993,'NZAC03013734|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|B2|MT-CO1':0.042438000000000003):0.09387200000000001,'NZAC03011156|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|A5|MT-CO1':0.14298300000000003):0.0020339999999999803):0.009166000000000007):0.0029850000000000154,(((((((('NZAC03010171|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|F7|MT-CO1':0.0024899999999999922,('NZAC03010152|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|C6|MT-CO1':5.179999999999629E-4,'NZAC03009040|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|A2|MT-CO1':0.0010930000000000106):7.980000000000209E-4):0.001784999999999981,'NZAC03010183|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|H8|MT-CO1':0.003157999999999994):0.07190800000000003,'NZAC03009313|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|C4|MT-CO1':0.07247100000000001):0.006117999999999957,'NZAC03010164|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|H6|MT-CO1':0.08235399999999998):0.004842000000000013,('NZAC03013082|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|F8|MT-CO1':0.0,'NZAC03014141|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|A10|MT-CO1':0.0):0.09839399999999998):0.022245000000000015,'NZAC03011688|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|C7|MT-CO1':0.10856100000000002):0.007390999999999981,((((((((('NZAC03010378|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-19|E3|MT-CO1':0.0,'NZAC03014289|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|F2|MT-CO1':0.0015799999999999703):2.9000000000056758E-5,'NZAC03013418|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|G12|MT-CO1':0.0):2.999999999997449E-5,'NZAC03012677|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|G2|MT-CO1':0.0):1.8699999999999273E-4,'NZAC03014308|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|F3|MT-CO1':0.0013390000000000346):2.3800000000001598E-4,('NZAC03014043|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|F9|MT-CO1':0.0,'NZAC03014263|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|C1|MT-CO1':0.0):0.0012870000000000381):3.269999999999662E-4,'NZAC03012151|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-17|H1|MT-CO1':0.0012020000000000364):3.4200000000000896E-4,('NZAC03014262|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|B1|MT-CO1':5.020000000000024E-4,'NZAC03014144|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|B10|MT-CO1':0.0):0.0011869999999999936):0.06731599999999999,'NZAC03013737|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|D2|MT-CO1':0.06541399999999997):0.027185999999999988,((((((((((('NZAC03009029|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|C1|MT-CO1':0.0,'NZAC03012404|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|G6|MT-CO1':8.620000000000017E-4):1.5200000000001324E-4,(((('NZAC03010367|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-19|G2|MT-CO1':0.0,('NZAC03012229|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|D3|MT-CO1':0.0010070000000000079,(((((('NZAC03012354|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|D3|MT-CO1':0.0,'NZAC03012246|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|A5|MT-CO1':0.0):0.0,'NZAC03011976|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|E2|MT-CO1':0.0):0.0,'NZAC03011896|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|D9|MT-CO1':0.0):0.0,'NZAC03012973|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|C8|MT-CO1':0.0):4.0100000000001246E-4,'NZAC03012214|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|D2|MT-CO1':0.0011549999999999894):3.700000000000092E-4,'NZAC03012803|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|F4|MT-CO1':0.0):5.529999999999979E-4):6.359999999999977E-4):3.2099999999998796E-4,'NZAC03012209|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|A2|MT-CO1':0.0):1.6199999999999548E-4,'NZAC03012321|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|B2|MT-CO1':0.0):8.199999999999874E-5,'NZAC03012248|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|C5|MT-CO1':0.0):6.140000000000034E-4):3.3099999999999796E-4,'NZAC03012674|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|H3|MT-CO1':0.0):5.320000000000047E-4,'NZAC03012207|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|G1|MT-CO1':9.830000000000116E-4):4.959999999999964E-4,'NZAC03014434|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-23|E12|MT-CO1':0.0010469999999999924):0.005429999999999963,'NZAC03011411|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|H6|MT-CO1':0.008474999999999983):0.05361900000000003,(((('NZAC03012280|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|B8|MT-CO1':0.0013980000000000103,('NZAC03012355|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|E3|MT-CO1':0.0,'NZAC03012270|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|G6|MT-CO1':0.0):1.2799999999998923E-4):2.6699999999998947E-4,'NZAC03012416|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|H7|MT-CO1':0.0012639999999999874):0.037969,'NZAC03013309|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|B11|MT-CO1':0.03573399999999999):0.01866000000000001,('NZAC03012100|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-17|B1|MT-CO1':0.009568999999999994,((((('NZAC03011208|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|C5|MT-CO1':0.0025969999999999605,'NZAC03011297|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|H5|MT-CO1':5.269999999999442E-4):6.880000000000219E-4,'NZAC03011148|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|E4|MT-CO1':8.519999999999639E-4):0.0016059999999999963,'NZAC03011294|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|G5|MT-CO1':0.0):0.0019589999999999885,'NZAC03011308|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|D6|MT-CO1':0.01031499999999999):0.0014799999999999813,'NZAC03011302|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|B6|MT-CO1':0.009176999999999963):0.003487000000000018):0.043362999999999985):0.002929999999999988):0.010388000000000008,(((((('NZAC03012223|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|H2|MT-CO1':0.0,('NZAC03012962|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|H7|MT-CO1':0.0,'NZAC03013119|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|B10|MT-CO1':0.0):0.001699000000000006):8.799999999997699E-5,'NZAC03013301|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|C7|MT-CO1':0.0):0.0013410000000000366,'NZAC03011876|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|B9|MT-CO1':0.0017120000000000468):0.0017079999999999318,'NZAC03011407|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|G6|MT-CO1':0.0013539999999999663):0.002919000000000005,('NZAC03012845|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|G4|MT-CO1':8.299999999999974E-4,('NZAC03013330|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|E8|MT-CO1':0.0,('NZAC03013361|Plot_8|Subplot_O|Leaf_Litter_Collection|LBILIS80|B10|MT-CO1':2.7399999999999647E-4,((('NZAC03012817|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|B1|MT-CO1':0.0,'NZAC03012600|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|D2|MT-CO1':1.26000000000015E-4):6.40000000000085E-5,'NZAC03012797|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|B4|MT-CO1':0.0):3.2000000000032E-5,'NZAC03012438|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|A3|MT-CO1':0.0):0.0012550000000000061):2.569999999999517E-4):6.990000000000052E-4):0.003278000000000003):0.015468000000000037,'NZAC03012947|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|G7|MT-CO1':0.023417999999999994):0.039561999999999986):0.016023999999999983,(('NZAC03012239|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|D4|MT-CO1':0.0,'NZAC03012366|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|C4|MT-CO1':0.0):0.05667699999999998,'NZAC03012337|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-14|D2|MT-CO1':0.06037999999999999):0.00967800000000002):0.003568999999999989,(('NZAC03010211|Plot_7|Subplot_M|Leaf_Litter_Collection|lbi-20120427-18|E10|MT-CO1':0.001963000000000048,'NZAC03012274|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|G7|MT-CO1':0.0):0.07201599999999997,'NZAC03013845|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|B5|MT-CO1':0.105741):0.007909999999999973):0.0020180000000000198,'NZAC03012277|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|A8|MT-CO1':0.08088699999999999):0.02742):0.0034310000000000174):0.002585000000000004,((('NZAC03012301|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|F9|MT-CO1':0.065882,('NZAC03011675|Plot_6|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|A7|MT-CO1':0.0024460000000000037,'NZAC03013146|Plot_6|Subplot_N|Leaf_Litter_Collection|lbi-20120427-17|D10|MT-CO1':0.0):0.08281099999999997):0.015932,(('NZAC03011149|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|F4|MT-CO1':4.7799999999997844E-4,'NZAC03014304|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|C3|MT-CO1':0.0):0.07884400000000003,'NZAC03013952|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|H8|MT-CO1':0.10178800000000005):0.009814999999999963):0.008234000000000019,(((((('NZAC03013735|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|C2|MT-CO1':0.0010000000000000009,(('NZAC03013717|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|F1|MT-CO1':0.003685999999999967,'NZAC03013984|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|F9|MT-CO1':0.0010270000000000001):0.0011360000000000259,'NZAC03013986|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|H9|MT-CO1':0.0):0.005931999999999937):0.0023859999999999992,(((('NZAC03014079|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|F12|MT-CO1':0.0,'NZAC03013922|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|C8|MT-CO1':0.0):0.0,'NZAC03014425|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-23|F11|MT-CO1':0.0):0.0,'NZAC03013844|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|A5|MT-CO1':0.0):5.299999999996974E-5,'NZAC03013864|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|A6|MT-CO1':0.001471):1.1200000000000099E-4):6.000000000000449E-4,'NZAC03014337|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-23|G10|MT-CO1':9.330000000000171E-4):0.0038229999999999653,'NZAC03014093|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|H12|MT-CO1':0.0):0.06359299999999996,('NZAC03013936|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|E8|MT-CO1':0.073264,((('NZAC03014050|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-24|G11|MT-CO1':0.0,'NZAC03014194|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|A11|MT-CO1':0.001679999999999987):8.439999999999837E-4,'NZAC03014267|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|E1|MT-CO1':0.0):4.2400000000003546E-4,'NZAC03014203|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|F11|MT-CO1':0.0):0.09815100000000004):0.00785899999999995):0.007817000000000018,(((('NZAC03014431|LB1|Subplot_E|Leaf_Litter_Collection|lbi-20120427-23|C12|MT-CO1':0.0,'NZAC03011245|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|G6|MT-CO1':0.0):7.199999999996098E-5,'NZAC03010980|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|C1|MT-CO1':0.0):5.580000000000029E-4,'NZAC03014277|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-23|B2|MT-CO1':9.689999999999976E-4):0.098165,(((('NZAC03012299|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|D9|MT-CO1':0.0011200000000000099,'NZAC03012288|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|G8|MT-CO1':4.3499999999999095E-4):0.0010439999999999894,'NZAC03012244|Plot_4|Subplot_A|Leaf_Litter_Collection|lbi-20120427-13|H4|MT-CO1':5.120000000000124E-4):3.2599999999999296E-4,'NZAC03012564|Plot_5|Subplot_I|Leaf_Litter_Collection|lbi-20120427-16|H4|MT-CO1':0.0012320000000000109):0.04939399999999999,'NZAC03014255|CM30c30|Subplot_L|Leaf_Litter_Collection|lbi-20120427-22|E12|MT-CO1':0.07014900000000002):0.049512999999999974):0.014959):0.007840000000000014):0.0011869999999999936):0.0011920000000000264):0.001556999999999975):0.0010100000000000109):0.00593699999999997):0.004418000000000033):8.960000000000079E-4):0.008207999999999993):0.007446999999999981):0.004406000000000021):0.0025350000000000095):0.013990000000000002):0.016820999999999975);";

    Node newNode() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return (Node) Class.forName(m_oNodeType.get()).newInstance();
        //return new NodeData();
    }

    void processMetadata(Node node) throws Exception {
        if (node.m_sMetaData != null) {
            String[] sMetaData = node.m_sMetaData.split(",");
            for (int i = 0; i < sMetaData.length; i++) {
                try {
                    String[] sStrs = sMetaData[i].split("=");
                    if (sStrs.length != 2) {
                        throw new Exception("misformed meta data '" + node.m_sMetaData + "'. Expected name='value' pairs");
                    }
                    String sPattern = sStrs[0];
                    sStrs[1] = sStrs[1].replaceAll("[\"']", "");
                    try {
                        Double fValue = Double.parseDouble(sStrs[1]);
                        node.setMetaData(sPattern, fValue);
                    } catch (NumberFormatException e) {
                        System.out.println("Warning: Meta data \"" + sPattern + "=" + sStrs[1] + "\" could not be interpreted as number. Storing as string.");
                        node.setMetaData(sPattern, sStrs[1]);
                    }
                } catch (Exception e) {
                    System.out.println("Warning 333: Attempt to parse metadata failed: " + node.m_sMetaData);
                    System.out.println(e.getMessage());

                }
            }
        }
        if (node.isLeaf()) {
            if (m_sLabels != null) {
                node.setID(m_sLabels.get(node.getNr()));
            }
        } else {
            processMetadata(node.getLeft());
            if (node.getRight() != null) {
                processMetadata(node.getRight());
            }
        }
    }

    void convertLengthToHeight(Node node) {
        double fTotalHeight = convertLengthToHeight(node, 0);
        offset(node, -fTotalHeight);
    }

    double convertLengthToHeight(Node node, double fHeight) {
        double fLength = node.getHeight();
        node.setHeight((fHeight - fLength) * scale.get());
        if (node.isLeaf()) {
            return node.getHeight();
        } else {
            double fLeft = convertLengthToHeight(node.getLeft(), fHeight - fLength);
            if (node.getRight() == null) {
                return fLeft;
            }
            double fRight = convertLengthToHeight(node.getRight(), fHeight - fLength);
            return Math.min(fLeft, fRight);
        }
    }

    void offset(Node node, double fDelta) {
        node.setHeight(node.getHeight() + fDelta);
        if (node.isLeaf()) {
            if (node.getHeight() < m_nThreshold.get()) {
                node.setHeight(0);
            }
        }
        if (!node.isLeaf()) {
            offset(node.getLeft(), fDelta);
            if (node.getRight() != null) {
                offset(node.getRight(), fDelta);
            }
        }
    }

    /**
     * Try to map sStr into an index. First, assume it is a number.
     * If that does not work, look in list of labels to see whether it is there.
     */
    private int getLabelIndex(String sStr) throws Exception {
        if (!m_bIsLabelledNewick.get() && m_sLabels == null) {
            try {
                int nIndex = Integer.parseInt(sStr) - m_nOffset.get();
                checkTaxaIsAvailable(sStr, nIndex);
                return nIndex;
            } catch (Exception e) {
                System.out.println(e.getClass().getName() + " " + e.getMessage() + ". Perhaps taxa or taxonset is not specified?");
            }
        }
        // look it up in list of taxa
        for (int nIndex = 0; nIndex < m_sLabels.size(); nIndex++) {
            if (sStr.equals(m_sLabels.get(nIndex))) {
                checkTaxaIsAvailable(sStr, nIndex);
                return nIndex;
            }
        }

        // if createUnrecognizedTaxa==true, then do it now, otherwise labels will not be populated and
        // out of bounds error will occur in m_sLabels later.
        if (createUnrecognizedTaxa) {
            m_sLabels.add(sStr);
            int nIndex = m_sLabels.size() - 1;
            checkTaxaIsAvailable(sStr, nIndex);
            return nIndex;
        }

        // finally, check if its an integer number indicating the taxon id
        try {
            int nIndex = Integer.parseInt(sStr) - m_nOffset.get();
            checkTaxaIsAvailable(sStr, nIndex);
            return nIndex;
        } catch (NumberFormatException e) {
        	// apparently not a number
        }
        throw new Exception("Label '" + sStr + "' in Newick beast.tree could not be identified. Perhaps taxa or taxonset is not specified?");
    }

    void checkTaxaIsAvailable(String sStr, int nIndex) throws Exception {
        while (nIndex + 1 > m_bTaxonIndexInUse.size()) {
            m_bTaxonIndexInUse.add(false);
        }
        if (m_bTaxonIndexInUse.get(nIndex)) {
            throw new Exception("Duplicate taxon found: " + sStr);
        }
        m_bTaxonIndexInUse.set(nIndex, true);
    }


    char[] m_chars;
    int m_iTokenStart;
    int m_iTokenEnd;
    final static int COMMA = 1;
    final static int BRACE_OPEN = 3;
    final static int BRACE_CLOSE = 4;
    final static int COLON = 5;
    final static int SEMI_COLON = 8;
    final static int META_DATA = 6;
    final static int TEXT = 7;
    final static int UNKNOWN = 0;

    int nextToken() {
        m_iTokenStart = m_iTokenEnd;
        while (m_iTokenEnd < m_chars.length) {
            // skip spaces
            while (m_iTokenEnd < m_chars.length && (m_chars[m_iTokenEnd] == ' ' || m_chars[m_iTokenEnd] == '\t')) {
                m_iTokenStart++;
                m_iTokenEnd++;
            }
            if (m_chars[m_iTokenEnd] == '(') {
                m_iTokenEnd++;
                return BRACE_OPEN;
            }
            if (m_chars[m_iTokenEnd] == ':') {
                m_iTokenEnd++;
                return COLON;
            }
            if (m_chars[m_iTokenEnd] == ';') {
                m_iTokenEnd++;
                return SEMI_COLON;
            }
            if (m_chars[m_iTokenEnd] == ')') {
                m_iTokenEnd++;
                return BRACE_CLOSE;
            }
            if (m_chars[m_iTokenEnd] == ',') {
                m_iTokenEnd++;
                return COMMA;
            }
            if (m_chars[m_iTokenEnd] == '[') {
                m_iTokenEnd++;
                while (m_iTokenEnd < m_chars.length && m_chars[m_iTokenEnd - 1] != ']') {
                    m_iTokenEnd++;
                }
                return META_DATA;
            }
            while (m_iTokenEnd < m_chars.length && (m_chars[m_iTokenEnd] != ' ' && m_chars[m_iTokenEnd] != '\t'
                    && m_chars[m_iTokenEnd] != '(' && m_chars[m_iTokenEnd] != ')' && m_chars[m_iTokenEnd] != '['
                    && m_chars[m_iTokenEnd] != ':' && m_chars[m_iTokenEnd] != ',' && m_chars[m_iTokenEnd] != ';')) {
                m_iTokenEnd++;
            }
            return TEXT;
        }
        return UNKNOWN;
    }

    public Node parseNewick(String sStr) throws Exception {
        // get rid of initial and terminal spaces
        sStr = sStr.replaceAll("^\\s+", "");
        sStr = sStr.replaceAll("\\s+$", "");

        try {
            m_chars = sStr.toCharArray();
            if (sStr == null || sStr.length() == 0) {
                return null;
            }
            m_iTokenStart = 0;
            m_iTokenEnd = 0;
            Vector<Node> stack = new Vector<Node>();
            Vector<Boolean> isFirstChild = new Vector<Boolean>();
            stack.add(newNode());
            isFirstChild.add(true);
            stack.lastElement().setHeight(DEFAULT_LENGTH);
            boolean bIsLabel = true;
            while (m_iTokenEnd < m_chars.length) {
                switch (nextToken()) {
                    case BRACE_OPEN: {
                        Node node2 = newNode();
                        node2.setHeight(DEFAULT_LENGTH);
                        stack.add(node2);
                        isFirstChild.add(true);
                        bIsLabel = true;
                    }
                    break;
                    case BRACE_CLOSE: {
                        if (isFirstChild.lastElement()) {
                            if (m_bAllowSingleChild.get()) {
                                // process single child nodes
                                Node left = stack.lastElement();
                                stack.remove(stack.size() - 1);
                                isFirstChild.remove(isFirstChild.size() - 1);
                                Node parent = stack.lastElement();
                                parent.setLeft(left);
                                //parent.setRight(null);
                                left.setParent(parent);
                                break;
                            } else {
                                // don't know how to process single child nodes
                                throw new Exception("Node with single child found.");
                            }
                        }
                        // process multi(i.e. more than 2)-child nodes by pairwise merging.
                        while (isFirstChild.get(isFirstChild.size() - 2) == false) {
                            Node right = stack.lastElement();
                            stack.remove(stack.size() - 1);
                            isFirstChild.remove(isFirstChild.size() - 1);
                            Node left = stack.lastElement();
                            stack.remove(stack.size() - 1);
                            isFirstChild.remove(isFirstChild.size() - 1);
                            Node dummyparent = newNode();
                            dummyparent.setHeight(DEFAULT_LENGTH);
                            dummyparent.setLeft(left);
                            left.setParent(dummyparent);
                            dummyparent.setRight(right);
                            right.setParent(dummyparent);
                            stack.add(dummyparent);
                            isFirstChild.add(false);
                        }
                        // last two nodes on stack merged into single parent node
                        Node right = stack.lastElement();
                        stack.remove(stack.size() - 1);
                        isFirstChild.remove(isFirstChild.size() - 1);
                        Node left = stack.lastElement();
                        stack.remove(stack.size() - 1);
                        isFirstChild.remove(isFirstChild.size() - 1);
                        Node parent = stack.lastElement();
                        parent.setLeft(left);
                        left.setParent(parent);
                        parent.setRight(right);
                        right.setParent(parent);
                    }
                    break;
                    case COMMA: {
                        Node node2 = newNode();
                        node2.setHeight(DEFAULT_LENGTH);
                        stack.add(node2);
                        isFirstChild.add(false);
                        bIsLabel = true;
                    }
                    break;
                    case COLON:
                        bIsLabel = false;
                        break;
                    case TEXT:
                        if (bIsLabel) {
                            String sLabel = sStr.substring(m_iTokenStart, m_iTokenEnd);
                            stack.lastElement().setNr(getLabelIndex(sLabel));
                        } else {
                            String sLength = sStr.substring(m_iTokenStart, m_iTokenEnd);
                            stack.lastElement().setHeight(Double.parseDouble(sLength));
                        }
                        break;
                    case META_DATA:
                        if (stack.lastElement().m_sMetaData == null) {
                            stack.lastElement().m_sMetaData = sStr.substring(m_iTokenStart + 1, m_iTokenEnd - 1);
                        } else {
                            stack.lastElement().m_sMetaData += " " + sStr.substring(m_iTokenStart + 1, m_iTokenEnd - 1);
                        }
                        break;
                    case SEMI_COLON:
                        //System.err.println(stack.lastElement().toString());
                        Node tree = stack.lastElement();
                        tree.sort();
                        // at this stage, all heights are actually lengths
                        convertLengthToHeight(tree);
                        int n = tree.getLeafNodeCount();
                        tree.labelInternalNodes(n);
                        if (!m_bSurpressMetadata) {
                            processMetadata(tree);
                        }
                        return stack.lastElement();
                    default:
                        throw new Exception("parseNewick: unknown token");
                }
            }
            Node tree = stack.lastElement();
            tree.sort();
            // at this stage, all heights are actually lengths
            convertLengthToHeight(tree);
            int n = tree.getLeafNodeCount();
            if (tree.getNr() == 0) {
                tree.labelInternalNodes(n);
            }
            if (!m_bSurpressMetadata) {
                processMetadata(tree);
            }
            return tree;
        } catch (Exception e) {
            System.err.println(e.getClass().toString() + "/" + e.getMessage() + ": " + sStr.substring(Math.max(0, m_iTokenStart - 100), m_iTokenStart) + " >>>" + sStr.substring(m_iTokenStart, m_iTokenEnd) + " <<< ...");
            throw new Exception(e.getMessage() + ": " + sStr.substring(Math.max(0, m_iTokenStart - 100), m_iTokenStart) + " >>>" + sStr.substring(m_iTokenStart, m_iTokenEnd) + " <<< ...");
        }
//        return node;
    }

    public void initStateNodes() {
        if (m_initial.get() != null) {
            m_initial.get().assignFrom(this);
        }
    }

    public List<StateNode> getInitialisedStateNodes() {
        List<StateNode> stateNodes = new ArrayList<StateNode>();
        if (m_initial.get() != null) {
            stateNodes.add(m_initial.get());
        }
        return stateNodes;
    }

    /**
     * Given a map of name translations (string to string),
     * rewrites all leaf ids that match a key in the map
     * to the respective value in the matching key/value pair.
     * If current leaf id is null, then interpret translation keys as node numbers (origin 1)
     * and set leaf id of node n to map.get(n-1).
     * @param translationMap
     */
    public void translateLeafIds(Map<String, String> translationMap) {

        for (Node leaf : getExternalNodes()) {
            String id = leaf.getID();

            if (id == null) {
                id = (leaf.getNr() + 1) + "";
            }

            String newId = translationMap.get(id);
            if (newId != null) {
                leaf.setID(newId);
            }
        }
    }
} // class TreeParser
