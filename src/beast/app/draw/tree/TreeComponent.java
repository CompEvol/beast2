package beast.app.draw.tree;

import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.Sequence;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.TreeParser;
import org.jtikz.TikzGraphics2D;
import org.jtikz.TikzRenderingHints;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Alexei Drummond
 */
public class TreeComponent extends JComponent {

    Tree tree;

    // the position of the "current" leaf node
    private double p = 0;

    // offset of leaf node labels from leaf node position
    double labelOffset;

    //String newick;
    //String options = "ultra thick";
    //ScaleBar scalebar;
    NumberFormat format = NumberFormat.getInstance();

    boolean isTriangle = true;

    boolean showInternodeIntervals = false;

    private Boolean showLeafLabels = true;

    String branchLabels = "";

    /**
     * The scaling of the node heights. If the scale is 0 then scale is automatically calculated from component size
     */
    double nhs = 0;

    /**
     * The scaling of the spacing between the nodes (The number of pixels between adjacent tip nodes). If the spacing is 0 then it is automatically calculated from component size
     */
    double ns = 0;

    double lineThickness = 1.0;
    
    Set<Double> internodeIntervals = null;

    /**
     * @param tree        the tree to draw
     * @param labelOffset the pixel labelOffset of labels from leaf nodes
     * @param isTriangle  true if tree should be drawn so that outside branches form a triangle on ultrametric trees
     */
    public TreeComponent(Tree tree, double labelOffset, boolean isTriangle) {

        this(tree, 0, 0, labelOffset, isTriangle, false);

    }

    public TreeComponent(Tree tree, double nodeHeightScale, double nodeSpacing, double labelOffset, boolean isTriangle, boolean showInternodeIntervals) {

        format.setMaximumFractionDigits(5);

        //this.scalebar = scalebar;
        this.isTriangle = isTriangle;
        this.showInternodeIntervals = showInternodeIntervals;
        if (showInternodeIntervals) {
            internodeIntervals = new TreeSet<Double>();
        }

        this.labelOffset = labelOffset;
        this.nhs = nodeHeightScale;
        this.ns = nodeSpacing;

        this.tree = tree;
    }

    /**
     * @param node the node to return scaled labelOffset height of
     * @return the position of this node in root-to-tip direction once scaled and labelOffset for component
     */
    double getScaledOffsetNodeHeight(Node node) {
        return getScaledTreeHeight() - node.getHeight() * getNodeHeightScale() + rootOffset();
    }

    /**
     * @param node the node
     * @return the position of this node in perpendicular to root-to-tip direction once scaled and labelOffset
     */
    double getNodePosition(Node node) {
        return (Double)node.getMetaData("p");
    }

    /**
     * @return the distance from the edge of component to the first leaf when traveling across the leaves
     */
    double firstLeafNodePosition() {
        return getNodeSpacing() / 2;
    }

    /**
     * @return the distance from edge of component to root when traveling towards the leaves
     */
    final double rootOffset() {
        return 0.05 * getScaledTreeHeight();
    }

    final double getNodeHeightScale() {
        if (nhs == 0) return getScaledTreeHeight() / tree.getRoot().getHeight();
        return nhs;
    }

    double getScaledTreeHeight() {
        return 0.9 * getWidth();
    }


    double getTotalSizeForNodeSpacing() {
        return getHeight();
    }

    /**
     * The spacing between the nodes (The number of pixels between adjacent leaf nodes)
     */
    final double getNodeSpacing() {
        if (ns == 0) return getTotalSizeForNodeSpacing() / tree.getLeafNodeCount();
        return ns;
    }

