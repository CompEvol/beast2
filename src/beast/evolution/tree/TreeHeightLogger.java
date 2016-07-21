package beast.evolution.tree;



import java.io.PrintStream;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Loggable;


@Description("Logger to report height of a tree")
public class TreeHeightLogger extends CalculationNode implements Loggable, Function {
    final public Input<Tree> treeInput = new Input<>("tree", "tree to report height for.", Validate.REQUIRED);
    final public Input<Boolean> logLengthInput = new Input<>("logLength", "If true, tree length will be logged as well.", false);

    @Override
    public void initAndValidate() {
        // nothing to do
    }

    @Override
    public void init(PrintStream out) {
        final Tree tree = treeInput.get();
        if (getID() == null || getID().matches("\\s*")) {
            out.print(tree.getID() + ".height\t");
        } else {
            out.print(getID() + "\t");
        }
        if (logLengthInput.get()) {
            out.print(tree.getID() + ".treeLength\t");
        }
    }

    @Override
    public void log(int sample, PrintStream out) {
        final Tree tree = treeInput.get();
        out.print(tree.getRoot().getHeight() + "\t");
        if (logLengthInput.get()) {
            out.print(getLength(tree) + "\t");
        }
    }

    private double getLength(Tree tree) {
    	double length = 0;
    	for (Node node : tree.getNodesAsArray()) {
    		if (!node.isRoot()) {
    			length += node.getLength();
    		}
    	}
		return length;
	}

	@Override
    public void close(PrintStream out) {
        // nothing to do
    }

    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public double getArrayValue() {
        return treeInput.get().getRoot().getHeight();
    }

    @Override
    public double getArrayValue(int dim) {
        return treeInput.get().getRoot().getHeight();
    }
}
