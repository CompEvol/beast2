package beast.evolution.alignment;


import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;

@Description("Set of taxa, useful for instance for multi-gene analysis")
public class TaxonSet extends Taxon {
	public Input<List<Taxon>> m_taxonset = new Input<List<Taxon>>("taxon","list of taxa making up the set", new ArrayList<Taxon>());
	public Input<Alignment> m_alignment = new Input<Alignment>("alignment","alignment where each seaquence represents a taxon");
	
    List<String> m_taxonList;
    
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
	        for (Taxon taxon : m_taxonset.get()) {
	        	m_taxonList.add(taxon.getID());
	        }
		}
	}

	public List<String> asStringList() {
		return m_taxonList;
	}
}
