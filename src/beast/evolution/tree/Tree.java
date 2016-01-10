package beast.evolution.tree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.StateNode;
import beast.core.StateNodeInitialiser;
import beast.core.util.Log;
import beast.evolution.alignment.TaxonSet;
import beast.util.TreeParser;


@Description("Tree (the T in BEAST) representing gene beast.tree, species"
        + " beast.tree, language history, or other time-beast.tree"
        + " relationships among sequence data.")
public class Tree extends StateNode implements TreeInterface {
    final public Input<Tree> m_initial = new Input<>("initial", "tree to start with");
    final public Input<List<TraitSet>> m_traitList = new Input<>("trait",
            "trait information for initializing traits (like node dates) in the tree",
            new ArrayList<>());
    final public Input<TaxonSet> m_taxonset = new Input<>("taxonset",
            "set of taxa that correspond to the leafs in the tree");
    final public Input<String> nodeTypeInput = new Input<>("nodetype",
            "type of the nodes in the beast.tree", Node.class.getName());

    /**
     * state of dirtiness of a node in the tree
     * DIRTY means a property on the node has changed, but not the topology. "property" includes the node height
     *       and that branch length to its parent.
     * FILTHY means the nodes' parent or child has changed.
     */
    public static final int IS_CLEAN = 0, IS_DIRTY = 1, IS_FILTHY = 2;

    /**
     * counters of number of nodes, nodeCount = internalNodeCount + leafNodeCount *
     */
    protected int nodeCount = -1;
    protected int internalNodeCount = -1;
    protected int leafNodeCount = -1;

    /**
     * node representation of the beast.tree *
     */
    protected beast.evolution.tree.Node root;
    protected beast.evolution.tree.Node storedRoot;

    /**
     * array of all nodes in the tree *
     */
    protected Node[] m_nodes = null;

    protected Node[] m_storedNodes = null;

    /**
     * array of taxa names for the nodes in the tree
     * such that m_sTaxaNames[node.getNr()] == node.getID()*
     */
    String[] m_sTaxaNames = null;

    /**
     * Trait set which specifies leaf node times.
     */
    protected TraitSet timeTraitSet = null;

    /*
     * Whether or not TraitSets have been processed.
     */
    protected boolean traitsProcessed = false;

    @Override
    public void initAndValidate() throws Exception {
        if (m_initial.get() != null && !(this instanceof StateNodeInitialiser)) {
        	throw new RuntimeException("initial-input should be specified for tree that is not a StateNodeInitialiser");
//            final Tree other = m_initial.get();
//            root = other.root.copy();
//            nodeCount = other.nodeCount;
//            internalNodeCount = other.internalNodeCount;
//            leafNodeCount = other.leafNodeCount;
        }

        if (nodeCount < 0) {
            if (m_taxonset.get() != null) {
                makeCaterpillar(0, 1, false);
            } else {
                // make dummy tree with a single root node
                root = newNode();
                root.labelNr = 0;
                root.height = 0;
                root.m_tree = this;
                nodeCount = 1;
                internalNodeCount = 0;
                leafNodeCount = 1;
            }
        }

        if (nodeCount >= 0) {
            initArrays();
        }

        processTraits(m_traitList.get());

        // Ensure tree is compatible with time trait.
        if (timeTraitSet != null)
            adjustTreeNodeHeights(root);
        
        // ensure all nodes have their taxon names set up
        String [] taxa = getTaxaNames();
        for (int i = 0; i < getNodeCount() && i < taxa.length; i++) {
        	if (taxa[i] != null)
        		m_nodes[i].setID(taxa[i]);
        }
    }

