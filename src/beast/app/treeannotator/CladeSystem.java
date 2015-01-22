package beast.app.treeannotator;

import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

import java.util.*;

/**
 * extracted from TreeAnnotator
 */
//TODO merge with CladeSet?
public class CladeSystem {
    //
    // Public stuff
    //

    /**
     */
    public CladeSystem() {
    }

    /**
     */
    public CladeSystem(Tree targetTree) throws Exception {
        //this.targetTree = targetTree;
        add(targetTree, true);
    }

    /**
     * adds all the clades in the tree
     */
    public void add(Tree tree, boolean includeTips) throws Exception {
//            if (taxonList == null) {
//            	List<Taxon> taxa = new ArrayList<Taxon>();
//
//                for (String taxonName : tree.getTaxaNames()) {
//                	taxa.add(new Taxon(taxonName));
//                }
//                taxonList = new TaxonSet(taxa);
//            }

        // Recurse over the tree and add all the clades (or increment their
        // frequency if already present). The root clade is added too (for
        // annotation purposes).
        addClades(tree.getRoot(), includeTips);
    }
//
//        public Clade getClade(Node node) {
//            return null;
//        }

    private BitSet addClades(Node node, boolean includeTips) {

        BitSet bits = new BitSet();

        if (node.isLeaf()) {

            int index = getTaxonIndex(node);
            bits.set(index+1);
            //bits.set(index);

            if (includeTips) {
                addClade(bits);
            }

        } else {

            for (int i = 0; i < node.getChildCount(); i++) {

                Node node1 = node.getChild(i);

                bits.or(addClades(node1, includeTips));
            }

            if (node.isFake()) {
                bits.set(0);
            } else {
                bits.set(0, false);
            }

            addClade(bits);
        }

        return bits;
    }

    private void addClade(BitSet bits) {
        Clade clade = cladeMap.get(bits);
        if (clade == null) {
            clade = new Clade(bits);
            cladeMap.put(bits, clade);
        }
        clade.setCount(clade.getCount() + 1);
    }

    public void collectAttributes(Tree tree, Set<String> attributeNames) {
        collectAttributes(tree.getRoot(), attributeNames);
    }

    private BitSet collectAttributes(Node node, Set<String> attributeNames) {

        BitSet bits = new BitSet();

        if (node.isLeaf()) {

            int index = getTaxonIndex(node);
            if (index < 0) {
                throw new IllegalArgumentException("Taxon, " + node.getID() + ", not found in target tree");
            }
            bits.set(index+1);
            //bits.set(index);

        } else {

            for (int i = 0; i < node.getChildCount(); i++) {

                Node node1 = node.getChild(i);

                bits.or(collectAttributes(node1, attributeNames));
            }

            if (node.isFake()) {
                bits.set(0);
            } else {
                bits.set(0, false);
            }
        }

        collectAttributesForClade(bits, node, attributeNames);

        return bits;
    }

    private void collectAttributesForClade(BitSet bits, Node node, Set<String> attributeNames) {
        Clade clade = cladeMap.get(bits);
        if (clade != null) {

            if (clade.attributeValues == null) {
                clade.attributeValues = new ArrayList<Object[]>();
            }

            int i = 0;
            Object[] values = new Object[attributeNames.size()];
            for (String attributeName : attributeNames) {
                boolean processed = false;

                if (!processed) {
                    Object value;
                    if (attributeName.equals("height")) {
                        value = node.getHeight();
                    } else if (attributeName.equals("length")) {
                        value = getBranchLength(node);
// AR - we deal with this once everything
//                        } else if (attributeName.equals(location1Attribute)) {
//                            // If this is one of the two specified bivariate location names then
//                            // merge this and the other one into a single array.
//                            Object value1 = tree.getNodeAttribute(node, attributeName);
//                            Object value2 = tree.getNodeAttribute(node, location2Attribute);
//
//                            value = new Object[]{value1, value2};
//                        } else if (attributeName.equals(location2Attribute)) {
//                            // do nothing - already dealt with this...
//                            value = null;
                    } else {
                        value = node.getMetaData(attributeName);
                        if (value instanceof String && ((String) value).startsWith("\"")) {
                            value = ((String) value).replaceAll("\"", "");
                        }
                    }

                    //if (value == null) {
                    //    progressStream.println("attribute " + attributeNames[i] + " is null.");
                    //}

                    values[i] = value;
                }
                i++;
            }
            clade.attributeValues.add(values);

            //progressStream.println(clade + " " + clade.getValuesSize());
            clade.setCount(clade.getCount() + 1);
        }
    }

