package beast.app.draw.tree;

import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.TreeParser;

import java.awt.*;

/**
 * @author Alexei Drummond
 */
public class VerticalTreeComponent extends TreeComponent {

    boolean showLabels = true;

    public VerticalTreeComponent(Tree tree,
                                 double offset, boolean showLabels) {

        super(tree, 0, 0, offset, true);

        this.showLabels = showLabels;
    }


    public VerticalTreeComponent(Tree tree, double nodeHeightScale, double nodeSpacingScale,
                                 double offset, boolean showLabels) {

        super(tree, nodeHeightScale, nodeSpacingScale, offset, true);

        this.showLabels = showLabels;
    }

    /**
     * @return the distance from edge of component to root when traveling towards the leaves
     */
    double rootOffset() {
        return 0.05 * getHeight();
    }

    double getNodeHeightScale() {
        if (nhs == 0) return 0.9 * getHeight() / tree.getRoot().getHeight();
        return nhs;
    }

    /**
     * The spacing between the nodes (The number of pixels between adjacent leaf nodes)
     */
    double getNodeSpacing() {
        if (ns == 0) return getWidth() / tree.getLeafNodeCount();
        return ns;
    }


    @Override
    void drawBranch(Tree tree, Node node, Node childNode, Graphics2D g) {

        double height = getHeight() - node.getHeight() * getNodeHeightScale() - rootOffset();
        double childHeight = getHeight() - childNode.getHeight() * getNodeHeightScale() - rootOffset();

        draw(childNode.getMetaData("p"), childHeight, node.getMetaData("p"), height, g);
    }

    @Override
    void drawLabel(Tree tree, Node node, Graphics2D g) {

        if (showLabels) {
            double height = Math.round(node.getHeight() * getNodeHeightScale() * 1000000.0) / 1000000.0;

            label(node.getMetaData("p"), height + offset, node.getID(), g);
        }
    }

    static String ladderTree(int size) {

        String tree = "(1:1, 2:1)";

        for (int i = 2; i < size; i++) {
            tree = "(" + tree + ":1," + (i + 1) + ":" + i + ")";
        }

        return tree + ";";
    }

    public static void main(String[] args) throws Exception {

        String tree1 = "((1:1,2:1):1,(3:1,4:1):1);";

        int size = 4;
        String tree2 = ladderTree(size);

        for (int i = 0; i < 3; i++) {
            TreeComponent treeComponent1 = new VerticalTreeComponent(new TreeParser(tree1), 0.666, 0.444, -0.2, true);
            //treeComponent1.paint(false, builder);

            TreeComponent treeComponent2 = new VerticalTreeComponent(new TreeParser(tree2), -(4.0 / 3.0) / (size - 1.0), (4.0 / 3.0) / (size - 1.0), +0.2, false);
            //tikzTree2.generateTikzPicture(false, builder);
        }
    }

}
