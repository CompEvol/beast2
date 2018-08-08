package beast.evolution.tree;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.util.Log;
import beast.evolution.alignment.TaxonSet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


@Description("A trait set represent a collection of properties of taxons, for the use of initializing a tree. " +
        "The traits are represented as text content in taxon=value form, for example, for a date trait, we" +
        "could have a content of chimp=1950,human=1991,neander=-10000. All white space is ignored, so they can" +
        "be put on multiple tabbed lines in the XML. " +
        "The type of node in the tree determines what happes with this information. The default Node only " +
        "recognizes 'date', 'date-forward' and 'date-backward' as a trait, but by creating custom Node classes " +
        "other traits can be supported as well.")
public class TraitSet extends BEASTObject {

    /**
     * @return a string describing the type of date that this trait represents. could be null.
     */
    public String getDateType() {
        return traitNameInput.get();
    }

    public enum Units {
        year, month, day
    }

    final public Input<String> traitNameInput = new Input<>("traitname", "name of the trait, used as meta data name for the tree. " +
            "Special traitnames that are recognized are '" + DATE_TRAIT + "','" + DATE_FORWARD_TRAIT + "' and '" + DATE_BACKWARD_TRAIT + "'.", Validate.REQUIRED);
    final public Input<Units> unitsInput = new Input<>("units", "name of the units in which values are posed, " +
            "used for conversion to a real value. This can be " + Arrays.toString(Units.values()) + " (default 'year')", Units.year, Units.values());
    final public Input<String> traitsInput = new Input<>("value", "traits encoded as taxon=value pairs separated by commas", Validate.REQUIRED);
    final public Input<TaxonSet> taxaInput = new Input<>("taxa", "contains list of taxa to map traits to", Validate.REQUIRED);

    final public Input<String> dateTimeFormatInput = new Input<>("dateFormat", "the date/time format to be parsed, (e.g., 'dd/M/yyyy')");

    final public static String DATE_TRAIT = "date";
    final public static String DATE_FORWARD_TRAIT = "date-forward";
    final public static String DATE_BACKWARD_TRAIT = "date-backward";
    final public static String AGE_TRAIT = "age";

    /**
     * String values of taxa in order of taxons in alignment*
     */
    protected String[] taxonValues;
    
    /**
     * double representation of taxa value *
     */
    protected double[] values;
    protected double minValue;
    protected double maxValue;

    protected Map<String, Integer> map;

    /**
     * Whether or not values are ALL numeric.
     */
    boolean numeric = true;
    
