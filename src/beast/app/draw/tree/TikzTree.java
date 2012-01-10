package beast.app.draw.tree;

import beast.core.Description;
import beast.core.Input;
import beast.core.Runnable;
import beast.evolution.tree.Tree;
import org.jtikz.TikzGraphics2D;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Alexei Drummond
 */
@Description("Generates the tree figure from input tree into Tikz/PGF format for addition to LaTeX document.")
public class TikzTree extends Runnable {

    public Input<Tree> tree = new Input<Tree>("tree", "phylogenetic tree with taxa data in the leafs", Input.Validate.REQUIRED);
    public Input<Double> lineThickness = new Input<Double>("lineThickness", "indicates the thickness of the lines", 1.0);
    public Input<Double> labelOffset = new Input<Double>("labelOffset", "indicates the distance from leaf node to its label in pts", 5.0);
    public Input<Integer> width = new Input<Integer>("width", "the width of the figure in pts", 100);
    public Input<Integer> height = new Input<Integer>("height", "the height of the figure in pts", 100);
    public Input<String> fileName = new Input<String>("fileName", "the name of the file to write Tikz code to", "");
    public Input<Boolean> showLabels = new Input<Boolean>("showLabels", "if true then the taxa labels are displayed", true);
    public Input<Boolean> showInternodeIntervals = new Input<Boolean>("showInternodeIntervals", "if true then dotted lines at each internal node height are displayed", true);

    public void initAndValidate() {
    }

    public void run() throws IOException {
        TreeComponent treeComponent = new SquareTreeComponent(tree.get(), labelOffset.get(), showInternodeIntervals.get());
        treeComponent.setLineThickness(lineThickness.get());
        treeComponent.setSize(new Dimension(width.get(), height.get()));

        String fileName = this.fileName.get();
        TikzGraphics2D tikzGraphics2D;

        OutputStream out = System.out;


        if (fileName != "") {
            out = new FileOutputStream(fileName);
        }

        tikzGraphics2D = new TikzGraphics2D(out);
        treeComponent.paint(tikzGraphics2D);
        tikzGraphics2D.flush();
        out.flush();
        if (out != System.out) out.close();
    }
}
