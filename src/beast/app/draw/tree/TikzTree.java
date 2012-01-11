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
import java.io.PrintStream;

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
    public Input<String> pdflatexPath = new Input<String>("pdflatexPath", "the path to pdflatex; if provided then will be run automatically", "");

    public void initAndValidate() {
    }

    public void run() throws IOException, InterruptedException {
        TreeComponent treeComponent = new SquareTreeComponent(tree.get(), labelOffset.get(), showInternodeIntervals.get());
        treeComponent.setLineThickness(lineThickness.get());
        treeComponent.setSize(new Dimension(width.get(), height.get()));

        String fileName = this.fileName.get();
        TikzGraphics2D tikzGraphics2D;

        PrintStream out = System.out;


        //if (fileName != "") {
        out = new PrintStream(new FileOutputStream(fileName));
        out.println("\\documentclass[12pt]{article}");
        out.println("\\usepackage{tikz,pgf}");
        out.println("\\begin{document}");
        // }

        tikzGraphics2D = new TikzGraphics2D(out);
        treeComponent.paint(tikzGraphics2D);
        tikzGraphics2D.flush();

        out.println("\\end{document}");
        out.flush();
        if (out != System.out) {
            
            
            out.close();
            
            String pdflatexPathString = pdflatexPath.get();
            if (!pdflatexPathString.equals("")) {
                String pdfFileName = fileName.substring(0,fileName.length()-3) + "pdf";
            
                Process p = Runtime.getRuntime().exec(pdflatexPathString + " " + fileName);
                p.waitFor();
                Process p2 = Runtime.getRuntime().exec("open " + pdfFileName);
                p2.waitFor();
            }
        }
    }
}
