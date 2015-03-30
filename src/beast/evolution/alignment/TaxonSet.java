package beast.evolution.alignment;



import beast.core.Description;
import beast.core.Input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


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
    
    public TaxonSet(final Alignment alignment) throws Exception {
        alignmentInput.setValue(alignment, this);
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
        Collections.sort(taxaNames);
    }

    public List<String> asStringList() {
        return taxaNames;
    }

    /**
     * @return the ID of the ith taxon.
     */
    public String getTaxonId(int taxonIndex) {
        return taxaNames.get(taxonIndex);
    }

    /**
     * return index of given Taxon name
     * @param id
     * @return  -1 if not found
     */
    public int getTaxonIndex(String id) {
        for (int i = 0; i < taxaNames.size(); i++) {
            if (getTaxonId(i).contentEquals(id)) return i;
        }
        return -1;
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
    
    /**
     * @return number of taxa in this taxon set
     */
    public int getTaxonCount() {
        return asStringList().size();
    }
    
    /**
     * @return number of taxa in this taxon set
     * @deprecated Exists only for consistency with method in Alignment. Use
     * getTaxonCount() instead.
     */
    @Deprecated
    public int getNrTaxa() {
        return getTaxonCount();
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
