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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import beast.core.Description;
import beast.core.Input;
import beast.core.StateNode;
import beast.core.StateNodeInitialiser;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.TaxonSet;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeUtils;
import beast.util.treeparser.NewickBaseVisitor;
import beast.util.treeparser.NewickLexer;
import beast.util.treeparser.NewickParser;

@Description("Create beast.tree by parsing from a specification of a beast.tree in Newick format " +
        "(includes parsing of any meta data in the Newick string).")
public class TreeParser extends Tree implements StateNodeInitialiser {
    /**
     * default beast.tree branch length, used when that info is not in the Newick beast.tree
     */
    final static double DEFAULT_LENGTH = 0.001f;

    /**
     * labels of leafs, order of this list corresponds to node numbers
     */
    List<String> labels = null;

    /**
     * for memory saving, set to true *
     */
    final boolean suppressMetadata = false;

    /**
     * This solves issues where the taxa labels are numbers (in generated
     * beast.tree data for example).
     */
    public final Input<Boolean> isLabelledNewickInput = new Input<>(
            "IsLabelledNewick",
            "Is the newick tree labelled (alternatively contains node numbers)? Default=false.", false);

    public final Input<Alignment> dataInput = new Input<>("taxa",
            "Specifies the list of taxa represented by leaves in the beast.tree");
    public final Input<String> newickInput = new Input<>("newick",
            "initial beast.tree represented in newick format");// not required, Beauti may need this for example
    public final Input<Integer> offsetInput = new Input<>("offset",
            "offset if numbers are used for taxa (offset=the lowest taxa number) default=1", 1);
    public final Input<Double> thresholdInput = new Input<>("threshold",
            "threshold under which node heights (derived from lengths) are set to zero. Default=0.", 0.0);
    public final Input<Boolean> allowSingleChildInput = new Input<>(
            "singlechild",
            "flag to indicate that single child nodes are allowed. Default=true.", true);
    public final Input<Boolean> adjustTipHeightsInput = new Input<>(
            "adjustTipHeights",
            "flag to indicate if tipHeights shall be adjusted when date traits missing. Default=true.", true);
    public final Input<Double> scaleInput = new Input<>("scale",
            "scale used to multiply internal node heights during parsing. Useful for importing starting from external" +
                    " programs, for instance, RaxML tree rooted using Path-o-gen.", 1.0);

    boolean createUnrecognizedTaxa = false;

    /**
     * Flag to indicate whether integer leaf labels have been used.  If
     * they have, these will be used to interpret Nexus label translation maps
     * in place of the node numbers.
     */
    boolean integerLeafLabels = true;

