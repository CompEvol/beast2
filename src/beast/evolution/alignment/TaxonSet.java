package beast.evolution.alignment;


import beast.core.Description;
import beast.core.Input;

import java.util.ArrayList;
import java.util.List;

@Description("Set of taxa, useful for instance for multi-gene analysis")
public class TaxonSet extends Taxon {
    public Input<List<Taxon>> m_taxonset = new Input<List<Taxon>>("taxon", "list of taxa making up the set", new ArrayList<Taxon>());
    public Input<Alignment> m_alignment = new Input<Alignment>("alignment", "alignment where each sequence represents a taxon");

    List<String> m_taxonList;

    public TaxonSet() {
    }

    public TaxonSet(final List<Taxon> taxa) throws Exception {
        m_taxonset.setValue(taxa, this);
        initAndValidate();
    }

    @Override
    public void initAndValidate() throws Exception {
        if (m_alignment.get() != null) {
            if (m_taxonset.get().size() > 0) {
                throw new Exception("Only one of taxon and alignment should be specified, not both.");
            }
            m_taxonList = m_alignment.get().m_sTaxaNames;
        } else {
            if (m_taxonset.get().size() == 0) {
                throw new Exception("One of taxon and alignment should be specified, (but not both).");
            }
            m_taxonList = new ArrayList<String>();
            for (final Taxon taxon : m_taxonset.get()) {
                m_taxonList.add(taxon.getID());
            }
        }
    }

    public List<String> asStringList() {
        return m_taxonList;
    }

    //  convenience methods

    public boolean containsAny(final List<String> taxa) {
        final List<String> me = asStringList();
        for (final String taxon : taxa ) {
            if (me.contains(taxon)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(final List<String> taxa) {
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
		StringBuffer buf = new StringBuffer();
		buf.append(indent).append(getID()).append("\n");
		indent += "\t";
		for (Taxon taxon : m_taxonset.get()) {
			buf.append(taxon.toString(indent));
		}
		return buf.toString();
	}
}
