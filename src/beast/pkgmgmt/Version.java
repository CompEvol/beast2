package beast.pkgmgmt;

/**
 * Version last changed 2004/05/07 by AER
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: Version.java,v 1.13 2005/07/11 14:06:25 rambaut Exp $
 */
public abstract class Version {

    public abstract String getVersion();

    public abstract String getVersionString();

    public abstract String getDateString();

    public abstract String[] getCredits();

    public String getHTMLCredits() {
        String str = "";
        for (String s : getCredits()) {
            if (s.contains("@")) {
                str += "<a href=\"mailto:" + s + "\">" + s + "</a><br>";
            }
            if (s.contains("http")) {
                str += "<a href=\"" + s + "\">" + s + "</a><br>";
            } else {
                str += "<p>" + s + "</p>";
            }
        }
        return str;
    }

    /**
     * such as 2.1
     * @return
     */
    public String getMajorVersion() {
        return getVersion().substring(0, getVersion().lastIndexOf("."));
    }

    /** Parse version string, assume it is of the form 1.2.3
     * returns version where each sub-version is divided by 100,
     * so 2.0 -> return 2
     * 2.1 return 2.01
     * 2.1.3 return 2.0103
     * Letters are ignored, so
     * 2.0.e -> 2.0
     * 2.x.1 -> 2.0001
     * @return
     */
    public static double parseVersion(String versionString) {
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
}
