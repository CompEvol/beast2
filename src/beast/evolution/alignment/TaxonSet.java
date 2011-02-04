package beast.evolution.alignment;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;

@Description("Set of taxa, useful for instance for multi-gene analysis")
public class TaxonSet extends Taxon {
	public Input<List<Taxon>> m_taxonset = new Input<List<Taxon>>("taxon","list of taxa making up the set", new ArrayList<Taxon>(), Validate.REQUIRED);
	
	@Override
	public void initAndValidate() {
		
	}
}
