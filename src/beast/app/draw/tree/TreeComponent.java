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
    double y = 0;

    // text offset
    double offset;

    // x offset
    double xOffset;

    // y offset
    double yOffset;


    //String newick;
    //String options = "ultra thick";
    //ScaleBar scalebar;
    NumberFormat format = NumberFormat.getInstance();

    boolean triangle = true;

    /**
     * The scaling of the node heights
     */
    double xScale;

    /**
     * The scaling of the spacing between the nodes (The number of pixels between adjacent tip nodes)
     */
    double yScale;

    public TreeComponent(Tree tree, double nodeHeightScale, double nodeSpacing, double offset, boolean triangle) {

        format.setMaximumFractionDigits(5);

        //this.scalebar = scalebar;
        this.triangle = triangle;

        this.offset = offset;
        this.xScale = nodeHeightScale;
        this.yScale = nodeSpacing;

        this.tree = tree;
    }

    double startingY() {
        return yOffset;
    }

    void setTipValues(Node node) {
        if (node.isLeaf()) {
            node.setMetaData("y", y);
            node.setMetaData("ymin", y);
            node.setMetaData("ymax", y);
            y += yScale;
        } else {

            double ymin = Double.MAX_VALUE;
            double ymax = Double.MIN_VALUE;
            for (Node childNode : node.getChildren()) {
                setTipValues(childNode);

                double cymin = childNode.getMetaData("ymin");
                double cymax = childNode.getMetaData("ymax");

                if (cymin < ymin) ymin = cymin;
                if (cymax > ymax) ymax = cymax;
            }
            node.setMetaData("ymin", ymin);
            node.setMetaData("ymax", ymax);
        }
    }

//    void drawScaleBar(Tree tree, StringBuilder builder) {
//
//        double sby = y;// - yScale - 1;
//        double width = scalebar.size * xScale;
//
//        double sbx1 = (tree.getHeight(tree.getRootNode()) * xScale + -width) / 2.0;
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
            g.drawString(label, (float) (x + xOffset), (float) (y + yOffset));
        }
    }

    void draw(double x1, double y1, double x2, double y2, Graphics2D g) {
        x1 += xOffset;
        y1 += yOffset;
        x2 += xOffset;
        y2 += yOffset;

        g.draw(new Line2D.Double(x1, y1, x2, y2));
        //builder.append("\\draw (" + x1 + ", " + y1 + ") -- (" + x2 + ", " + y2 + ");\n");
    }

    void drawBranch(Tree tree, Node node, Node childNode, Graphics2D g) {

        double height = Math.round(node.getHeight() * xScale * 1000000.0) / 1000000.0;
        double childHeight = Math.round(childNode.getHeight() * xScale * 1000000.0) / 1000000.0;

        draw(childHeight, childNode.getMetaData("y"), height, node.getMetaData("y"), g);
    }

    void drawLabel(Tree tree, Node node, Graphics2D g) {

        double height = Math.round(node.getHeight() * xScale * 1000000.0) / 1000000.0;

        label(height + offset, y, node.getID(), g);
    }

    void draw(Tree tree, Node node, Graphics2D g) {

        y = startingY();

        if (node.isRoot()) {
            setTipValues(node);
        }

        if (node.isLeaf()) {
            drawLabel(tree, node, g);
        } else {

            double py = 0;
            if (triangle) {
                if (node.isRoot()) {

                    int tipCount = tree.getLeafNodeCount();

                    py = ((tipCount - 1) * yScale) / 2.0;
                } else {

                    Node parent = node.getParent();

                    double ppy = parent.getMetaData("y");
                    double ph = parent.getHeight();
                    double h = node.getHeight();

                    double ymin = node.getMetaData("ymin");
                    double ymax = node.getMetaData("ymax");

                    double yminDist = Math.abs(ppy - ymin);
                    double ymaxDist = Math.abs(ppy - ymax);

                    if (yminDist > ymaxDist) {
                        py = ((ppy * h) + (ymin * (ph - h))) / ph;
                    } else {
                        py = ((ppy * h) + (ymax * (ph - h))) / ph;
                    }
                }
                node.setMetaData("y", py);
            }

            int count = 0;
            for (Node childNode : node.getChildren()) {
                draw(tree, childNode, g);
                py += childNode.getMetaData("y");
                count += 1;
            }
            py /= count;
            if (!triangle) node.setMetaData("y", py);

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

    public void setOffset(double xOffset, double yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public static void main(String[] args) throws Exception {

        String newickTree = "((((1:0.1,2:0.2):0.12,3:0.3):0.123,4:0.4):0.1234,5:0.5);";
        //String newickTree2 = "((((A: 1, B: 1): 1, C: 2): 1, D: 3): 1, E: 4);";

        double nodeHeightScale = 500;
        double nodeSpacing = 100;
        double offset = 5;

        TreeComponent treeComponent = new VerticalTreeComponent(new TreeParser(newickTree), nodeHeightScale, nodeSpacing, offset, true);
        //treeComponent.setOffset(0,500);


        JFrame frame = new JFrame("TreeComponent");
        frame.getContentPane().add(treeComponent, BorderLayout.CENTER);
        frame.setSize(new Dimension(800, 600));
        frame.setVisible(true);
    }
}

