package beast.app;

import beast.app.util.Version;

/**
 * This class provides a mechanism for returning the version number of the
 * dr software. It relies on the administrator of the dr source using the
 * module tagging system in CVS. The method getVersion() will return
 * the version of dr under the following condition: <BR>
 * 1. the dr source has been checked out *by tag* before being packaged for
 * distribution.
 * <p/>
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class BEASTVersion extends Version {

    /**
     * Version string: assumed to be in format x.x.x
     */
    private static final String VERSION = "2.5.0";

    private static final String DATE_STRING = "2002-2018";

    private static final boolean IS_PRERELEASE = true;

    private static final String BEAST2_WEBPAGE = "http://beast2.org/";
    
    private static final String BEAST2_SOURCE = "http://github.com/CompEvol/beast2";

    @Override
	public String getVersion() {
        return VERSION;
    }

    @Override
	public String getVersionString() {
        return "v" + VERSION + (IS_PRERELEASE ? " Prerelease" : "");
    }

    @Override
	public String getDateString() {
        return DATE_STRING;
    }

    @Override
	public String[] getCredits() {
        return new String[]{
                "Designed and developed by",
                "Remco Bouckaert, Alexei J. Drummond, Andrew Rambaut & Marc A. Suchard",
                "",
                "Department of Computer Science",
                "University of Auckland",
                "remco@cs.auckland.ac.nz",
                "alexei@cs.auckland.ac.nz",
                "",
                "Institute of Evolutionary Biology",
                "University of Edinburgh",
                "a.rambaut@ed.ac.uk",
                "",
                "David Geffen School of Medicine",
                "University of California, Los Angeles",
                "msuchard@ucla.edu",
                "",
                "Downloads, Help & Resources:",

                BEAST2_WEBPAGE,
                "",
                "Source code distributed under the GNU Lesser General Public License:",
                BEAST2_SOURCE,
                "",
                "BEAST developers:",
                "Alex Alekseyenko, Trevor Bedford, Erik Bloomquist, Joseph Heled, ",
                "Sebastian Hoehna, Denise Kuehnert, Philippe Lemey, Wai Lok Sibon Li, ",
                "Gerton Lunter, Sidney Markowitz, Vladimir Minin, Michael Defoin Platel, ",
                "Oliver Pybus, Tim Vaughan, Chieh-Hsi Wu, Walter Xie",
                "",
                "Thanks to:",
                "Roald Forsberg, Beth Shapiro and Korbinian Strimmer"};
    }

    @Override
	public String getHTMLCredits() {
        return
                "<p>Designed and developed by<br>" +
                        "Remco Bouckaert, Alexei J. Drummond, Andrew Rambaut and Marc A. Suchard</p>" +
                        "<p>Department of Computer Science, University of Auckland<br>" +
                        "<a href=\"mailto:remco@cs.auckland.ac.nz\">remco@cs.auckland.ac.nz</a><br>" +
                        "<a href=\"mailto:alexei@cs.auckland.ac.nz\">alexei@cs.auckland.ac.nz</a></p>" +
                        "<p>Institute of Evolutionary Biology, University of Edinburgh<br>" +
                        "<a href=\"mailto:a.rambaut@ed.ac.uk\">a.rambaut@ed.ac.uk</a></p>" +
                        "<p>David Geffen School of Medicine, University of California, Los Angeles<br>" +
                        "<a href=\"mailto:msuchard@ucla.edu\">msuchard@ucla.edu</a></p>" +
                        "<p><a href=\"" + BEAST2_WEBPAGE + "\">" + BEAST2_WEBPAGE + "</a></p>" +
                        "<p>Source code distributed under the GNU LGPL:<br>" +
                        "<a href=\"" + BEAST2_SOURCE + "\">" + BEAST2_SOURCE + "</a></p>" +
                        "<p>BEAST developers:<br>" +
                        "Alex Alekseyenko, Erik Bloomquist, Joseph Heled, Sebastian Hoehna, Philippe Lemey,<br>" +
                        "Wai Lok Sibon Li, Gerton Lunter, Sidney Markowitz, Vladimir Minin,<br>" +
                        "Michael Defoin Platel, Oliver Pybus, Tim Vaughan, Chieh-Hsi Wu, Walter Xie,<br>" +
                        "Denise Kuehnert</p>" +
                        "<p>Thanks to Roald Forsberg, Beth Shapiro and Korbinian Strimmer</p>";
    }

    /**
     * such as 2.1
     * @return
     */
    public String getMajorVersion() {
        return VERSION.substring(0, VERSION.lastIndexOf("."));
    }

    /** Parse version string, assume it is of the form 1.2.3
     * returns version where each sub-version is divided by 100,
     * so 2.0 -> return 2
     * 2.1 return 2.01
     * 2.2.3 return 2.0103
     * Letters are ignored, so
     * 2.0.e -> 2.0
     * 2.x.1 -> 2.0001
     * @return
     */
    public double parseVersion(String versionString) {
        // is of the form 1.2.3
        String [] strs = versionString.split("\\.");
        double version = 0;
        double divider = 1.0;
        for (int i = 0; i < strs.length; i++) {
            try {
                version += Double.parseDouble(strs[i]) / divider;
                divider = divider * 100.0;
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return version;
    }

    /** inverse of parseVersion **/
    public String formatVersion(double version) {
        if (Double.isInfinite(version)) {
            return " any number";
        }
        String str = "" + (int) (version + 0.000001);
        version = version - (int) (version + 0.000001);
        while (version > 0.00001) {
            version *= 100;
            str += "." + (int) (version + 0.00001);
            version = version - (int) (version + 0.00001);
        }
        return str;
    }

	public static BEASTVersion INSTANCE = new BEASTVersion();
}