    public void makeCaterpillar(final double minInternalHeight, final double step, final boolean finalize) {
        // make a caterpillar
        final List<String> taxa = m_taxonset.get().asStringList();
        Node left = newNode();
        left.labelNr = 0;
        left.height = 0;
        left.setID(taxa.get(0));
        for (int i = 1; i < taxa.size(); i++) {
            final Node right = newNode();
            right.labelNr = i;
            right.height = 0;
            right.setID(taxa.get(i));
            final Node parent = newNode();
            parent.labelNr = taxa.size() + i - 1;
            parent.height = minInternalHeight + i * step;
            left.parent = parent;
            parent.setLeft(left);
            right.parent = parent;
            parent.setRight(right);
            left = parent;
        }
        root = left;
        leafNodeCount = taxa.size();
        nodeCount = leafNodeCount * 2 - 1;
        internalNodeCount = leafNodeCount - 1;

        if (finalize) {
            initArrays();
        }
    }

    /**
     * Process trait sets.
     *
     * @param traitList List of trait sets.
     */
    protected void processTraits(List<TraitSet> traitList) {
        for (TraitSet traitSet : traitList) {
            for (Node node : getExternalNodes()) {
            	String id = node.getID();
            	if (id != null) {
                    node.setMetaData(traitSet.getTraitName(), traitSet.getValue(id));
            	}
            }
            if (traitSet.isDateTrait())
                timeTraitSet = traitSet;
        }
        traitsProcessed = true;
    }

