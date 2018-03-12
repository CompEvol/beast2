package beast.evolution.speciation;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Loggable;
import beast.core.StateNode;
import beast.core.parameter.Parameter;
import beast.evolution.speciation.SpeciesTreePrior.TreePopSizeFunction;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;



@Description("Logs tree annotated with metadata in StarBeast format")
public class SpeciesTreeLogger extends BEASTObject implements Loggable {
    final public Input<Tree> treeInput = new Input<>("tree", "tree to be logged", Validate.REQUIRED);
    final public Input<Function> parameterInput = new Input<>("popSize", "population size parameter associated with tree nodes", Validate.REQUIRED);
    final public Input<Function> parameterTopInput = new Input<>("popSizeTop", "population size parameter associated with top of tree branches, only used for non-constant *beast analysis");
    final public Input<SpeciesTreePrior> speciesTreePriorInput = new Input<>("speciesTreePrior", "species tree prior, used to find which Population Size Function is used. If not specified, assumes 'constant'");
    final public Input<TreeTopFinder> treeTopFinderInput = new Input<>("treetop", "calculates height of species tree", Validate.REQUIRED);
    final public Input<List<Function>> metadataInput = new Input<>("metadata", "meta data to be logged with the tree nodes",new ArrayList<>());

    TreePopSizeFunction popSizeFunction;
    String metaDataLabel;

    static final String dmv = "dmv";
    static final String dmt = "dmt";

    @Override
    public void initAndValidate() {
        metaDataLabel = "[&" + dmv + "=";
        if (speciesTreePriorInput.get() != null) {
            popSizeFunction = speciesTreePriorInput.get().popFunctionInput.get();
        } else {
            popSizeFunction = TreePopSizeFunction.constant;
        }
    }

    @Override
    public void init(final PrintStream out) {
        treeInput.get().init(out);
    }

    @Override
    public void log(final long sample, final PrintStream out) {
        // make sure we get the current version of the inputs
        final Tree tree = (Tree) treeInput.get().getCurrent();
        Function metadata = parameterInput.get();
        if (metadata instanceof StateNode) {
            metadata = ((StateNode) metadata).getCurrent();
        }
        Function metadataTop = parameterTopInput.get();
        if (metadataTop != null && metadataTop instanceof StateNode) {
            metadataTop = ((StateNode) metadataTop).getCurrent();
        }

        List<Function> metadataList = metadataInput.get();
        for (int i = 0; i < metadataList.size(); i++) {
        	if (metadataList.get(i) instanceof StateNode) {
        		metadataList.set(i, ((StateNode) metadataList.get(i)).getCurrent());
        	}
        }

        // write out the log tree with meta data
        out.print("tree STATE_" + sample + " = ");
        tree.getRoot().sort();
        out.print(toNewick(tree.getRoot(), metadata, metadataTop, metadataList));
        //out.print(tree.getRoot().toShortNewick(false));
        out.print(";");
    }


    String toNewick(final Node node, final Function metadata, final Function metadataTop, List<Function> metadataList) {
        final StringBuilder buf = new StringBuilder();

        if (node.getLeft() != null) {
            buf.append("(");
            buf.append(toNewick(node.getLeft(), metadata, metadataTop, metadataList));
            if (node.getRight() != null) {
                buf.append(',');
                buf.append(toNewick(node.getRight(), metadata, metadataTop, metadataList));
            }
            buf.append(")");
        } else {
            buf.append(node.getNr()+Tree.taxaTranslationOffset);
        }
        buf.append("[&");
        switch (popSizeFunction) {
            case constant: {
                final double popStart = metadata.getArrayValue(node.getNr());
                buf.append(dmv + "=").append(popStart);
                break;
            }
            case linear:
            case linear_with_constant_root:
                buf.append(dmt + "=");
                final double b;
                if (node.isRoot()) {
                    b = treeTopFinderInput.get().getHighestTreeHeight() - node.getHeight();
                } else {
                    b = node.getLength();
                }
                buf.append(b).append("," + dmv + "={");

                final double popStart;
                if (node.isLeaf()) {
                    popStart = metadata.getArrayValue(node.getNr());
                } else {
                    popStart = (getMetaDataTopValue(node.getLeft(), metadataTop) +
                            getMetaDataTopValue(node.getRight(), metadataTop));
                }
                buf.append(popStart);

                final double popEnd;
                if (node.isRoot() && popSizeFunction == TreePopSizeFunction.linear_with_constant_root) {
                    popEnd = popStart;
                } else {
                  popEnd = getMetaDataTopValue(node, metadataTop);
                }
                buf.append(",").append(popEnd).append("}");
                break;
        }
        if (metadataList.size() > 0) {
        	for (Function metadata2 : metadataList) {
	            if (metadataList.indexOf(metadata2) > 0 || buf.length() > 1) {
	            	buf.append(",");
	            }
	            buf.append(((BEASTObject)metadata2).getID());
	            buf.append('=');
	            if (metadata2 instanceof Parameter<?>) {
	            	Parameter<?> p = (Parameter<?>) metadata2;
	            	int dim = p.getMinorDimension1();
	            	if (dim > 1) {
		            	buf.append('{');
		            	for (int i = 0; i < dim; i++) {
			            	buf.append(p.getMatrixValue(node.getNr(), i));
			            	if (i < dim - 1) {
				            	buf.append(',');
			            	}
		            	}
		            	buf.append('}');
	            	} else {
		            	buf.append(metadata2.getArrayValue(node.getNr()));
	            	}
	            } else {
	            	buf.append(metadata2.getArrayValue(node.getNr()));
	            }
        	}
        }
        buf.append(']');
        if (!node.isRoot()) {
            buf.append(":").append(node.getLength());
        }
        return buf.toString();
    }

    double getMetaDataTopValue(final Node node, final Function metadataTop) {
        int nr = node.getNr();
        if (nr >= metadataTop.getDimension()) {
            nr = node.getTree().getRoot().getNr();
        }
        return metadataTop.getArrayValue(nr);
    }

    @Override
    public void close(final PrintStream out) {
        treeInput.get().close(out);
    }

}