    @Override
    public void initAndValidate() {
        if (traitsInput.get().matches("^\\s*$")) {
            return;
        }

        // first, determine taxon numbers associated with traits
        // The Taxon number is the index in the alignment, and
        // used as node number in a tree.
        map = new HashMap<>();
        List<String> labels = taxaInput.get().asStringList();
        String[] traits = traitsInput.get().split(",");
        taxonValues = new String[labels.size()];
        values = new double[labels.size()];
        for (String trait : traits) {
            trait = trait.replaceAll("\\s+", " ");
            String[] strs = trait.split("=");
            if (strs.length != 2) {
                throw new IllegalArgumentException("could not parse trait: " + trait);
            }
            String taxonID = normalize(strs[0]);
            int taxonNr = labels.indexOf(taxonID);
            if (taxonNr < 0) {
                throw new IllegalArgumentException("Trait (" + taxonID + ") is not a known taxon. Spelling error perhaps?");
            }
            taxonValues[taxonNr] = normalize(strs[1]);
            try {
                values[taxonNr] = convertValueToDouble(taxonValues[taxonNr]);
            } catch (DateTimeParseException ex) {
                Log.err.println("Failed to parse date '" + taxonValues[taxonNr] + "' using format '" + dateTimeFormatInput.get() + "'.");
                System.exit(1);
            } catch (IllegalArgumentException ex) {
                Log.err.println("Failed to parse date '" + taxonValues[taxonNr] + "'.");
                System.exit(1);
            }

            map.put(taxonID, taxonNr);
            
            if (Double.isNaN(values[taxonNr]))
                numeric = false;
        }

        // find extremes
        minValue = values[0];
        maxValue = values[0];
        for (double value : values) {
            minValue = Math.min(minValue, value);
            maxValue = Math.max(maxValue, value);
        }

        // sanity check: did we cover all taxa?
        double defaultValue = 0;
        if (traitNameInput.get().equals(DATE_TRAIT) || traitNameInput.get().equals(DATE_FORWARD_TRAIT)) {
        	defaultValue = maxValue;
        }
        for (int i = 0; i < labels.size(); i++) {
            if (taxonValues[i] == null) {
                Log.warning.println("WARNING: no trait specified for " + labels.get(i) +": Assumed to be " + defaultValue);
                map.put(labels.get(i), i);
                values[i] = defaultValue;
            }
        }

        if (traitNameInput.get().equals(DATE_TRAIT) || traitNameInput.get().equals(DATE_FORWARD_TRAIT)) {
            for (int i = 0; i < labels.size(); i++) {
                values[i] = maxValue - values[i];
            }
        }

        if (traitNameInput.get().equals(DATE_BACKWARD_TRAIT) || traitNameInput.get().equals(AGE_TRAIT)) {
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

    @Deprecated // use getStringValue by name instead
    public String getStringValue(int taxonNr) {
        return taxonValues[taxonNr];
    }

    @Deprecated // use getValue by name instead
    public double getValue(int taxonNr) {
        if (values == null) {
            return 0;
        }
        return values[taxonNr];
    }

    public String getStringValue(String taxonName) {
        if (taxonValues == null || map == null || map.get(taxonName) == null) {
            return null;
        }
        return taxonValues[map.get(taxonName)];

    }

    public double getValue(String taxonName) {
        if (values == null || map == null || map.get(taxonName) == null) {
                return 0;
        }
        //Log.trace.println("Trait " + taxonName + " => " + values[map.get(taxonName)]);
        return values[map.get(taxonName)];
    }

    /**
     * see if we can convert the string to a double value *
     */
    public double convertValueToDouble(String str) {
        // default, try to interpret the string as a number
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            // does not look like a number
            if (traitNameInput.get().equals(DATE_TRAIT) ||
                    traitNameInput.get().equals(DATE_FORWARD_TRAIT) ||
                    traitNameInput.get().equals(DATE_BACKWARD_TRAIT)) {

                double year;
                if (dateTimeFormatInput.get() == null) {
                    if (str.matches(".*[a-zA-Z].*")) {
                        str = str.replace('/', '-');
                    }
                    // following is deprecated, but the best thing around at the moment
                    // see also comments in TipDatesInputEditor
                    long date = Date.parse(str);
                    year = 1970.0 + date / (60.0 * 60 * 24 * 365 * 1000);
                    Log.warning.println("No date/time format provided, using default parsing: '" + str + "' parsed as '" + year + "'");
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormatInput.get());
                    LocalDate date = LocalDate.parse(str, formatter);

                    Log.warning.println("Using format '" + dateTimeFormatInput.get() + "' to parse '" + str +
                            "' as: " + (date.getYear() + (date.getDayOfYear()-1.0) / (date.isLeapYear() ? 366.0 : 365.0)));

                    year = date.getYear() + (date.getDayOfYear()-1.0) / (date.isLeapYear() ? 366.0 : 365.0);
                }

                switch (unitsInput.get()) {
                    case month:
                        return year * 12.0;
                    case day:
                        return year * 365;
                    default:
                        return year;
                }
            }
        }

        return Double.NaN;
    }

    /**
     * remove start and end spaces
     */
    protected String normalize(String str) {
        if (str.charAt(0) == ' ') {
            str = str.substring(1);
        }
        if (str.endsWith(" ")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * Retrieve absolute date from height above most recent sample.
     *
     * @param height age before most recent sample.
     * @return date, which may be either backward-time or forward-time depending
     * on the trait.  If trait type is not defined, the height itself is returned.
     */
    public double getDate(double height) {
        if (traitNameInput.get().equals(DATE_TRAIT) || traitNameInput.get().equals(DATE_FORWARD_TRAIT)) {
            return maxValue - height;
        }

        if (traitNameInput.get().equals(DATE_BACKWARD_TRAIT) || traitNameInput.get().equals(AGE_TRAIT)) {
            return minValue + height;
        }
        return height;
    }

    /**
     * Determines whether trait is recognised as specifying taxa dates.
     * @return true if this is a date trait.
     */
    public boolean isDateTrait() {
        return traitNameInput.get().equals(DATE_TRAIT)
                || traitNameInput.get().equals(DATE_FORWARD_TRAIT)
                || traitNameInput.get().equals(DATE_BACKWARD_TRAIT) || traitNameInput.get().equals(AGE_TRAIT);
    }

    /**
     * @return true if trait values are (all) numeric.
     */
    public boolean isNumeric() {
        return numeric;
    }
} // class TraitSet