    /**
     * Overridable method to construct new node object of the specific type
     * defined by nodeTypeInput.
     *
     * @return new node object.
     */
    protected Node newNode() {
        try {
            return (Node) Class.forName(nodeTypeInput.get()).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create node of type "
                    + nodeTypeInput.get() + ": " + e.getMessage());
        }
        //return new NodeData();
    }

    protected void initArrays() {
        // initialise tree-as-array representation + its stored variant
        m_nodes = new Node[nodeCount];
        listNodes(root, m_nodes);
        m_storedNodes = new Node[nodeCount];
        final Node copy = root.copy();
        listNodes(copy, m_storedNodes);
    }


    public Tree() {
    }

    public Tree(final Node rootNode) {
        setRoot(rootNode);
        initArrays();
    }

    /**
     * Construct a tree from newick string -- will not automatically adjust tips to zero.
     */
    public Tree(final String newick) throws Exception {
        this(new TreeParser(newick).getRoot());
    }

    /**
     * Ensure no negative branch lengths exist in tree.  This can occur if
     * leaf heights given as a trait are incompatible with the existing tree.
     */
    final static double EPSILON = 0.0000001;

    protected void adjustTreeNodeHeights(final Node node) {
        if (!node.isLeaf()) {
            for (final Node child : node.getChildren()) {
                adjustTreeNodeHeights(child);
            }
            for (final Node child : node.getChildren()) {
                final double minHeight = child.getHeight() + EPSILON;
                if (node.height < minHeight) {
                    node.height = minHeight;
                }
            }
        }
    }


    /**
     * getters and setters
     *
     * @return the number of nodes in the beast.tree
     */
    @Override
	public int getNodeCount() {
        if (nodeCount < 0) {
            nodeCount = this.root.getNodeCount();
        }

        //System.out.println("nodeCount=" + nodeCount);
        return nodeCount;
    }

    @Override
	public int getInternalNodeCount() {
        if (internalNodeCount < 0) {
            internalNodeCount = root.getInternalNodeCount();
        }
        return internalNodeCount;
    }

    @Override
	public int getLeafNodeCount() {
        //TODO will this caching work if trees can have random numbers of tips during MCMC
        if (leafNodeCount < 0) {
            leafNodeCount = root.getLeafNodeCount();
        }
        return leafNodeCount;
    }

    /**
     * @return a list of external (leaf) nodes contained in this tree
     */
    @Override
	public List<Node> getExternalNodes() {
        final ArrayList<Node> externalNodes = new ArrayList<>();
        for (int i = 0; i < getNodeCount(); i++) {
            final Node node = getNode(i);
            if (node.isLeaf()) externalNodes.add(node);
        }
        return externalNodes;
    }

    /**
     * @return a list of internal (ancestral) nodes contained in this tree, including the root node
     */
    @Override
	public List<Node> getInternalNodes() {
        final ArrayList<Node> internalNodes = new ArrayList<>();
        for (int i = 0; i < getNodeCount(); i++) {
            final Node node = getNode(i);
            if (!node.isLeaf()) internalNodes.add(node);
        }
        return internalNodes;
    }

    @Override
	public Node getRoot() {
        return root;
    }

    public void setRoot(final Node root) {
        this.root = root;
        nodeCount = this.root.getNodeCount();
        // ensure root is the last node
        if (m_nodes != null && root.labelNr != m_nodes.length - 1) {
            final int rootPos = m_nodes.length - 1;
            Node tmp = m_nodes[rootPos];
            m_nodes[rootPos] = root;
            m_nodes[root.labelNr] = tmp;
            tmp.labelNr = root.labelNr;
            m_nodes[rootPos].labelNr = rootPos;
        }
    }

    /**
     * Sets root without recalculating nodeCount or ensuring that root is the last node in the internal array.
     * Currently only used by sampled ancestor tree operators. Use carefully!
     *
     * @param root the new root node
     */
    public void setRootOnly(final Node root) {
        //TODO should we flag this with startEditing since it is an operator call?

        this.root = root;
    }

    @Override
	public Node getNode(final int nodeNr) {
        return m_nodes[nodeNr];
        //return getNode(nodeNr, root);
    }

    /**
     * @returns an array of taxon names in order of their node numbers.
     * Note that in general no special order of taxa is assumed, just the order
     * assumed in this tree. Consider using tree.m_taxonset.get().asStringList()
     * instead.
     */
    public String [] getTaxaNames() {
         if (m_sTaxaNames == null || (m_sTaxaNames.length == 1 && m_sTaxaNames[0] == null) || m_sTaxaNames.length == 0) {
            final TaxonSet taxonSet = m_taxonset.get();
            if (taxonSet != null) {
                final List<String> txs = taxonSet.asStringList();
                m_sTaxaNames = txs.toArray(new String[txs.size()]);
            } else {
                m_sTaxaNames = new String[getNodeCount()];
                collectTaxaNames(getRoot());
                List<String> taxaNames = new ArrayList<>();
                for (String name : m_sTaxaNames) {
                	if (name != null) {
                		taxaNames.add(name);
                	}
                }
                m_sTaxaNames = taxaNames.toArray(new String[]{});
                
            }
        }

        // sanity check
        if (m_sTaxaNames.length == 1 && m_sTaxaNames[0] == null) {
            Log.warning.println("WARNING: tree interrogated for taxa, but the tree was not initialised properly. To fix this, specify the taxonset input");
        }
        return m_sTaxaNames;
    }

    void collectTaxaNames(final Node node) {
        if (node.getID() != null) {
            m_sTaxaNames[node.getNr()] = node.getID();
        }
        if (node.isLeaf()) {
            if (node.getID() == null) {
            	node.setID("node" + node.getNr());
            }
        } else {
        	for (Node child : node.getChildren()) {
        		collectTaxaNames(child);
        	}
        }
    }


    /**
     * copy meta data matching pattern to double array
     *
     * @param node     the node
     * @param t       the double array to be filled with meta data
     * @param pattern the name of the meta data
     */
    @Override
	public void getMetaData(final Node node, final Double[] t, final String pattern) {
        t[Math.abs(node.getNr())] = (Double) node.getMetaData(pattern);
        if (!node.isLeaf()) {
            getMetaData(node.getLeft(), t, pattern);
            if (node.getRight() != null) {
                getMetaData(node.getRight(), t, pattern);
            }
        }
    }

    /**
     * copy meta data matching pattern to double array
     *
     * @param node     the node
     * @param t       the integer array to be filled with meta data
     * @param pattern the name of the meta data
     */
    public void getMetaData(final Node node, final Integer[] t, final String pattern) {
        t[Math.abs(node.getNr())] = (Integer) node.getMetaData(pattern);
        if (!node.isLeaf()) {
            getMetaData(node.getLeft(), t, pattern);
            if (node.getRight() != null) {
                getMetaData(node.getRight(), t, pattern);
            }
        }
    }


    /**
     * traverse tree and assign meta-data values in t to nodes in the
     * tree to the meta-data field represented by the given pattern.
     * This only has an effect when setMetadata() in a subclass
     * of Node know how to process such value.
     */
    @Override
	public void setMetaData(final Node node, final Double[] t, final String pattern) {
        node.setMetaData(pattern, t[Math.abs(node.getNr())]);
        if (!node.isLeaf()) {
            setMetaData(node.getLeft(), t, pattern);
            if (node.getRight() != null) {
                setMetaData(node.getRight(), t, pattern);
            }
        }
    }


    /**
     * convert tree to array representation *
     */
    void listNodes(final Node node, final Node[] nodes) {
        nodes[node.getNr()] = node;
        node.m_tree = this;  //(JH) I don't understand this code

        // (JH) why not  node.children, we don't keep it around??
        for (final Node child : node.getChildren()) {
            listNodes(child, nodes);
        }
    }

