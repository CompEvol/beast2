package beast.evolution.tree;

//import java.text.SimpleDateFormat;
//import java.util.Date;

import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;
import beast.evolution.alignment.Alignment;

@Description("A trait set represent a collection of properties of taxons, for the use of initializing a tree. " +
		"The traits are represented as text content in taxon=value form, for example, for a date trait, we" +
		"could have a content of chimp=1950,human=1991,neander=-10000. All white space is ignored, so they can" +
		"be put on multiple tabbed lines in the XML. " +
		"The type of node in the tree determines what happes with this information. The default Node only " +
		"recognizes 'date', 'date-forward' and 'date-backward' as a trait, but by creating custom Node classes " +
		"other traits can be supported as well.")
public class TraitSet extends Plugin {
	public Input<String> m_sTraitName = new Input<String>("traitname", "name of the trait, used as meta data name for the tree. " +
			"Special traitnames that are recognized are '"+DATE_TRAIT+"','"+DATE_FORWARD_TRAIT+"' and '"+ DATE_BACKWARD_TRAIT+"'.", Validate.REQUIRED);
	public Input<String> m_sUnits = new Input<String>("units", "name of the units in which values are posed, " +
			"used for conversion to a real value. For a date-trait, this can be 'year', 'month' or 'day'", Validate.REQUIRED);
	public Input<String> m_traits = new Input<String>("value","traits encoded as taxon=value pairs separated by commas", Validate.REQUIRED);
	public Input<Alignment> m_taxa = new Input<Alignment>("taxa","contains list of taxa to map traits to", Validate.REQUIRED);

	final static String DATE_TRAIT = "date";
	final static String DATE_FORWARD_TRAIT = "date-forward";
	final static String DATE_BACKWARD_TRAIT = "date-backward";
	
	/** String values of taxa in order of taxons in alignment**/
    String [] m_sValues;
    /** double representation of taxa value **/
    double [] m_fValues;
    
	@Override
    public void initAndValidate() throws Exception {
		// first, determine taxon numbers associated with traits
		// The Taxon number is the index in the alignment, and
		// used as node number in a tree.
        List<String> sLabels = m_taxa.get().m_sTaxaNames;
        String [] sTraits = m_traits.get().split(",");
        m_sValues = new String[sLabels.size()];
        m_fValues = new double[sLabels.size()];
		for (String sTrait : sTraits) {
			sTrait = sTrait.replaceAll("\\s+", " ");
			String [] sStrs = sTrait.split("=");
			if (sStrs.length!=2) {
				throw new Exception("could not parse trait: " + sTrait);
			}
			String sTaxonID = normalize(sStrs[0]);
			int iTaxon = sLabels.indexOf(sTaxonID);
			if (iTaxon < 0) {
				throw new Exception("Trait (" + sTaxonID +") is not a known taxon. Spelling error perhaps?");
			}
			m_sValues[iTaxon] = normalize(sStrs[1]);
			m_fValues[iTaxon] = parseDouble(m_sValues[iTaxon]); 
		}
		
		// sanity check: did we cover all taxa?
	    for (int i = 0; i < sLabels.size(); i++){
	    	if (m_sValues[i] == null) {
	    		System.err.println("WARNING: no trait specified for " + sLabels.get(i));
	    	}
	    }

	    // find extremes
	    double fMinValue = m_fValues[0];
	    double fMaxValue = m_fValues[0];
	    for (double fValue : m_fValues) {
	    	fMinValue = Math.min(fMinValue, fValue);
	    	fMaxValue = Math.max(fMaxValue, fValue);
	    }
	    
	    if (m_sTraitName.get().equals(DATE_BACKWARD_TRAIT)) {
		    for (int i = 0; i < sLabels.size(); i++){
		    	m_fValues[i] = m_fValues[i] - fMinValue;
		    }
	    } else if (m_sTraitName.get().equals(DATE_FORWARD_TRAIT) || m_sTraitName.get().equals(DATE_TRAIT)) {
		    for (int i = 0; i < sLabels.size(); i++){
		    	m_fValues[i] = fMaxValue - m_fValues[i];
		    }
	    }
	    
	    
	    for (int i = 0; i < sLabels.size(); i++){
	    	System.out.println(sLabels.get(i) + " = " + m_sValues[i] + " (" + (m_fValues[i]) + ")");
	    }
	} // initAndValidate
	
	/** some getters and setters **/
	public String getTraitName() {
		return m_sTraitName.get();
	}
	
	public String getStringValue(int iTaxonNr) {
		return m_sValues[iTaxonNr];
	}

	public double getValue(int iTaxonNr) {
		return m_fValues[iTaxonNr];
	}

	/** see if we can convert the string to a double value **/
	private double parseDouble(String sStr) throws Exception {
		/** deal with a few special cases **/
//		try {
//			if (getName().equals("date")) {
//					Date date = new SimpleDateFormat().parse(sStr);
//					// the number of milliseconds since January 1, 1970, 00:00:00 GMT
//					long nMilliSecs = date.getTime();
//					if (m_sUnits.get().equals("year")) {
//						// ... 
//					} else if (m_sUnits.get().equals("months")) {
//						// ... 
//					} else if (m_sUnits.get().equals("days")) {
//						// ... 
//					}
//					// return in years
//					return nMilliSecs/1000/3600/356;
//			} 
//		} catch (Exception e) {
//			// ignore
//		}
		// default, try to interpret the string as a number
		try {
			return Double.parseDouble(sStr); 
		} catch (NumberFormatException e) {
			// does not look like a number
		}
		return 0;
	} // parseStrings
	
	/** remove start and end spaces */
	private String normalize(String sStr) {
		if (sStr.charAt(0) == ' ') {
			sStr = sStr.substring(1);
		}
		if (sStr.endsWith(" ")) {
			sStr = sStr.substring(0, sStr.length()-1);
		}
		return sStr;
	}
	
} // class TraitSet
