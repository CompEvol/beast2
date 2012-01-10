package beast.app.draw.tree;

import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.TreeParser;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;

/**
 * @author Alexei Drummond
 */
public class SquareTreeComponent extends TreeComponent {


    public SquareTreeComponent(Tree tree, double labelOffset, boolean showInternodeIntervals) {

        super(tree, labelOffset, false);
        this.showInternodeIntervals = showInternodeIntervals;
    }

    public SquareTreeComponent(Tree tree, double nodeHeightScale, double nodeSpacing,
                               double labelOffset,
                               boolean showInternodeIntervals) {

        super(tree, nodeHeightScale, nodeSpacing, labelOffset, false, showInternodeIntervals);
    }

    void drawBranch(Tree tree, Node node, Node childNode, Graphics2D g) {

        double height = getScaledOffsetNodeHeight(node);
        double childHeight = getScaledOffsetNodeHeight(childNode);

        double pos = getNodePosition(node);
        double childPos = getNodePosition(childNode);

        Path2D path = new GeneralPath();
        path.moveTo(childHeight, childPos);
        path.lineTo(height, childPos);
        path.lineTo(height, pos);

        g.draw(path);

        //g.draw(new Line2D.Double(childHeight, childPos, height, childPos));
        //g.draw(new Line2D.Double(childHeight, childPos, height, childPos));

//        builder.append("\\draw (");
//        builder.append(childHeight);
//        builder.append(", ");
//        builder.append(childNode.getAttribute("y"));
//        builder.append(") -- (");
//        builder.append(height);
//        builder.append(", ");
//        builder.append(childNode.getAttribute("y"));
//        builder.append(") -- (");
//        builder.append(height);
//        builder.append(", ");
//        builder.append(node.getAttribute("y"));
//        builder.append(");\n");

    }

    public static void main(String[] args) throws Exception {

        String newickTree = "((((1:0.1,2:0.1):0.1,3:0.2):0.1,4:0.3):0.1,5:0.4);";

        double labelOffset = 5;

        TreeComponent treeComponent = new SquareTreeComponent(new TreeParser(newickTree), labelOffset, false);

        JFrame frame = new JFrame("SquareTreeComponent");
        frame.getContentPane().add(treeComponent, BorderLayout.CENTER);
        frame.setSize(new Dimension(800, 600));
        frame.setVisible(true);
    }


}
