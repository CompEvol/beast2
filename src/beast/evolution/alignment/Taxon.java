package beast.evolution.alignment;


import beast.core.Description;
import beast.core.Plugin;

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
}
