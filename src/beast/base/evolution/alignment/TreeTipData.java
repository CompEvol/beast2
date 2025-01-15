package beast.base.evolution.alignment;


import java.io.PrintStream;

import org.w3c.dom.Node;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.datatype.DataType;
import beast.base.inference.StateNode;
import beast.base.inference.parameter.Map;

@Description("Generic class for associating data to tips of a tree, e.g. an Alignment.")
public class TreeTipData extends Map<String> {

    final public Input<TaxonSet> taxonSetInput =
            new Input<>("taxa", "An optional taxon-set used only to sort the sequences into the same order as they appear in the taxon-set.", new TaxonSet(), Validate.OPTIONAL);

    final public Input<DataType.Base> userDataTypeInput = new Input<>("userDataType", "non-standard, user specified data type, if specified 'dataType' is ignored");

	/** Loggable implementation **/

    @Override
	public void init(PrintStream out) {
	}

	@Override
	public void log(long sample, PrintStream out) {
	}

	@Override
	public void close(PrintStream out) {
	}


	/** Function implementation **/

	@Override
	public int getDimension() {
		return 0;
	}

	@Override
	public double getArrayValue(int dim) {
		return 0;
	}

	/** Map implementation **/
	
	@Override
	protected Class<?> mapType() {
        return String.class;
	}

	
	/** StateNode implementation **/
	
	@Override
	public void setEverythingDirty(boolean isDirty) {
	}

	@Override
	public StateNode copy() {
		return null;
	}

	@Override
	public void assignTo(StateNode other) {
	}

	@Override
	public void assignFrom(StateNode other) {
	}

	@Override
	public void assignFromFragile(StateNode other) {
	}

	@Override
	public void fromXML(Node node) {
	}

	@Override
	public int scale(double scale) {
		return 0;
	}

	@Override
	protected void store() {
	}

	@Override
	public void restore() {
	}

}
