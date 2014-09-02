package beast.app.util;

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
        String sStr = "";
        for (String s : getCredits()) {
            if (s.contains("@")) {
                sStr += "<a href=\"mailto:" + s + "\">" + s + "</a><br>";
            }
            if (s.contains("http")) {
                sStr += "<a href=\"" + s + "\">" + s + "</a><br>";
            } else {
                sStr += "<p>" + s + "</p>";
            }
        }
        return sStr;

    }
}