//    private int
//    getNodesPostOrder(final Node node, final Node[] nodes, int pos) {
//        node.m_tree = this;
//        for (final Node child : node.children) {
//            pos = getNodesPostOrder(child, nodes, pos);
//        }
//        nodes[pos] = node;
//        return pos + 1;
//    }

//    /**
//     * @param node  top of tree/sub tree (null defaults to whole tree)
//     * @param nodes array to fill (null will result in creating a new one)
//     * @return tree nodes in post-order, children before parents
//     */
//    public Node[] listNodesPostOrder(Node node, Node[] nodes) {
//        if (node == null) {
//            node = root;
//        }
//        if (nodes == null) {
//            final int n = (node == root) ? nodeCount : node.getNodeCount();
//            nodes = new Node[n];
//        }
//        getNodesPostOrder(node, nodes, 0);
//        return nodes;
//    }

    protected Node[] postCache = null;
    @Override
	public Node[] listNodesPostOrder(Node node, Node[] nodes) {
        if( node != null ) {
            return TreeInterface.super.listNodesPostOrder(node, nodes);
        }
        if( postCache == null ) {
            postCache = TreeInterface.super.listNodesPostOrder(node, nodes);
        }
        return postCache;
    }

    /**
     * @return list of nodes in array format.
     *         *
     */
    @Override
	public Node[] getNodesAsArray() {
        return m_nodes;
    }

    /**
     * deep copy, returns a completely new tree
     *
     * @return a deep copy of this beast.tree.
     */
    @Override
    public Tree copy() {
        Tree tree = new Tree();
        tree.setID(getID());
        tree.index = index;
        tree.root = root.copy();
        tree.nodeCount = nodeCount;
        tree.internalNodeCount = internalNodeCount;
        tree.leafNodeCount = leafNodeCount;
        return tree;
    }

    /**
     * copy of all values into existing tree *
     */
    @Override
    public void assignTo(final StateNode other) {
        final Tree tree = (Tree) other;
        final Node[] nodes = new Node[nodeCount];
        listNodes(tree.root, nodes);
        tree.setID(getID());
        //tree.index = index;
        root.assignTo(nodes);
        tree.root = nodes[root.getNr()];
        tree.nodeCount = nodeCount;
        tree.internalNodeCount = internalNodeCount;
        tree.leafNodeCount = leafNodeCount;
    }

    /**
     * copy of all values from existing tree *
     */
    @Override
    public void assignFrom(final StateNode other) {
        final Tree tree = (Tree) other;
        final Node[] nodes = new Node[tree.getNodeCount()];//tree.getNodesAsArray();
        for (int i = 0; i < tree.getNodeCount(); i++) {
            nodes[i] = newNode();
        }
        setID(tree.getID());
        //index = tree.index;
        root = nodes[tree.root.getNr()];
        root.assignFrom(nodes, tree.root);
        root.parent = null;
        nodeCount = tree.nodeCount;
        internalNodeCount = tree.internalNodeCount;
        leafNodeCount = tree.leafNodeCount;
        initArrays();
    }

    /**
     * as assignFrom, but only copy tree structure *
     */
    @Override
    public void assignFromFragile(final StateNode other) {
        final Tree tree = (Tree) other;
        if (m_nodes == null) {
            initArrays();
        }
        root = m_nodes[tree.root.getNr()];
        final Node[] otherNodes = tree.m_nodes;
        final int rootNr = root.getNr();
        assignFrom(0, rootNr, otherNodes);
        root.height = otherNodes[rootNr].height;
        root.parent = null;
        if (otherNodes[rootNr].getLeft() != null) {
            root.setLeft(m_nodes[otherNodes[rootNr].getLeft().getNr()]);
        } else {
            root.setLeft(null);
        }
        if (otherNodes[rootNr].getRight() != null) {
            root.setRight(m_nodes[otherNodes[rootNr].getRight().getNr()]);
        } else {
            root.setRight(null);
        }
        assignFrom(rootNr + 1, nodeCount, otherNodes);
    }

    /**
     * helper to assignFromFragile *
     */
    private void assignFrom(final int start, final int end, final Node[] otherNodes) {
        for (int i = start; i < end; i++) {
            Node sink = m_nodes[i];
            Node src = otherNodes[i];
            sink.height = src.height;
            sink.parent = m_nodes[src.parent.getNr()];
            if (src.getLeft() != null) {
                sink.setLeft(m_nodes[src.getLeft().getNr()]);
                if (src.getRight() != null) {
                    sink.setRight(m_nodes[src.getRight().getNr()]);
                } else {
                    sink.setRight(null);
                }
            }
        }
    }


    @Override
	public String toString() {
        return root.toString();
    }


    /**
     * StateNode implementation
     */
    @Override
    public void setEverythingDirty(final boolean isDirty) {
        setSomethingIsDirty(isDirty);
        if (!isDirty) {
            for( Node n : m_nodes ) {
                n.isDirty = IS_CLEAN;
            }
          //  root.makeAllDirty(IS_CLEAN);
        } else {
            for( Node n : m_nodes ) {
                n.isDirty = IS_FILTHY;
            }
        //    root.makeAllDirty(IS_FILTHY);
        }
    }

    @Override
    public int scale(final double scale) throws Exception {
        root.scale(scale);
        return getInternalNodeCount()- getDirectAncestorNodeCount();
    }


