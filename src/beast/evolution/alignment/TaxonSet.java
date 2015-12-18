package beast.evolution.alignment;



import beast.core.Description;
import beast.core.Input;

import java.util.*;


@Description("A TaxonSet is an ordered set of taxa. The order on the taxa is provided at the time of construction" +
        " either from a list of taxon objects or an alignment.")
public class TaxonSet extends Taxon {

    final public Input<List<Taxon>> taxonsetInput = new Input<>("taxon", "list of taxa making up the set", new ArrayList<>());

    final public Input<Alignment> alignmentInput = new Input<>("alignment", "alignment where each sequence represents a taxon");

    protected List<String> taxaNames;
    protected List<Taxon> taxonList;

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

    // for testing purposes (Huw)
    public TaxonSet(final String id, final List<Taxon> taxa) throws Exception {
        setID(id);
        taxonsetInput.setValue(taxa, this);
        initAndValidate();
    }

    @Override
    public void initAndValidate() throws Exception {
        taxonList = taxonsetInput.get();
        if (alignmentInput.get() != null) {
            if (taxonList.size() > 0) {
                throw new Exception("Only one of taxon and alignment should be specified, not both.");
            }
            taxaNames = alignmentInput.get().taxaNames;
        } else {
            if (taxonList.size() == 0) {
                throw new Exception("Either taxon or alignment should be specified.");
            }
            taxaNames = new ArrayList<>();
            for (final Taxon taxon : taxonList) {
            	taxaNames.add(taxon.getID());
            }
        }
    }

    public Set<Taxon> getTaxonSet() {
        final Set<Taxon> unorderedTaxa = new HashSet<>(taxonList);
        return unorderedTaxa;
    }

    /**
     * @return an unmodifiable list of taxa names as strings.
     */
    public List<String> asStringList() {
        if (taxaNames == null) return null;
        return Collections.unmodifiableList(taxaNames);
    }

    /**
     * @return the taxa names as a set of strings.
     */
    public Set<String> getTaxaNames() {
        return new TreeSet<>(taxaNames);
    }

    /**
     * @return the ID of the i'th taxon.
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
        if (taxaNames == null) return 0;
        return taxaNames.size();
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
		for (final Taxon taxon : taxonList) {
			buf.append(taxon.toString(indent));
		}
		return buf.toString();
	}
}