    private Object getBranchLength(Node node) {
        if (node.isRoot()) {
            return 0;
        }
        return node.getParent().getHeight() - node.getHeight();
    }

    public Map<BitSet, Clade> getCladeMap() {
        return cladeMap;
    }

    public void calculateCladeCredibilities(int totalTreesUsed) {
        for (Clade clade : cladeMap.values()) {

            if (clade.getCount() > totalTreesUsed) {

                throw new AssertionError("clade.getCount=(" + clade.getCount() +
                        ") should be <= totalTreesUsed = (" + totalTreesUsed + ")");
            }

            clade.setCredibility(((double) clade.getCount()) / (double) totalTreesUsed);
        }
    }

    public double getSumCladeCredibility(Node node, BitSet bits) {

        double sum = 0.0;

        if (node.isLeaf()) {

            int index = getTaxonIndex(node);
            bits.set(index+1);
            //bits.set(index);
        } else {

            BitSet bits2 = new BitSet();
            for (int i = 0; i < node.getChildCount(); i++) {

                Node node1 = node.getChild(i);

                sum += getSumCladeCredibility(node1, bits2);
            }

            sum += getCladeCredibility(bits2);

            if (bits != null) {
                bits.or(bits2);
            }
        }

        return sum;
    }

    public double getLogCladeCredibility(Node node, BitSet bits) {

        double logCladeCredibility = 0.0;

        if (node.isLeaf()) {

            int index = getTaxonIndex(node);
            bits.set(index+1);
            //bits.set(index);
        } else {

            BitSet bits2 = new BitSet();
            for (int i = 0; i < node.getChildCount(); i++) {

                Node node1 = node.getChild(i);

                logCladeCredibility += getLogCladeCredibility(node1, bits2);
            }

            if (node.isFake()) {
                bits2.set(0);
            }  else {
                bits2.set(0,false);
            }

            logCladeCredibility += Math.log(getCladeCredibility(bits2));

            if (bits != null) {
                bits.or(bits2);
            }
        }

        return logCladeCredibility;
    }

    private double getCladeCredibility(BitSet bits) {
        Clade clade = cladeMap.get(bits);
        if (clade == null) {
            return 0.0;
        }
        return clade.getCredibility();
    }

    public BitSet removeClades(Node node, boolean includeTips) {

        BitSet bits = new BitSet();

        if (node.isLeaf()) {

            int index = getTaxonIndex(node);
            bits.set(index +1);

            if (includeTips) {
                removeClade(bits);
            }

        } else {

            for (int i = 0; i < node.getChildCount(); i++) {

                Node node1 = node.getChild(i);

                bits.or(removeClades(node1, includeTips));
            }

            if (node.isFake()) {
                bits.set(0);
            } else {
                bits.set(0, false);
            }

            removeClade(bits);
        }

        return bits;
    }

    private void removeClade(BitSet bits) {
        Clade clade = cladeMap.get(bits);
        if (clade != null) {
            clade.setCount(clade.getCount() - 1);
        }

    }

    // Get tree clades as bitSets on target taxa
    // codes is an array of existing BitSet objects, which are reused

    void getTreeCladeCodes(Tree tree, BitSet[] codes) {
        getTreeCladeCodes(tree.getRoot(), codes);
    }

    int getTreeCladeCodes(Node node, BitSet[] codes) {
        final int inode = node.getNr();
        codes[inode].clear();
        if (node.isLeaf()) {
            int index = getTaxonIndex(node);//getTaxonIndex(node);
            codes[inode].set(index);
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                final Node child = node.getChild(i);
                final int childIndex = getTreeCladeCodes(child, codes);

                codes[inode].or(codes[childIndex]);
            }
        }
        return inode;
    }

    public int getTaxonIndex(Node node) {
        return node.getNr();
//        	String ID = node.getID();
//			return taxonList.asStringList().indexOf(ID);
    }

    public class Clade {
        public Clade(BitSet bits) {
            this.bits = bits;
            count = 0;
            credibility = 0.0;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public double getCredibility() {
            return credibility;
        }

        public void setCredibility(double credibility) {
            this.credibility = credibility;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Clade clade = (Clade) o;

            return !(bits != null ? !bits.equals(clade.bits) : clade.bits != null);

        }

        public int hashCode() {
            return (bits != null ? bits.hashCode() : 0);
        }

        public String toString() {
            return "clade " + bits.toString();
        }

        int count;
        double credibility;
        BitSet bits;
        List<Object[]> attributeValues = null;
    }

    //
    // Private stuff
    //
    //TaxonSet taxonList = null;
    protected Map<BitSet, Clade> cladeMap = new HashMap<BitSet, Clade>();

    //Tree targetTree;
}
