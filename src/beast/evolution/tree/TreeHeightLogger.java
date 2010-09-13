package beast.evolution.tree;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Loggable;
import beast.core.Plugin;

import java.io.PrintStream;

@Description("Logger to report height of a tree")
public class TreeHeightLogger extends Plugin implements Loggable {
	public Input<Tree> m_tree = new Input<Tree>("tree", "tree to report height for.", Validate.REQUIRED);

	@Override
	public void initAndValidate() {
		// nothing to do
	}

	@Override
	public void init(PrintStream out) throws Exception {
		final Tree tree = m_tree.get();
		out.print(tree.getID() + ".height\t");
	}

	@Override
	public void log(int nSample, PrintStream out) {
		final Tree tree = m_tree.get();
		out.print(tree.getRoot().getHeight() + "\t");
	}

	@Override
	public void close(PrintStream out) {
		// nothing to do
	}
}
