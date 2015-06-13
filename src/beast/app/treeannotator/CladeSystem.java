package beast.app.treeannotator;

import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

import java.util.*;

/**
 * extracted from TreeAnnotator
 */
//TODO merge with CladeSet?
public class CladeSystem {

    protected Map<BitSet, Clade> cladeMap = new HashMap<>();

    public CladeSystem() { }

    public CladeSystem(Tree targetTree) {
        add(targetTree, true);
    }

    /**
     * adds all the clades in the tree
     */
    public void add(Tree tree, boolean includeTips) {
        // Recurse over the tree and add all the clades (or increment their
        // frequency if already present). The root clade is added too (for
        // annotation purposes).
        addClades(tree.getRoot(), includeTips);
    }

    private BitSet addClades(Node node, boolean includeTips) {

        BitSet bits = new BitSet();

        if (node.isLeaf()) {

            int index = getTaxonIndex(node);
            bits.set(2*index);

            if (includeTips) {
                addClade(bits);
            }

        } else {

            for (int i = 0; i < node.getChildCount(); i++) {

                Node node1 = node.getChild(i);

                bits.or(addClades(node1, includeTips));
            }

            for (int i=1; i<bits.length(); i=i+2) {
                bits.set(i, false);
            }
            if (node.isFake()) {
                int index = getTaxonIndex(node.getDirectAncestorChild());
                bits.set(2 * index + 1);
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
            bits.set(2*index);

        } else {

            for (int i = 0; i < node.getChildCount(); i++) {

                Node node1 = node.getChild(i);

                bits.or(collectAttributes(node1, attributeNames));
            }

            for (int i=1; i<bits.length(); i=i+2) {
                bits.set(i, false);
            }
            if (node.isFake()) {
                int index = getTaxonIndex(node.getDirectAncestorChild());
                bits.set(2 * index + 1);
            }
        }

        collectAttributesForClade(bits, node, attributeNames);

        return bits;
    }

    private void collectAttributesForClade(BitSet bits, Node node, Set<String> attributeNames) {
        Clade clade = cladeMap.get(bits);
        if (clade != null) {

            if (clade.attributeValues == null) {
                clade.attributeValues = new ArrayList<>();
            }

            int i = 0;
            Object[] values = new Object[attributeNames.size()];
            for (String attributeName : attributeNames) {

                Object value;
                switch (attributeName) {
                    case "height":
                        value = node.getHeight();
                        break;
                    case "length":
                        value = getBranchLength(node);
                        break;
                    default:
                        value = node.getMetaData(attributeName);
                        if (value instanceof String && ((String) value).startsWith("\"")) {
                            value = ((String) value).replaceAll("\"", "");
                        }
                        break;
                }

                values[i] = value;

                i++;
            }
            clade.attributeValues.add(values);

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
            bits.set(2*index);
        } else {

            BitSet bits2 = new BitSet();
            for (int i = 0; i < node.getChildCount(); i++) {

                Node node1 = node.getChild(i);

                sum += getSumCladeCredibility(node1, bits2);
            }

            for (int i=1; i<bits2.length(); i=i+2) {
                bits2.set(i, false);
            }

            if (node.isFake()) {
                int index = getTaxonIndex(node.getDirectAncestorChild());
                bits2.set(2 * index + 1);
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
            bits.set(2*index);
        } else {

            BitSet bits2 = new BitSet();
            for (int i = 0; i < node.getChildCount(); i++) {

                Node node1 = node.getChild(i);

                logCladeCredibility += getLogCladeCredibility(node1, bits2);
            }

            for (int i=1; i<bits2.length(); i=i+2) {
                bits2.set(i, false);
            }

            if (node.isFake()) {
                int index = getTaxonIndex(node.getDirectAncestorChild());
                bits2.set(2 * index + 1);
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
            bits.set(2*index);

            if (includeTips) {
                removeClade(bits);
            }

        } else {

            for (int i = 0; i < node.getChildCount(); i++) {

                Node node1 = node.getChild(i);

                bits.or(removeClades(node1, includeTips));
            }

            for (int i=1; i<bits.length(); i=i+2) {
                bits.set(i, false);
            }
            if (node.isFake()) {
                int index = getTaxonIndex(node.getDirectAncestorChild());
                bits.set(2 * index + 1);
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

        public List<Object[]> getAttributeValues() {
            return attributeValues;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final Clade clade = (Clade) o;

            return !(bits != null ? !bits.equals(clade.bits) : clade.bits != null);

        }

        @Override
        public int hashCode() {
            return (bits != null ? bits.hashCode() : 0);
        }

        @Override
        public String toString() {
            return "clade " + bits.toString();
        }

        int count;
        double credibility;
        BitSet bits;
        List<Object[]> attributeValues = null;
    }


}