    /**
     * Ensure the class behaves properly, even when inputs are not specified.
     */
    @Override
    public void initAndValidate() throws Exception {
    	boolean sortNodesAlphabetically = false;

        if (dataInput.get() != null) {
            labels = dataInput.get().getTaxaNames();
        } else if (m_taxonset.get() != null) {
        	if (labels == null) {
        		labels = m_taxonset.get().asStringList();
        	} else { // else labels were set by TreeParser c'tor
        		sortNodesAlphabetically = true;
        	}
        } else {
            if (isLabelledNewickInput.get()) {
                if (m_initial.get() != null) {
                    labels = m_initial.get().getTaxonset().asStringList();
                } else {
                    labels = new ArrayList<>();
                    createUnrecognizedTaxa = true;
            		sortNodesAlphabetically = true;
                }
            } else {
                if (m_initial.get() != null) {
                    // try to pick up taxa from initial tree
                    final Tree tree = m_initial.get();
                    if (tree.m_taxonset.get() != null) {
                        labels = tree.m_taxonset.get().asStringList();
                    } else {
                        // m_sLabels = null;
                    }
                } else {
                    // m_sLabels = null;
                }
            }
//            m_bIsLabelledNewick = false;
        }
        final String sNewick = newickInput.get();
        if (sNewick == null || sNewick.equals("")) {
            // can happen while initalising Beauti
            final Node dummy = new Node();
            setRoot(dummy);
        } else {
            try {
                setRoot(parseNewick(newickInput.get()));
            } catch (ParseCancellationException e) {
                throw new RuntimeException(
                        "TreeParser cannot make sense of the Newick string " +
                                "provided.  It gives the following clue:\n" +
                                e.getMessage());
            }
        }

        super.initAndValidate();
        
        if (sortNodesAlphabetically) {
	        // correct for node ordering: ensure order is alphabetical
	        for (int i = 0; i < getNodeCount() && i < labels.size(); i++) {
	       		m_nodes[i].setID(labels.get(i));
	        }

	        Node [] nodes = new Node[labels.size()];
            System.arraycopy(m_nodes, 0, nodes, 0, labels.size());

	        Arrays.sort(nodes, (o1, o2) -> o1.getID().compareTo(o2.getID()));
	        for (int i = 0; i < labels.size(); i++) {
	        	m_nodes[i] = nodes[i];
	        	nodes[i].setNr(i);
	        }
        }
        
        
        if (m_initial.get() != null)
            processTraits(m_initial.get().m_traitList.get());
        else
            processTraits(m_traitList.get());

        if (timeTraitSet != null) {
            adjustTreeNodeHeights(root);
        } else if (adjustTipHeightsInput.get()) {

            double treeLength = TreeUtils.getTreeLength(this,getRoot());

            double extraTreeLength = 0.0;
            double maxTipHeight = 0.0;

            // all nodes should be at zero height if no date-trait is available
            for (int i = 0; i < getLeafNodeCount(); i++) {
                double height = getNode(i).getHeight();
                if (maxTipHeight < height) {
                    maxTipHeight = height;
                }
                extraTreeLength += height;
                getNode(i).setHeight(0);
            }

            double scaleFactor = (treeLength+extraTreeLength)/treeLength;

            final double SCALE_FACTOR_THRESHOLD = 0.001;

            // if the change in total tree length is more than 0.1% then give the user a warning!
            if (scaleFactor > 1.0 + SCALE_FACTOR_THRESHOLD) {

                DecimalFormat format = new DecimalFormat("#.##");

                System.out.println("WARNING: Adjust tip heights attribute set to 'true' in " + getClass().getSimpleName());
                System.out.println("         has resulted in significant (>" + format.format(SCALE_FACTOR_THRESHOLD*100.0) + "%) change in tree length.");
                System.out.println("         Use "+adjustTipHeightsInput.getName()+"='false' to override this default.");
                System.out.printf( "  original max tip age = %8.3f\n", maxTipHeight);
                System.out.printf( "       new max tip age = %8.3f\n", 0.0);
                System.out.printf( "  original tree length = %8.3f\n", treeLength);
                System.out.printf( "       new tree length = %8.3f\n", treeLength+extraTreeLength);
                System.out.printf( "       TL scale factor = %8.3f\n", scaleFactor);
            }
        }

        if( m_taxonset.get() == null && labels != null && isLabelledNewickInput.get() ) {
            m_taxonset.setValue(new TaxonSet(TaxonSet.createTaxonList(labels)), this);
        }

        initStateNodes();
    } // init

    public TreeParser() {
    }

    public TreeParser(final Alignment alignment, final String newick) throws Exception {
        dataInput.setValue(alignment, this);
        newickInput.setValue(newick, this);
        initAndValidate();
    }

