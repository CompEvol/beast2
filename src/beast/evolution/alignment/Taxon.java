package beast.evolution.alignment;



import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.BEASTObject;


@Description("For identifying a single taxon")
public class Taxon extends BEASTObject {
    // we can use the ID to identify a taxon name/taxon label
    // if there are multiple taxaset with the same taxa, use
    // idref to refer to the single taxon.
//	public Input<String> m_sLabel = new Input<String>("label", "name of the taxon", Validate.REQUIRED);

    public Taxon(final String id) throws Exception {
        setID(id);
        initAndValidate();
    }

    public Taxon() {
    }

    @Override
    public void initAndValidate() throws Exception {

    }

    protected String toString(final String indent) {
    	return indent + getID() + "\n";
    }

    /**
     * Convenience method to produce a list of taxon objects
     * @param taxaNames a list of taxa names
     * @return a list of Taxon objects with corresponding names
     */
    public static List<Taxon> createTaxonList(final List<String> taxaNames) throws Exception {
        final List<Taxon> taxa = new ArrayList<Taxon>();
        for (final String taxaName : taxaNames) {
            taxa.add(new Taxon(taxaName));
        }
        return taxa;
    }
}
