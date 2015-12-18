package beast.evolution.alignment;



import java.util.*;

import beast.core.Description;
import beast.core.BEASTObject;


@Description("For identifying a single taxon")
public class Taxon extends BEASTObject {
    // we can use the ID to identify a taxon name/taxon label
    // if there are multiple taxaset with the same taxa, use
    // idref to refer to the single taxon.
//	public Input<String> m_sLabel = new Input<>("label", "name of the taxon", Validate.REQUIRED);

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
        final List<Taxon> taxa = new ArrayList<>();
        for (final String taxaName : taxaNames) {
            taxa.add(new Taxon(taxaName));
        }
        return taxa;
    }

//    /**
//     * Convenience method to produce a list of taxon objects sorted alphabetically
//     * @param taxaNames a list of taxa names
//     * @return a list of Taxon objects with corresponding names
//     */
//    @Deprecated
//    public static List<Taxon> createSortedTaxonList(final List<String> taxaNames) throws Exception {
//        final List<Taxon> taxa = new ArrayList<>();
//        for (final String taxaName : taxaNames) {
//            taxa.add(new Taxon(taxaName));
//        }
//        Collections.sort(taxa, new Comparator<>() {
//            @Override // assumes IDs are not null
//            public int compare(Taxon o1, Taxon o2) {
//                return o1.getID().compareTo(o2.getID());
//            }
//        });
//        return taxa;
//    }

    /**
     * @param taxa1 a collection of taxa name strings
     * @param taxa2 a second collection of taxa name strings
     * Throws a runtime exception if the two collections do not have the same taxa.
     */
    public static void assertSameTaxa(String id1, Collection<String> taxa1, String id2, Collection<String> taxa2) {
        if (taxa1.size() != taxa2.size()) {
            throw new RuntimeException("Incompatible taxon sets in " + id1 + " and " + id2);
        }
        for (String taxon : taxa1) {
            boolean found = false;
            for (String taxon2 : taxa2) {
                if (taxon.equals(taxon2)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException("Taxon" + taxon + "is not in " + id2);
            }
        }    }
}