    /**
     * Create a tree from the given newick format
     *
     * @param taxaNames a list of taxa names to use, or null.
     *                  If null then IsLabelledNewick will be set to true
     * @param newick    the newick of the tree
     * @param offset    the offset to map node numbers in newick format to indices in taxaNames.
     *                  so, name(node with nodeNumber) = taxaNames[nodeNumber-offset]
     * @param adjustTipHeightsWhenMissingDateTraits
     *                  true if tip heights should be adjusted to zero
     * @throws Exception
     */
    public TreeParser(final List<String> taxaNames,
                      final String newick,
                      final int offset,
                      final boolean adjustTipHeightsWhenMissingDateTraits) throws Exception {

        if (taxaNames == null) {
            isLabelledNewickInput.setValue(true, this);
        } else {
            m_taxonset.setValue(new TaxonSet(TaxonSet.createTaxonList(taxaNames)), this);
        }
        newickInput.setValue(newick, this);
        offsetInput.setValue(offset, this);
        adjustTipHeightsInput.setValue(adjustTipHeightsWhenMissingDateTraits, this);
        labels = taxaNames;
        initAndValidate();
    }

    /**
     * Parses newick format. The default does not adjust heights and allows single child nodes.
     * Modifications of the input should be deliberately made by calling e.g. new TreeParser(newick, true, false).
     *
     * @param newick a string representing a tree in newick format
     */
    public TreeParser(final String newick) throws Exception {
        this(newick, false, true, true, 1);
    }

    /**
     * @param newick                a string representing a tree in newick format
     * @param adjustTipHeights      true if the tip heights should be adjusted to 0 (i.e. contemporaneous) after reading in tree.
     * @param allowSingleChildNodes true if internal nodes with single children are allowed
     * @throws Exception
     */
    public TreeParser(final String newick,
                      final boolean adjustTipHeights,
                      final boolean allowSingleChildNodes) throws Exception {
        this(newick, adjustTipHeights, allowSingleChildNodes, true, 1);
    }

    /**
     * @param newick           a string representing a tree in newick format
     * @param adjustTipHeights true if the tip heights should be adjusted to 0 (i.e. contemporaneous) after reading in tree.
     * @throws Exception
     */
    public TreeParser(final String newick,
                      final boolean adjustTipHeights) throws Exception {
        this(newick, adjustTipHeights, true, true, 1);
    }

    /**
     * @param newick                a string representing a tree in newick format
     * @param adjustTipHeights      true if the tip heights should be adjusted to 0 (i.e. contemporaneous) after reading in tree.
     * @param allowSingleChildNodes true if internal nodes with single children are allowed
     * @param isLabeled             true if nodes are labeled with taxa labels
     * @param offset                if isLabeled == false and node labeling starts with x
     *                              then offset should be x. When isLabeled == true offset should
     *                              be 1 as by default.
     * @throws Exception
     */
    public TreeParser(final String newick,
                      final boolean adjustTipHeights,
                      final boolean allowSingleChildNodes,
                      final boolean isLabeled,
                      final int offset) throws Exception {

        newickInput.setValue(newick, this);
        isLabelledNewickInput.setValue(isLabeled, this);
        adjustTipHeightsInput.setValue(adjustTipHeights, this);
        allowSingleChildInput.setValue(allowSingleChildNodes, this);

        offsetInput.setValue(offset, this);

        initAndValidate();
    }

