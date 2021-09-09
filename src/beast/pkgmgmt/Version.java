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
}
