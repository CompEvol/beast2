package beast.evolution.tree;



import java.io.PrintStream;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.util.Log;
import beast.core.Loggable;


@Description("Logger to report statistics of a tree")
public class TreeStatLogger extends CalculationNode implements Loggable, Function {
    final public Input<Tree> treeInput = new Input<>("tree", "tree to report height for.", Validate.REQUIRED);
    @Deprecated
    final public Input<Boolean> logHeigthInput = new Input<>("logHeigth", "If true, tree height will be logged (unless logHeight input = false).", true);
    final public Input<Boolean> logHeightInput = new Input<>("logHeight", "If true, tree height will be logged.", true);
    final public Input<Boolean> logLengthInput = new Input<>("logLength", "If true, tree length will be logged.", true);

    boolean logHeight, logLength;

    @Override
    public void initAndValidate() {

        // This confusing line is because the default situation is to log
        // the tree height: we want the height to be logged only if neither of
        // these defaults is altered.
        logHeight = logHeightInput.get() && logHeigthInput.get();
        logLength = logLengthInput.get();

    	if (!logHeight && !logLength) {
    		Log.warning.println("TreeStatLogger " + getID() + "logs nothing. Set logHeight=true or logLength=true to log at least something");
    	}
    }

    @Override
    public void init(PrintStream out) {
        final Tree tree = treeInput.get();
        if (logHeight) {
            out.print(tree.getID() + ".height\t");
        }
        if (logLength) {
            out.print(tree.getID() + ".treeLength\t");
        }
    }

    @Override
    public void log(long sample, PrintStream out) {
        final Tree tree = treeInput.get();
        if (logHeight) {
        	out.print(tree.getRoot().getHeight() + "\t");
        }
        if (logLength) {
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
        return 2;
    }

    @Override
    public double getArrayValue() {
        return treeInput.get().getRoot().getHeight();
    }

    @Override
    public double getArrayValue(int dim) {
    	if (dim == 0) {
    		return treeInput.get().getRoot().getHeight();
    	}
    	return getLength(treeInput.get());
    }
}