    /**
     * Parse a newick-ish string and generate the BEAST tree it describes.
     *
     * @param newick string to parse
     * @return root node of tree
     */
    public Node parseNewick(String newick) {
        ANTLRInputStream input = new ANTLRInputStream(newick);

        // Custom parse/lexer error listener
        BaseErrorListener errorListener = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                throw new ParseCancellationException("Error parsing character "
                        + charPositionInLine + " of Newick string: " + msg);
            }
        };

        // Use lexer to produce token stream

        NewickLexer lexer = new NewickLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Parse token stream to produce parse tree

        NewickParser parser = new NewickParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        ParseTree parseTree = parser.tree();

        // Traverse parse tree, constructing BEAST tree along the way

        NewickASTVisitor visitor = new NewickASTVisitor();
        Node root = visitor.visit(parseTree);

        return root;
    }


    /**
     * Given a map of name translations (string to string),
     * rewrites all leaf ids that match a key in the map
     * to the respective value in the matching key/value pair.
     * If current leaf id is null, then interpret translation keys as node numbers (origin 1)
     * and set leaf id of node n to map.get(n-1).
     *
     * @param translationMap map of name translations
     */
    public void translateLeafIds(final Map<String, String> translationMap) {

        for (final Node leaf : getExternalNodes()) {
            String id = leaf.getID();

            if (id == null || !integerLeafLabels) {
                id = Integer.toString(leaf.getNr() + 1);
            }

            final String newId = translationMap.get(id);
            if (newId != null) {
                leaf.setID(newId);
            }
        }
    }


    /**
     * Visits each component of the AST built from the Newick string, constructing
     * a BEAST tree along the way.
     */
    class NewickASTVisitor extends NewickBaseVisitor<Node> {

        private int numberedNodeCount = 0;

        @Override
        public Node visitTree(@NotNull NewickParser.TreeContext ctx) {
            Node root = visit(ctx.node());

            // Ensure tree is properly sorted in terms of node numbers.
            root.sort();

            // Replace lengths read from Newick with heights.
            convertLengthToHeight(root);

            // Make sure internal nodes are numbered correctly
            numberUnnumberedNodes(root);

            // Check for duplicate taxa
            BitSet nodeNrSeen = new BitSet();
            for (Node leaf : root.getAllLeafNodes()) {
                if (leaf.getNr()<0)
                   continue;  // Skip unnumbered leaves

                if (nodeNrSeen.get(leaf.getNr()))
                    throw new ParseCancellationException("Duplicate taxon found: " + labels.get(leaf.getNr()));
                else
                    nodeNrSeen.set(leaf.getNr());
            }

            return root;
        }

        @Override
        public Node visitNode(NewickParser.NodeContext ctx) {
            Node node = newNode();

            for (NewickParser.NodeContext ctxChild : ctx.node()) {
                node.addChild(visit(ctxChild));
            }

            NewickParser.PostContext postCtx = ctx.post();

            // Process metadata

            if (postCtx.meta() != null) {

                node.metaDataString = "";
                for (int i=0; i<postCtx.meta().attrib().size(); i++) {
                    if (i>0)
                        node.metaDataString += ",";
                    node.metaDataString += postCtx.meta().attrib().get(i).getText();
                }

                if (!suppressMetadata) {
                    for (NewickParser.AttribContext attribctx : postCtx.meta().attrib()) {
                        String key = attribctx.attribKey.getText();

                        if (attribctx.attribValue().number() != null) {
                            node.setMetaData(key, Double.parseDouble(
                                    attribctx.attribValue().number().getText()));
                        } else if (attribctx.attribValue().STRING() != null) {
                            String stringValue = attribctx.attribValue().STRING().getText();
                            if (stringValue.startsWith("\"") || stringValue.startsWith("\'"));
                                stringValue = stringValue.substring(1, stringValue.length()-1);
                            node.setMetaData(key, stringValue);
                        } else {
                            // BEAST doesn't do anything with vectors yet.
                        }
                    }
                }
            }

            // Process edge length

            if (postCtx.length != null)
                node.setHeight(Double.parseDouble(postCtx.length.getText()));
            else
                node.setHeight(DEFAULT_LENGTH);

            // Process label

            node.setNr(-1);
            if (postCtx.label() != null) {
                node.setID(postCtx.label().getText());

                if (postCtx.label().number() == null
                        || postCtx.label().number().INT() == null)
                    integerLeafLabels = false;

                // RRB: next line is for debugging only?
                @SuppressWarnings("unused")
				String postText = postCtx.getText();

                // Treat labels as node numbers in certain situations
                if (!isLabelledNewickInput.get()
                        && postCtx.label().number() != null
                        && postCtx.label().number().INT() != null) {

                    int nodeNr = Integer.parseInt(postCtx.label().getText()) - offsetInput.get();
                    if (nodeNr<0)
                        throw new ParseCancellationException("Node number given " +
                                "is smaller than current offset (" +
                                offsetInput.get() + ").  Perhaps offset is " +
                                "too high?");

                    node.setNr(nodeNr);
                    numberedNodeCount += 1;
                } else {
                    if (node.isLeaf()) {
                        node.setNr(getLabelIndex(postCtx.label().getText()));
                        numberedNodeCount += 1;
                    }
                }
            }

            if (node.getChildCount()==1 && !allowSingleChildInput.get())
                throw new ParseCancellationException("Node with single child found.");

            if (node.getChildCount()>2)
                throw new ParseCancellationException("Node with two or more children found.");

            return node;
        }

        /**
         * Try to map sStr into an index.
         */
        private int getLabelIndex(final String sStr) {

            // look it up in list of taxa
            for (int nIndex = 0; nIndex < labels.size(); nIndex++) {
                if (sStr.equals(labels.get(nIndex))) {
                    return nIndex;
                }
            }

            // if createUnrecognizedTaxon==true, then do it now, otherwise labels will not be populated and
            // out of bounds error will occur in m_sLabels later.
            if (createUnrecognizedTaxa) {
                labels.add(sStr);
                return labels.size() - 1;
            }

            throw new ParseCancellationException("Label '" + sStr + "' in Newick beast.tree could " +
                    "not be identified. Perhaps taxa or taxonset is not specified?");
        }

        /**
         * The node height field is initially populated with the length of the edge above due
         * to the way the tree is stored in Newick format.  This method converts these lengths
         * to actual ages before the most recent sample.
         *
         * @param root root of tree
         */
        private void convertLengthToHeight(final Node root) {
            final double fTotalHeight = convertLengthToHeight(root, 0);
            offset(root, -fTotalHeight);
        }

        /**
         * Recursive method used to convert lengths to heights.  Applied to the root,
         * results in heights from 0 to -total_height_of_tree.
         *
         * @param node node of a clade to convert
         * @param fHeight Parent height.
         * @return total height of clade
         */
        private double convertLengthToHeight(final Node node, final double fHeight) {
            final double fLength = node.getHeight();
            node.setHeight((fHeight - fLength) * scaleInput.get());
            if (node.isLeaf()) {
                return node.getHeight();
            } else {
                final double fLeft = convertLengthToHeight(node.getLeft(), fHeight - fLength);
                if (node.getRight() == null) {
                    return fLeft;
                }
                final double fRight = convertLengthToHeight(node.getRight(), fHeight - fLength);
                return Math.min(fLeft, fRight);
            }
        }

        /**
         * Method used by convertLengthToHeight(node) to remove negative offset from
         * node heights that is produced by convertLengthToHeight(node, height).
         *
         * @param node node of clade to offset
         * @param fDelta offset
         */
        private void offset(final Node node, final double fDelta) {
            node.setHeight(node.getHeight() + fDelta);
            if (node.isLeaf()) {
                if (node.getHeight() < thresholdInput.get()) {
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
         * Number any nodes in a clade which were not explicitly numbered by
         * the parsed string.
         *
         * @param node clade parent
         */
        private void numberUnnumberedNodes(Node node) {
            if (node.isLeaf())
                return;

            for (Node child : node.getChildren()) {
                numberUnnumberedNodes(child);
            }

            if (node.getNr()<0)
                node.setNr(numberedNodeCount);

            numberedNodeCount += 1;
        }
    }


    /*
     *StateNodeInitializer implementation
     */

    @Override
    public void initStateNodes() {
        if (m_initial.get() != null) {
            m_initial.get().assignFrom(this);
        }
    }

    @Override
    public void getInitialisedStateNodes(final List<StateNode> stateNodes) {
        if (m_initial.get() != null) {
            stateNodes.add(m_initial.get());
        }
    }
}
