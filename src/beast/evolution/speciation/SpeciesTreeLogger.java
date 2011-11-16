package beast.evolution.speciation;

import java.io.PrintStream;

import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.Plugin;
import beast.core.Input.Validate;
import beast.core.StateNode;
import beast.core.Valuable;
import beast.evolution.speciation.SpeciesTreePrior.PopSizeFunction;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

@Description("Logs tree annotated with metadata in starBeast format")
public class SpeciesTreeLogger extends Plugin implements Loggable {
	public Input<Tree> m_tree = new Input<Tree>("tree","tree to be logged",Validate.REQUIRED);
	public Input<Valuable> m_parameter = new Input<Valuable>("popSize","population size parameter associated with tree nodes",Validate.REQUIRED);
	public Input<Valuable> m_parameterTop = new Input<Valuable>("popSizeTop","population size parameter associated with top of tree branches, only used for non-constant *beast analysis");
	public Input<SpeciesTreePrior> speciesTreePrior = new Input<SpeciesTreePrior>("speciesTreePrior", "species tree prior, used to find which Population Size Function is used. If not specified, assumes 'constant'");
	public Input<TreeTopFinder> treeTopFinder = new Input<TreeTopFinder>("treetop","calculates height of species tree", Validate.REQUIRED);

	PopSizeFunction popSizeFunction;
	String m_sMetaDataLabel;
	
	@Override
	public void initAndValidate() {
		m_sMetaDataLabel = "[&dmv=";
		if (speciesTreePrior.get() != null) {
			popSizeFunction = speciesTreePrior.get().m_popFunctionInput.get();
		} else {
			popSizeFunction = PopSizeFunction.constant;
		}
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
        Valuable metadataTop = m_parameterTop.get();
        if (metadataTop != null && metadataTop instanceof StateNode) {
        	metadataTop = ((StateNode) metadataTop).getCurrent();
        }
        
        // write out the log tree with meta data
        out.print("tree STATE_" + nSample + " = ");
		tree.getRoot().sort();
		out.print(toNewick(tree.getRoot(), metadata, metadataTop));
        //out.print(tree.getRoot().toShortNewick(false));
        out.print(";");
	}

	
	String toNewick(Node node, Valuable metadata, Valuable metadataTop) {
		StringBuffer buf = new StringBuffer();
		if (node.m_left != null) {
			buf.append("(");
			buf.append(toNewick(node.m_left, metadata, metadataTop));
			if (node.m_right != null) {
				buf.append(',');
				buf.append(toNewick(node.m_right, metadata, metadataTop));
			}
			buf.append(")");
		} else {
			buf.append(node.getNr());
		}
		buf.append("[&dmt=");
		if (node.isRoot()) {
		    buf.append(treeTopFinder.get().getHighestTreeHeight() - node.getHeight());
		} else {
		    buf.append(node.getLength());
		}
		buf.append(",dmv=");
		switch (popSizeFunction) {
		case constant:
			buf.append("{" + metadata.getArrayValue(node.getNr()) + "}");
			break;
		case linear:
			if (node.isLeaf()) {
				buf.append("{" + metadata.getArrayValue(node.getNr()));
			} else {
				buf.append("{" + (metadataTop.getArrayValue(node.m_left.getNr()) + metadataTop.getArrayValue(node.m_right.getNr())));
			}
			buf.append("," + getMetaDataTopValue(node, metadataTop) + "}");
			break;
		case linear_with_constant_root:
			if (node.isLeaf()) {
				buf.append("{" + metadata.getArrayValue(node.getNr()));
			} else {
				buf.append("{" + (getMetaDataTopValue(node.m_left, metadataTop) + getMetaDataTopValue(node.m_right, metadataTop)));
			}
			if (node.isRoot()) {
				buf.append("," + (getMetaDataTopValue(node.m_left, metadataTop) + getMetaDataTopValue(node.m_right, metadataTop)) + "}");
			} else {
				buf.append("," + getMetaDataTopValue(node, metadataTop) + "}");
			}
			break;
		}
		buf.append(']');
		if (!node.isRoot()) {
		    buf.append(":").append(node.getLength());
		}
		return buf.toString();
	}
	
	double getMetaDataTopValue(Node node, Valuable metadataTop) {
		if (node.getNr() < metadataTop.getDimension()) {
			return metadataTop.getArrayValue(node.getNr());
		} else {
			Node root = node.getTree().getRoot();
			return metadataTop.getArrayValue(root.getNr());
		}
	}
	
	@Override
	public void close(PrintStream out) {
		m_tree.get().close(out);
	}

}
