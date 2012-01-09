package beast.app.draw.tree;

import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.TreeParser;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.text.NumberFormat;

/**
 * @author Alexei Drummond
 */
public class TreeComponent extends JComponent {

    Tree tree;

    // the position of the "current" leaf node
    private double p = 0;

    // text offset
    double offset;

    //String newick;
    //String options = "ultra thick";
    //ScaleBar scalebar;
    NumberFormat format = NumberFormat.getInstance();

    boolean triangle = true;

    /**
     * The scaling of the node heights. If the scale is 0 then scale is automatically calculated from component size
     */
    double nhs = 0;

    /**
     * The scaling of the spacing between the nodes (The number of pixels between adjacent tip nodes). If the spacing is 0 then it is automatically calculated from component size
     */
    double ns = 0;

    public TreeComponent(Tree tree, double offset, boolean triangle) {

        this(tree, 0, 0, offset, triangle);

    }

    public TreeComponent(Tree tree, double nodeHeightScale, double nodeSpacing, double offset, boolean triangle) {

        format.setMaximumFractionDigits(5);

        //this.scalebar = scalebar;
        this.triangle = triangle;

        this.offset = offset;
        this.nhs = nodeHeightScale;
        this.ns = nodeSpacing;

        this.tree = tree;
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
    double rootOffset() {
        return 0.05 * getWidth();
    }

    double getNodeHeightScale() {
        if (nhs == 0) return 0.9 * getWidth() / tree.getRoot().getHeight();
        return nhs;
    }

    /**
     * The spacing between the nodes (The number of pixels between adjacent leaf nodes)
     */
    double getNodeSpacing() {
        if (ns == 0) return getHeight() / tree.getLeafNodeCount();
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

                double cpmin = childNode.getMetaData("pmin");
                double cpmax = childNode.getMetaData("pmax");

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
//        label(sbx3, sby + offset, "" + scalebar.size, builder);
//    }

    void label(double x, double y, String label, Graphics2D g) {

//        builder.append("\\node at (");
//        builder.append(x+ xOffset);
//        builder.append(", ");
//        builder.append(y + yOffset);
//        builder.append(") {");
//        builder.append(label);
//        builder.append("};\n");
        if (label != null) {
            System.out.println("draw label \"" + label + "\" at " + x + ", " + y);
            g.drawString(label, (float) x, (float) y);
        }
    }

    void draw(double x1, double y1, double x2, double y2, Graphics2D g) {

        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    void drawBranch(Tree tree, Node node, Node childNode, Graphics2D g) {

        double height = getWidth() - node.getHeight() * getNodeHeightScale() - rootOffset();
        double childHeight = getWidth() - childNode.getHeight() * getNodeHeightScale() - rootOffset();

        double position = getHeight() - node.getMetaData("p");
        double childPosition = getHeight() - childNode.getMetaData("p");

        draw(childHeight, childPosition, height, position, g);
    }

    void drawLabel(Tree tree, Node node, Graphics2D g) {

        double height = Math.round(node.getHeight() * getNodeHeightScale() * 1000000.0) / 1000000.0;

        label(height + offset, p, node.getID(), g);
    }

    void draw(Tree tree, Node node, Graphics2D g) {

        p = firstLeafNodePosition();

        if (node.isRoot()) {
            setTipValues(node);
        }

        if (node.isLeaf()) {
            drawLabel(tree, node, g);
        } else {

            double cp = 0;
            if (triangle) {
                if (node.isRoot()) {

                    int tipCount = tree.getLeafNodeCount();

                    cp = ((tipCount - 1) * getNodeSpacing()) / 2.0 + firstLeafNodePosition();
                } else {

                    Node parent = node.getParent();

                    double pp = parent.getMetaData("p");
                    double ph = parent.getHeight();
                    double h = node.getHeight();

                    double pmin = node.getMetaData("pmin");
                    double pmax = node.getMetaData("pmax");

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
                cp += childNode.getMetaData("p");
                count += 1;
            }
            cp /= count;
            if (!triangle) node.setMetaData("p", cp);

            for (Node childNode : node.getChildren()) {

                drawBranch(tree, node, childNode, g);
            }
        }
    }

    public void paintComponent(Graphics g) {

        System.out.println("draw tree " + tree.toString());

        Graphics2D g2d = (Graphics2D) g;

        draw(tree, tree.getRoot(), g2d);
    }

    public static void main(String[] args) throws Exception {

        String newickTree = "((((1:0.1,2:0.1):0.1,3:0.2):0.1,4:0.3):0.1,5:0.4);";
        //String newickTree2 = "((((A: 1, B: 1): 1, C: 2): 1, D: 3): 1, E: 4);";

        double nodeHeightScale = 1000;
        double nodeSpacing = 100;
        double offset = 5;

        TreeComponent treeComponent = new VerticalTreeComponent(new TreeParser(newickTree), offset, true);
        //treeComponent.setOffset(0,500);

        JFrame frame = new JFrame("TreeComponent");
        frame.getContentPane().add(treeComponent, BorderLayout.CENTER);
        frame.setSize(new Dimension(800, 600));
        frame.setVisible(true);
    }
}

