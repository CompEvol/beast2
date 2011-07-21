package beast.app.util;

/**
 * Version last changed 2004/05/07 by AER
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: Version.java,v 1.13 2005/07/11 14:06:25 rambaut Exp $
 */
public interface Version {

    String getVersion();

	String getVersionString();

	String getBuildString();

	String getDateString();

    String[] getCredits();

    String getHTMLCredits();
}
