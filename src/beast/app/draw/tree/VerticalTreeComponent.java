package beast.app.draw.tree;

import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

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

    @Override
    double getScaledTreeHeight() {
        return 0.9 * getHeight();
    }

    /**
     * The total amount of space for nodes (The number of pixels between adjacent leaf nodes * the number of leaf nodes)
     */
    double getTotalSizeForNodeSpacing() {
        return getWidth();
    }

    @Override
    void drawBranch(Tree tree, Node node, Node childNode, Graphics2D g) {

        double height = getScaledOffsetNodeHeight(node);
        double childHeight = getScaledOffsetNodeHeight(childNode);

        double position = getNodePosition(node);
        double childPosition = getNodePosition(childNode);

        draw(childPosition, childHeight, position, height, g);
    }

    @Override
    void drawLabel(Tree tree, Node node, Graphics2D g) {

        if (showLabels) {
            label(getNodePosition(node), getScaledOffsetNodeHeight(node) + labelOffset, node.getID(), g);
        }
    }

    static String ladderTree(int size) {

        String tree = "(1:1, 2:1)";

        for (int i = 2; i < size; i++) {
            tree = "(" + tree + ":1," + (i + 1) + ":" + i + ")";
        }

        return tree + ";";
    }
}