    void setTipValues(Node node) {
        if (node.isLeaf()) {
            node.setMetaData("p", p);
            node.setMetaData("pmin", p);
            node.setMetaData("pmax", p);
            p += getNodeSpacing();
        } else {

            double pmin = Double.MAX_VALUE;
            double pmax = Double.MIN_VALUE;
            for (Node childNode : node.getChildren()) {
                setTipValues(childNode);

                double cpmin = (Double)childNode.getMetaData("pmin");
                double cpmax = (Double)childNode.getMetaData("pmax");

                if (cpmin < pmin) pmin = cpmin;
                if (cpmax > pmax) pmax = cpmax;
            }
            node.setMetaData("pmin", pmin);
            node.setMetaData("pmax", pmax);
        }
    }

//    void drawScaleBar(Tree tree, StringBuilder builder) {
//
//        double sby = y;// - nodeSpacing - 1;
//        double width = scalebar.size * nodeHeightScale;
//
//        double sbx1 = (tree.getHeight(tree.getRootNode()) * nodeHeightScale + -width) / 2.0;
//        double sbx2 = sbx1 + width;
//
//        double sbx3 = (sbx1 + sbx2) / 2;
//
//        draw(sbx1, sby, sbx2, sby, builder);
//
//        label(sbx3, sby + labelOffset, "" + scalebar.size, builder);
//    }

    void label(double x, double y, String label, Graphics2D g) {

        if (label != null) {
            Object oldHintValue = g.getRenderingHint(TikzRenderingHints.KEY_NODE_ANCHOR);
            g.setRenderingHint(TikzRenderingHints.KEY_NODE_ANCHOR,TikzRenderingHints.VALUE_CENTER);
            g.drawString(label, (float) x, (float) y);
            if (oldHintValue != null) g.setRenderingHint(TikzRenderingHints.KEY_NODE_ANCHOR, oldHintValue);
        }
    }

    void draw(double x1, double y1, double x2, double y2, Graphics2D g) {

        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }
    
    void drawNode(String label, double x, double y, Object anchor, double fontSize, Graphics2D g) {
        Object oldHintValue = g.getRenderingHint(TikzRenderingHints.KEY_NODE_ANCHOR);
        g.setRenderingHint(TikzRenderingHints.KEY_NODE_ANCHOR, anchor);
        Font oldFont = g.getFont();
        g.setFont(oldFont.deriveFont((float) fontSize));
        g.drawString(label, (float) x, (float) y);
        if (oldHintValue != null) g.setRenderingHint(TikzRenderingHints.KEY_NODE_ANCHOR, oldHintValue);
        g.setFont(oldFont);
    }

    void drawBranch(Tree tree, Node node, Node childNode, Graphics2D g) {

        double height = getScaledOffsetNodeHeight(node);
        double childHeight = getScaledOffsetNodeHeight(childNode);

        double position = getNodePosition(node);
        double childPosition = getNodePosition(childNode);

        if (branchLabels != null && !branchLabels.equals("")) {
            Object metaData = childNode.getMetaData(branchLabels);
            String branchLabel;
            if (metaData instanceof Number) {
                branchLabel = format.format(metaData);
            } else {
                branchLabel = metaData.toString();
            }
            drawNode(branchLabel,(height+childHeight)/2, (position+childPosition)/2, TikzRenderingHints.VALUE_SOUTH, 9.0, g);
        }
        draw(height, position, childHeight, childPosition, g);
    }

    void drawLabel(Tree tree, Node node, Graphics2D g) {

        double height = getScaledOffsetNodeHeight(node);
        double position = getNodePosition(node);

        label(height + labelOffset, position, node.getID(), g);
    }

