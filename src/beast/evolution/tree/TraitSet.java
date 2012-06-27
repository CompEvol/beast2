package beast.evolution.tree;

//import java.text.SimpleDateFormat;
//import java.util.Date;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;
import beast.evolution.alignment.TaxonSet;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.sun.org.apache.regexp.internal.recompile;

@Description("A trait set represent a collection of properties of taxons, for the use of initializing a tree. " +
        "The traits are represented as text content in taxon=value form, for example, for a date trait, we" +
        "could have a content of chimp=1950,human=1991,neander=-10000. All white space is ignored, so they can" +
        "be put on multiple tabbed lines in the XML. " +
        "The type of node in the tree determines what happes with this information. The default Node only " +
        "recognizes 'date', 'date-forward' and 'date-backward' as a trait, but by creating custom Node classes " +
        "other traits can be supported as well.")
public class TraitSet extends Plugin {

    public enum Units {
        year, month, day
    }

    public Input<String> m_sTraitName = new Input<String>("traitname", "name of the trait, used as meta data name for the tree. " +
            "Special traitnames that are recognized are '" + DATE_TRAIT + "','" + DATE_FORWARD_TRAIT + "' and '" + DATE_BACKWARD_TRAIT + "'.", Validate.REQUIRED);
    public Input<Units> m_sUnits = new Input<Units>("units", "name of the units in which values are posed, " +
            "used for conversion to a real value. This can be " + Arrays.toString(Units.values()) + " (default 'year')", Units.year, Units.values());
    public Input<String> m_traits = new Input<String>("value", "traits encoded as taxon=value pairs separated by commas", Validate.REQUIRED);
    public Input<TaxonSet> m_taxa = new Input<TaxonSet>("taxa", "contains list of taxa to map traits to", Validate.REQUIRED);

    final static String DATE_TRAIT = "date";
    final static String DATE_FORWARD_TRAIT = "date-forward";
    final static String DATE_BACKWARD_TRAIT = "date-backward";

    /**
     * String values of taxa in order of taxons in alignment*
     */
    String[] m_sValues;
    /**
     * double representation of taxa value *
     */
    double[] m_fValues;
    double m_fMinValue;
    double m_fMaxValue;

    @Override
    public void initAndValidate() throws Exception {
        if (m_traits.get().matches("^\\s*$")) {
            return;
        }

        // first, determine taxon numbers associated with traits
        // The Taxon number is the index in the alignment, and
        // used as node number in a tree.
        List<String> sLabels = m_taxa.get().asStringList();
        String[] sTraits = m_traits.get().split(",");
        m_sValues = new String[sLabels.size()];
        m_fValues = new double[sLabels.size()];
        for (String sTrait : sTraits) {
            sTrait = sTrait.replaceAll("\\s+", " ");
            String[] sStrs = sTrait.split("=");
            if (sStrs.length != 2) {
                throw new Exception("could not parse trait: " + sTrait);
            }
            String sTaxonID = normalize(sStrs[0]);
            int iTaxon = sLabels.indexOf(sTaxonID);
            if (iTaxon < 0) {
                throw new Exception("Trait (" + sTaxonID + ") is not a known taxon. Spelling error perhaps?");
            }
            m_sValues[iTaxon] = normalize(sStrs[1]);
            m_fValues[iTaxon] = parseDouble(m_sValues[iTaxon]);
        }

        // sanity check: did we cover all taxa?
        for (int i = 0; i < sLabels.size(); i++) {
            if (m_sValues[i] == null) {
                System.out.println("WARNING: no trait specified for " + sLabels.get(i));
            }
        }

        // find extremes
        m_fMinValue = m_fValues[0];
        m_fMaxValue = m_fValues[0];
        for (double fValue : m_fValues) {
            m_fMinValue = Math.min(m_fMinValue, fValue);
            m_fMaxValue = Math.max(m_fMaxValue, fValue);
        }

        if (m_sTraitName.get().equals(DATE_TRAIT) || m_sTraitName.get().equals(DATE_FORWARD_TRAIT)) {
            for (int i = 0; i < sLabels.size(); i++) {
                m_fValues[i] = m_fMaxValue - m_fValues[i];
            }
        }

        if (m_sTraitName.get().equals(DATE_BACKWARD_TRAIT)) {
            for (int i = 0; i < sLabels.size(); i++) {
                m_fValues[i] = m_fValues[i] - m_fMinValue;
            }
        }

        for (int i = 0; i < sLabels.size(); i++) {
            System.out.println(sLabels.get(i) + " = " + m_sValues[i] + " (" + (m_fValues[i]) + ")");
        }
    } // initAndValidate

    /**
     * some getters and setters *
     */
    public String getTraitName() {
        return m_sTraitName.get();
    }

    public String getStringValue(int iTaxonNr) {
        return m_sValues[iTaxonNr];
    }

    public double getValue(int iTaxonNr) {
        if (m_fValues == null) {
            return 0;
        }
        return m_fValues[iTaxonNr];
    }

    /**
     * see if we can convert the string to a double value *
     */
    private double parseDouble(String sStr) throws Exception {
        // default, try to interpret the string as a number
        try {
            return Double.parseDouble(sStr);
        } catch (NumberFormatException e) {
            // does not look like a number
            if (m_sTraitName.get().equals(DATE_TRAIT) || 
            	m_sTraitName.get().equals(DATE_FORWARD_TRAIT) ||
            	m_sTraitName.get().equals(DATE_BACKWARD_TRAIT))	{
            	try {
            		if (sStr.matches(".*[a-zA-Z].*")) {
            			sStr = sStr.replace('/', '-');
            		}
            		long date = Date.parse(sStr);
            		double year = 1970.0 + date / (60.0*60*24*365*1000);
            		switch (m_sUnits.get()) {
            		case month : return year * 12.0;
            		case day: return year * 365;
            		default :
            			return year;
            		}
            	} catch (Exception e2) {
            		// does not look like a date, give up
    			}
            }
        }
        return 0;
    } // parseStrings

    /**
     * remove start and end spaces
     */
    String normalize(String sStr) {
        if (sStr.charAt(0) == ' ') {
            sStr = sStr.substring(1);
        }
        if (sStr.endsWith(" ")) {
            sStr = sStr.substring(0, sStr.length() - 1);
        }
        return sStr;
    }

    public double getDate(double fHeight) {
        if (m_sTraitName.get().equals(DATE_TRAIT) || m_sTraitName.get().equals(DATE_FORWARD_TRAIT)) {
            return m_fMaxValue - fHeight;
        }

        if (m_sTraitName.get().equals(DATE_BACKWARD_TRAIT)) {
            return m_fMinValue + fHeight;
        }
        return fHeight;
    }

} // class TraitSet
