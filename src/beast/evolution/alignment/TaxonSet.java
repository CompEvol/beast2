package beast.evolution.alignment;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import beast.core.Description;
import beast.core.Input;


@Description("Set of taxa, useful for instance for multi-gene analysis")
public class TaxonSet extends Taxon {
    public Input<List<Taxon>> taxonsetInput = new Input<List<Taxon>>("taxon", "list of taxa making up the set", new ArrayList<Taxon>());
    public Input<Alignment> alignmentInput = new Input<Alignment>("alignment", "alignment where each sequence represents a taxon");

    List<String> taxaNames;

    public TaxonSet() {
    }

    public TaxonSet(final List<Taxon> taxa) throws Exception {
        taxonsetInput.setValue(taxa, this);
        initAndValidate();
    }

    @Override
    public void initAndValidate() throws Exception {
        if (alignmentInput.get() != null) {
            if (taxonsetInput.get().size() > 0) {
                throw new Exception("Only one of taxon and alignment should be specified, not both.");
            }
            taxaNames = alignmentInput.get().taxaNames;
        } else {
            if (taxonsetInput.get().size() == 0) {
                throw new Exception("One of taxon and alignment should be specified, (but not both).");
            }
            taxaNames = new ArrayList<String>();
            for (final Taxon taxon : taxonsetInput.get()) {
            	taxaNames.add(taxon.getID());
            }
        }
    }

    public List<String> asStringList() {
        return taxaNames;
    }

    //  convenience methods

    public boolean containsAny(final Collection<String> taxa) {
        final List<String> me = asStringList();
        for (final String taxon : taxa ) {
            if (me.contains(taxon)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(final Collection<String> taxa) {
        final List<String> me = asStringList();
        for (final String taxon : taxa ) {
            if (!me.contains(taxon)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if at least 1 member of taxa contained in this set.
     * @param taxa a collection of taxa
     */
    public boolean containsAny(final TaxonSet taxa) {
        return containsAny(taxa.asStringList());
    }

    /**
     * @return true if taxa is a subset of this set
     * @param    taxa
     */
    public boolean containsAll(final TaxonSet taxa) {
        return containsAll(taxa.asStringList());
    }
    
    @Override
    public String toString() {
    	return toString("\t");
    }

    @Override
	protected String toString(String indent) {
		final StringBuilder buf = new StringBuilder();
		buf.append(indent).append(getID()).append("\n");
		indent += "\t";
		for (final Taxon taxon : taxonsetInput.get()) {
			buf.append(taxon.toString(indent));
		}
		return buf.toString();
	}
}