    void draw(Tree tree, Node node, Graphics2D g) {

        g.setStroke(new BasicStroke((float) lineThickness));

        p = firstLeafNodePosition();

        if (showInternodeIntervals) {
            drawInternodeInterval(node, g);
        }
        
        if (node.isRoot()) {
            setTipValues(node);
        }

        if (node.isLeaf() && showLeafLabels) {
            drawLabel(tree, node, g);
        } else {

            double cp = 0;
            if (isTriangle) {
                if (node.isRoot()) {

                    int tipCount = tree.getLeafNodeCount();

                    cp = ((tipCount - 1) * getNodeSpacing()) / 2.0 + firstLeafNodePosition();
                } else {

                    Node parent = node.getParent();

                    double pp = (Double)parent.getMetaData("p");
                    double ph = parent.getHeight();
                    double h = node.getHeight();

                    double pmin = (Double)node.getMetaData("pmin");
                    double pmax = (Double)node.getMetaData("pmax");

                    double pminDist = Math.abs(pp - pmin);
                    double pmaxDist = Math.abs(pp - pmax);

                    if (pminDist > pmaxDist) {
                        cp = ((pp * h) + (pmin * (ph - h))) / ph;
                    } else {
                        cp = ((pp * h) + (pmax * (ph - h))) / ph;
                    }
                }
                node.setMetaData("p", cp);
            }

            int count = 0;
            for (Node childNode : node.getChildren()) {
                draw(tree, childNode, g);
                cp += (Double)childNode.getMetaData("p");
                count += 1;
            }
            cp /= count;
            if (!isTriangle) node.setMetaData("p", cp);

            for (Node childNode : node.getChildren()) {

                drawBranch(tree, node, childNode, g);
            }
        }
    }

    final void drawInternodeInterval(Node node, Graphics2D g) {

        if (internodeIntervals == null) internodeIntervals = new TreeSet<Double>();
        if (!internodeIntervals.contains(node.getHeight())) {

            double height = getScaledOffsetNodeHeight(node);
    
            double p1 = firstLeafNodePosition()/2;
            double p2 = getTotalSizeForNodeSpacing()-p1;
    
            Stroke s = g.getStroke();
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10, new float[]{1.0f, 1.0f}, 0));
            drawInternodeInterval(height, p1, p2, g);
            g.setStroke(s);
            internodeIntervals.add(node.getHeight());
        }
    }

    void drawInternodeInterval(double nodeHeight, double p1, double p2, Graphics2D g) {
        draw(nodeHeight, p1, nodeHeight, p2, g);
    }


    public void paintComponent(Graphics g) {

        if (showInternodeIntervals && internodeIntervals != null) {
            internodeIntervals.clear();
        }

        draw(tree, tree.getRoot(), (Graphics2D) g);
    }

    public static void main(String[] args) throws Exception {

        String newickTree = "((((1:0.1,2:0.1):0.1,3:0.2):0.1,4:0.3):0.1,5:0.4);";

        List<Sequence> sequences = new ArrayList<Sequence>();
        sequences.add(new Sequence("A", "A"));
        sequences.add(new Sequence("B", "A"));
        sequences.add(new Sequence("C", "A"));
        sequences.add(new Sequence("D", "A"));
        sequences.add(new Sequence("E", "A"));

        Alignment alignment = new Alignment(sequences, 4, "nucleotide");

        double labelOffset = 5;

        TreeComponent treeComponent = new SquareTreeComponent(new TreeParser(alignment, newickTree), labelOffset, false);

        TikzGraphics2D tikzGraphics2D = new TikzGraphics2D();
        treeComponent.setSize(new Dimension(100, 100));
        treeComponent.paintComponent(tikzGraphics2D);
        tikzGraphics2D.flush();

        //System.out.println(tikzGraphics2D.toString());
        //tikzGraphics2D.paintComponent(treeComponent);

//        JFrame frame = new JFrame("TreeComponent");
//        frame.getContentPane().add(treeComponent, BorderLayout.CENTER);
//        frame.setSize(new Dimension(800, 600));
//        frame.setVisible(true);
    }

    public void setLineThickness(double lineThickness) {
        this.lineThickness = lineThickness;
    }

    public void setBranchLabelAttribute(String branchLabels) {
        this.branchLabels = branchLabels;
    }

    public void setShowLeafLabels(Boolean showLeafLabels) {
        this.showLeafLabels = showLeafLabels;
    }
}

