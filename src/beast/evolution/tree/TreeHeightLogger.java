package beast.evolution.tree;

import java.io.PrintStream;

import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.Plugin;
import beast.core.State;
import beast.core.Input.Validate;

@Description("Logger to report height of a tree")
public class TreeHeightLogger extends Plugin implements Loggable {
	public Input<Tree> m_tree = new Input<Tree>("tree", "tree to report height for.", Validate.REQUIRED);

	@Override
	public void initAndValidate() {
		// nothing to do
	}

	@Override
	public void init(State state, PrintStream out) throws Exception {
		Tree tree = m_tree.get();
		out.print(tree.getID() + ".height\t");
	}

	@Override
	public void log(int nSample, State state, PrintStream out) {
		Tree tree = m_tree.get();
		out.print(tree.getRoot().getHeight() + "\t");
	}

	@Override
	public void close(PrintStream out) {
		// nothing to do
	}
}
