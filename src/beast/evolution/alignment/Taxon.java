package beast.evolution.alignment;


import beast.core.Description;
import beast.core.Plugin;

import java.util.ArrayList;
import java.util.List;

@Description("For identifying a single taxon")
public class Taxon extends Plugin {
    // we can use the ID to identify a taxon name/taxon label
    // if there are multiple taxaset with the same taxa, use
    // idref to refer to the single taxon.
//	public Input<String> m_sLabel = new Input<String>("label", "name of the taxon", Validate.REQUIRED);

    public Taxon(String id) throws Exception {
        setID(id);
        initAndValidate();
    }

    public Taxon() {
    }

    @Override
    public void initAndValidate() throws Exception {

    }

    protected String toString(String indent) {
    	return indent + getID() + "\n";
    }

    /**
     * Convenience method to produce a list of taxon objects
     * @param taxaNames a list of taxa names
     * @return a list of Taxon objects with corresponding names
     */
    public static List<Taxon> createTaxonList(List<String> taxaNames) throws Exception {
        List<Taxon> taxa = new ArrayList<Taxon>();
        for (String taxaName : taxaNames) {
            taxa.add(new Taxon(taxaName));
        }
        return taxa;
    }
}
