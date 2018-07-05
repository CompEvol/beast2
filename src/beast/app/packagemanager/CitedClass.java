package beast.app.packagemanager;

import beast.core.Citation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 */
public class CitedClass {

    protected final String className;
    protected String description = "";
    protected List<Citation> citations = new ArrayList<>();


    public CitedClass(String className, List<Citation> citations) {
        this.className = className;
        this.citations.addAll(citations);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCitations() {
        String citaStr = "";
        for (Citation citation : citations) {
            citaStr += citation.value() + "\n";
            // print DOI
            if (citation.DOI().length() > 0) {
                citaStr += citation.DOI() + "\n";
            }
        }
        return citaStr;
    }
}
