package beast.evolution.tree;

//import java.text.SimpleDateFormat;
//import java.util.Date;



import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.core.Description;
import beast.core.Input;
import beast.core.BEASTObject;
import beast.core.Input.Validate;
import beast.core.util.Log;
import beast.evolution.alignment.TaxonSet;


@Description("A trait set represent a collection of properties of taxons, for the use of initializing a tree. " +
        "The traits are represented as text content in taxon=value form, for example, for a date trait, we" +
        "could have a content of chimp=1950,human=1991,neander=-10000. All white space is ignored, so they can" +
        "be put on multiple tabbed lines in the XML. " +
        "The type of node in the tree determines what happes with this information. The default Node only " +
        "recognizes 'date', 'date-forward' and 'date-backward' as a trait, but by creating custom Node classes " +
        "other traits can be supported as well.")
public class TraitSet extends BEASTObject {

    public enum Units {
        year, month, day
    }

    public Input<String> traitNameInput = new Input<String>("traitname", "name of the trait, used as meta data name for the tree. " +
            "Special traitnames that are recognized are '" + DATE_TRAIT + "','" + DATE_FORWARD_TRAIT + "' and '" + DATE_BACKWARD_TRAIT + "'.", Validate.REQUIRED);
    public Input<Units> unitsInput = new Input<Units>("units", "name of the units in which values are posed, " +
            "used for conversion to a real value. This can be " + Arrays.toString(Units.values()) + " (default 'year')", Units.year, Units.values());
    public Input<String> traitsInput = new Input<String>("value", "traits encoded as taxon=value pairs separated by commas", Validate.REQUIRED);
    public Input<TaxonSet> taxaInput = new Input<TaxonSet>("taxa", "contains list of taxa to map traits to", Validate.REQUIRED);

    final public static String DATE_TRAIT = "date";
    final public static String DATE_FORWARD_TRAIT = "date-forward";
    final public static String DATE_BACKWARD_TRAIT = "date-backward";

    /**
     * String values of taxa in order of taxons in alignment*
     */
    protected String[] taxonValues;
    
    /**
     * double representation of taxa value *
     */
    double[] values;
    double minValue;
    double maxValue;

    Map<String, Double> map;
    
    /**
     * Whether or not values are ALL numeric.
     */
    boolean numeric = true;
    
    @Override
    public void initAndValidate() throws Exception {
        if (traitsInput.get().matches("^\\s*$")) {
            return;
        }

        // first, determine taxon numbers associated with traits
        // The Taxon number is the index in the alignment, and
        // used as node number in a tree.
        map = new HashMap<String, Double>();
        List<String> labels = taxaInput.get().asStringList();
        String[] traits = traitsInput.get().split(",");
        taxonValues = new String[labels.size()];
        values = new double[labels.size()];
        for (String trait : traits) {
            trait = trait.replaceAll("\\s+", " ");
            String[] sStrs = trait.split("=");
            if (sStrs.length != 2) {
                throw new Exception("could not parse trait: " + trait);
            }
            String taxonID = normalize(sStrs[0]);
            int taxonNr = labels.indexOf(taxonID);
            if (taxonNr < 0) {
                throw new Exception("Trait (" + taxonID + ") is not a known taxon. Spelling error perhaps?");
            }
            taxonValues[taxonNr] = normalize(sStrs[1]);
            values[taxonNr] = parseDouble(taxonValues[taxonNr]);
            map.put(taxonID,  values[taxonNr]);
            
            if (Double.isNaN(values[taxonNr]))
                numeric = false;
        }

        // sanity check: did we cover all taxa?
        for (int i = 0; i < labels.size(); i++) {
            if (taxonValues[i] == null) {
                Log.warning.println("WARNING: no trait specified for " + labels.get(i));
            }
        }

        // find extremes
        minValue = values[0];
        maxValue = values[0];
        for (double fValue : values) {
            minValue = Math.min(minValue, fValue);
            maxValue = Math.max(maxValue, fValue);
        }

        if (traitNameInput.get().equals(DATE_TRAIT) || traitNameInput.get().equals(DATE_FORWARD_TRAIT)) {
            for (int i = 0; i < labels.size(); i++) {
                values[i] = maxValue - values[i];
            }
        }

        if (traitNameInput.get().equals(DATE_BACKWARD_TRAIT)) {
            for (int i = 0; i < labels.size(); i++) {
                values[i] = values[i] - minValue;
            }
        }

        for (int i = 0; i < labels.size(); i++) {
            Log.info.println(labels.get(i) + " = " + taxonValues[i] + " (" + (values[i]) + ")");
        }
    } // initAndValidate

    /**
     * some getters and setters *
     */
    public String getTraitName() {
        return traitNameInput.get();
    }

    public String getStringValue(int iTaxonNr) {
        return taxonValues[iTaxonNr];
    }

    @Deprecated // use getValue by name instead 
    public double getValue(int iTaxonNr) {
        if (values == null) {
            return 0;
        }
        return values[iTaxonNr];
    }

    public double getValue(String taxonName) {
        if (values == null) {
            return 0;
        }
        Log.trace.println("Trait " + taxonName + " => " + map.get(taxonName));
        return map.get(taxonName);
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
            if (traitNameInput.get().equals(DATE_TRAIT) || 
            	traitNameInput.get().equals(DATE_FORWARD_TRAIT) ||
            	traitNameInput.get().equals(DATE_BACKWARD_TRAIT))	{
            	try {
            		if (sStr.matches(".*[a-zA-Z].*")) {
            			sStr = sStr.replace('/', '-');
            		}
            		long date = Date.parse(sStr);
            		double year = 1970.0 + date / (60.0*60*24*365*1000);
            		switch (unitsInput.get()) {
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
        //return 0;
        return Double.NaN;
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
        if (traitNameInput.get().equals(DATE_TRAIT) || traitNameInput.get().equals(DATE_FORWARD_TRAIT)) {
            return maxValue - fHeight;
        }

        if (traitNameInput.get().equals(DATE_BACKWARD_TRAIT)) {
            return minValue + fHeight;
        }
        return fHeight;
    }
    
    /**
     * Determines whether trait is recognised as specifying taxa dates.
     * @return true if this is a date trait.
     */
    public boolean isDateTrait() {
        return traitNameInput.get().equals(DATE_TRAIT)
                || traitNameInput.get().equals(DATE_FORWARD_TRAIT)
                || traitNameInput.get().equals(DATE_BACKWARD_TRAIT);
    }
    

    /**
     * @return true if trait values are (all) numeric.
     */
    public boolean isNumeric() {
        return numeric;
    }
    
} // class TraitSet
