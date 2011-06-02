package beast.evolution.speciation;

import java.io.PrintStream;

import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.Plugin;
import beast.core.Input.Validate;
import beast.core.StateNode;
import beast.core.Valuable;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

@Description("Logs tree annotated with metadata in starBeast format")
public class SpeciesTreeLogger extends Plugin implements Loggable {
	public Input<Tree> m_tree = new Input<Tree>("tree","tree to be logged",Validate.REQUIRED);
	public Input<Valuable> m_parameter = new Input<Valuable>("popSize","population size parameter associated with tree nodes",Validate.REQUIRED);
	

	String m_sMetaDataLabel;
	
	@Override
	public void initAndValidate() {
		m_sMetaDataLabel = "[&dmv=";
	}
	
	@Override
	public void init(PrintStream out) throws Exception {
		m_tree.get().init(out);
	}

	@Override
	public void log(int nSample, PrintStream out) {
		// make sure we get the current version of the inputs
        Tree tree = (Tree) m_tree.get().getCurrent();
        Valuable metadata = m_parameter.get();
        if (metadata instanceof StateNode) {
        	metadata = ((StateNode) metadata).getCurrent();
        }
        // write out the log tree with meta data
        out.print("tree STATE_" + nSample + " = ");
		tree.getRoot().sort();
		out.print(toNewick(tree.getRoot(), metadata));
        //out.print(tree.getRoot().toShortNewick(false));
        out.print(";");
	}

	
	String toNewick(Node node, Valuable metadata) {
		StringBuffer buf = new StringBuffer();
		if (node.m_left != null) {
			buf.append("(");
			buf.append(toNewick(node.m_left, metadata));
			if (node.m_right != null) {
				buf.append(',');
				buf.append(toNewick(node.m_right, metadata));
			}
			buf.append(")");
		} else {
			buf.append(node.getNr());
		}
		buf.append(m_sMetaDataLabel);
		buf.append("{"+metadata.getArrayValue(node.getNr()) + "}");
		buf.append(']');
	    buf.append(":").append(node.getLength());
		return buf.toString();
	}
	
	
	@Override
	public void close(PrintStream out) {
		m_tree.get().close(out);
	}

}