//    /**
//     * The same as scale but with option to scale all sampled nodes
//     * @param scale
//     * @param scaleSNodes if true all sampled nodes are scaled. Note, the most recent node is considered to
//     *                    have height 0.
//     * @return
//     * @throws Exception
//     */
//    public int scale(double scale, boolean scaleSNodes) throws Exception {
//        ((ZeroBranchSANode)root).scale(scale, scaleSNodes);
//        if (scaleSNodes) {
//            return getNodeCount() - 1 - getDirectAncestorNodeCount();
//        } else {
//            return getInternalNodeCount() - getDirectAncestorNodeCount();
//        }
//    }

    /** Loggable interface implementation follows **/

    /**
     * print translate block for NEXUS beast.tree file
     */
    public static void printTranslate(final Node node, final PrintStream out, final int nodeCount) {
        final List<String> translateLines = new ArrayList<>();
        printTranslate(node, translateLines, nodeCount);
        Collections.sort(translateLines);
        for (final String line : translateLines) {
            out.println(line);
        }
    }

    static public int taxaTranslationOffset = 1;

    /**
     * need this helper so that we can sort list of entries *
     */
    static void printTranslate(Node node, List<String> translateLines, int nodeCount) {
        if (node.isLeaf()) {
            final String nr = (node.getNr() + taxaTranslationOffset) + "";
            String line = "\t\t" + "    ".substring(nr.length()) + nr + " " + node.getID();
            if (node.getNr() < nodeCount) {
                line += ",";
            }
            translateLines.add(line);
        } else {
            printTranslate(node.getLeft(), translateLines, nodeCount);
            if (node.getRight() != null) {
                printTranslate(node.getRight(), translateLines, nodeCount);
            }
        }
    }

    public static void printTaxa(final Node node, final PrintStream out, final int nodeCount) {
        final List<String> translateLines = new ArrayList<>();
        printTranslate(node, translateLines, nodeCount);
        Collections.sort(translateLines);
        for (String line : translateLines) {
            line = line.split("\\s+")[2];
            out.println("\t\t\t" + line.replace(',', ' '));
        }
    }

    @Override
	public void init(PrintStream out) throws Exception {
        Node node = getRoot();
        out.println("#NEXUS\n");
        out.println("Begin taxa;");
        out.println("\tDimensions ntax=" + getLeafNodeCount() + ";");
        out.println("\t\tTaxlabels");
        printTaxa(node, out, getNodeCount() / 2);
        out.println("\t\t\t;");
        out.println("End;");

        out.println("Begin trees;");
        out.println("\tTranslate");
        printTranslate(node, out, getNodeCount() / 2);
        out.print(";");
    }

    @Override
	public void log(int sample, PrintStream out) {
        Tree tree = (Tree) getCurrent();
        out.print("tree STATE_" + sample + " = ");
        // Don't sort, this can confuse CalculationNodes relying on the tree
        //tree.getRoot().sort();
        final int[] dummy = new int[1];
        final String newick = tree.getRoot().toSortedNewick(dummy);
        out.print(newick);
        out.print(";");
    }

    /**
     * @see beast.core.Loggable *
     */
    @Override
	public void close(PrintStream out) {
        out.print("End;");
    }

    /**
     * reconstruct tree from XML fragment in the form of a DOM node *
     */
    @Override
    public void fromXML(final org.w3c.dom.Node node) {
        final String newick = node.getTextContent();
        final TreeParser parser = new TreeParser();
        try {
            parser.thresholdInput.setValue(1e-10, parser);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        try {
            parser.offsetInput.setValue(0, parser);
            setRoot(parser.parseNewick(newick));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        initArrays();
    }

    /**
     * Valuable implementation *
     */
    @Override
	public int getDimension() {
        return getNodeCount();
    }

    @Override
	public double getArrayValue() {
        return root.height;
    }

    @Override
	public double getArrayValue(int value) {
        return m_nodes[value].height;
    }

    /**
     * StateNode implementation *
     */
    @Override
    protected void store() {

        // this condition can only be true for sampled ancestor trees
        if (m_storedNodes.length != nodeCount) {
            final Node[] tmp = new Node[nodeCount];
            System.arraycopy(m_storedNodes, 0, tmp, 0, m_storedNodes.length - 1);
            if (nodeCount > m_storedNodes.length) {
                tmp[m_storedNodes.length - 1] = m_storedNodes[m_storedNodes.length - 1];
                tmp[nodeCount - 1] = newNode();
                tmp[nodeCount - 1].setNr(nodeCount - 1);
            }
            m_storedNodes = tmp;
        }

        storeNodes(0, nodeCount);
        storedRoot = m_storedNodes[root.getNr()];
    }


    /**
     * Stores nodes with index i, for start <= i < end
     * (i.e. including start but not including end)
     *
     * @param start the first index to be stored
     * @param end   nodes are stored up to but not including this index
     */
    private void storeNodes(final int start, final int end) {
        // Use direct members for speed (we are talking 5-7% or more from total time for large trees :)
        for (int i = start; i < end; i++) {
            final Node sink = m_storedNodes[i];
            final Node src = m_nodes[i];
            sink.height = src.height;

            if ( src.parent != null ) {
                sink.parent = m_storedNodes[src.parent.getNr()];
            } else {
                // currently only called in the case of sampled ancestor trees
                // where root node is not always last in the list
                sink.parent = null;
            }

            final List<Node> children = sink.children;
            final List<Node> srcChildren = src.children;

            if( children.size() == srcChildren.size() ) {
               // shave some more time by avoiding list clear and add
               for (int k = 0; k < children.size(); ++k) {
                   final Node srcChild = srcChildren.get(k);
                   // don't call addChild, which calls  setParent(..., true);
                   final Node c = m_storedNodes[srcChild.getNr()];
                   c.parent = sink;
                   children.set(k, c);
               }
            } else {
                children.clear();
                //sink.removeAllChildren(false);
                for (final Node srcChild : srcChildren) {
                    // don't call addChild, which calls  setParent(..., true);
                    final Node c = m_storedNodes[srcChild.getNr()];
                    c.parent = sink;
                    children.add(c);
                    //sink.addChild(c);
                }
            }
        }
    }

    @Override
    public void startEditing(final Operator operator) {
        super.startEditing(operator);
        postCache = null;
    }

    @Override
    public void restore() {

        // necessary for sampled ancestor trees
        nodeCount = m_storedNodes.length;

        final Node[] tmp = m_storedNodes;
        m_storedNodes = m_nodes;
        m_nodes = tmp;
        root = m_nodes[storedRoot.getNr()];

        // necessary for sampled ancestor trees,
        // we have the nodes, no need for expensive recursion
        leafNodeCount = 0;
        for( Node n : m_nodes ) {
            leafNodeCount += n.isLeaf() ? 1 : 0;
        }

        //leafNodeCount = root.getLeafNodeCount();

        hasStartedEditing = false;

        for( Node n : m_nodes ) {
            n.isDirty = Tree.IS_CLEAN;
        }

        postCache = null;
    }

    /**
     * @return Date trait set if available, null otherwise.
     */
    public TraitSet getDateTrait() {
        if (!traitsProcessed)
            processTraits(m_traitList.get());

        return timeTraitSet;
    }

    /**
     * Determine whether tree has a date/time trait set associated with it.
     *
     * @return true if so
     */
    public boolean hasDateTrait() {
        return getDateTrait() != null;
    }

    /**
     * Specifically set the date trait set for this tree. A null value simply
     * removes the existing trait set.
     *
     * @param traitSet
     */
    public void setDateTrait(TraitSet traitSet) {
        if (hasDateTrait()) {
            m_traitList.get().remove(timeTraitSet);
        }

        if (traitSet != null)
            m_traitList.get().add(traitSet);

        timeTraitSet = traitSet;
    }

    /**
     * Convert age/height to the date time scale given by a trait set,
     * if one exists.  Otherwise just return the unconverted height.
     *
     * @param height
     * @return date specified by height
     */
    public double getDate(final double height) {
        if (hasDateTrait()) {
            return timeTraitSet.getDate(height);
        } else
            return height;
    }

    /**
     * This method allows the retrieval of the taxon label of a node without using the node number.
     *
     * @param node
     * @return the name of the given node, or null if the node is unlabelled
     */
    public String getTaxonId(final Node node) {
        //TODO should be implemented to avoid using deprecated methods
        return getTaxaNames()[node.getNr()];  //To change body of created methods use File | Settings | File Templates.
    }

    /**
     * Removes the i'th node in the tree. Results in a renumbering of the remaining nodes so that their numbers
     * faithfully describe their new position in the array. nodeCount and leafNodeCount are recalculated.
     * Use with care!
     *
     * @param i the index of the node to be removed.
     */
    public void removeNode(final int i) {
        final Node[] tmp = new Node[nodeCount - 1];
        System.arraycopy(m_nodes, 0, tmp, 0, i);
        for (int j = i; j < nodeCount - 1; j++) {
            tmp[j] = m_nodes[j + 1];
            tmp[j].setNr(j);
        }
        m_nodes = tmp;
        nodeCount--;
        leafNodeCount--;
    }

    /**
     * Adds a node to the end of the node array. nodeCount and leafNodeCount are recalculated.
     * Use with care!
     */
    public void addNode(final Node newNode) {
        final Node[] tmp = new Node[nodeCount + 1];
        System.arraycopy(m_nodes, 0, tmp, 0, nodeCount);
        tmp[nodeCount] = newNode;
        newNode.setNr(nodeCount);
        m_nodes = tmp;
        nodeCount++;
        leafNodeCount++;
    }

    public int getDirectAncestorNodeCount() {
        int directAncestorNodeCount = 0;
        for (int i = 0; i < leafNodeCount; i++) {
            if (this.getNode(i).isDirectAncestor()) {
                directAncestorNodeCount += 1;
            }
        }
        return directAncestorNodeCount;
    }


    @Override
    public TaxonSet getTaxonset() {
        return m_taxonset.get();
    }
} // class Tree