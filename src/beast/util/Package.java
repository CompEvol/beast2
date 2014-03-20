package beast.util;

import beast.core.Description;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static beast.util.AddOnManager.NOT_INSTALLED;
import static beast.util.AddOnManager.getBeastDirectories;

/**
 * BEAUti Package managed by AddOnManager
 * all property is for installed package only
 * the released/latest package info is online
 *
 * modified by Walter Xie
 */
@Description("BEAUti package managed by package manager, also named as add-on previously")
public class Package {
    public String description = "";
    public String url = "";
    public String packageName = "";
    public String installedVersion = ""; // get from local /version.xml
    public String latestVersion = ""; // get from packages.xml

    public Set<PackageDependency> dependencies = new TreeSet<PackageDependency>();

    public Package(Element packageE) {
        url = packageE.getAttribute("url");
//        packageName = URL2PackageName(url);
        packageName = packageE.getAttribute("name");
        latestVersion = packageE.getAttribute("version");
        description = packageE.getAttribute("description");

        NodeList nodes = packageE.getElementsByTagName("depends");
        setVersionDependencies(nodes);
    }

    public void setVersionDependencies(NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Element depend_on = (Element) nodes.item(i);

            installedVersion = getVersionDependencyFromLocal(packageName, dependencies);

            if (installedVersion == null) {
                installedVersion = "";
                PackageDependency dep = getPackageDependency(packageName, depend_on);
                dependencies.add(dep);
            }
        }
    }

    public String getVersionDependencyFromLocal(String packageName, Set<PackageDependency> dependencies) {
        List<String> sBeastDirs = getBeastDirectories();

        // gather dependency info for this package
        for (String sDir : sBeastDirs) {
            File f = new File(sDir + "/" + packageName);
            if (f.exists()) {
                File vf = new File(sDir + "/" + packageName + "/version.xml");

                if (vf.exists()) {
                    try {
                        // parse installed version.xml
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        Document doc = factory.newDocumentBuilder().parse(vf);
                        doc.normalize();
                        // get name and version of package
                        Element packageE = doc.getDocumentElement();
//                        String packageName = packageE.getAttribute("name");
                        String installedVersion = packageE.getAttribute("version");

                        // get dependencies of add-n
                        NodeList nodes = doc.getElementsByTagName("depends");
                        for (int i = 0; i < nodes.getLength(); i++) {
                            Element depend_on = (Element) nodes.item(i);

                            PackageDependency dep = getPackageDependency(packageName, depend_on);
                            dependencies.add(dep);
                        }

                        return installedVersion;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return null;
    }

    private PackageDependency getPackageDependency(String packageName, Element depend_on) {
        PackageDependency dep = new PackageDependency();
        dep.packageName = packageName;
        dep.dependson = depend_on.getAttribute("on");

        String sAtLeast = depend_on.getAttribute("atleast");
        dep.setAtLest(sAtLeast);
        String sAtMost = depend_on.getAttribute("atmost");
        dep.setAtMost(sAtMost);
        return dep;
    }

    public boolean isInstalled() {
        return installedVersion.trim().length() > 0;
    }

    public String getStatus() {
        return isInstalled() ? installedVersion : NOT_INSTALLED;
    }

    /**
     * the latest package info is online
     * @return
     */
    public String getLatestVersion() {
        return latestVersion;
    }

    public boolean dependsOn(String packageName) {
        for (PackageDependency packageDependency : dependencies) {
            if (packageDependency.compareTo(packageName) == 0)
                return true;
        }
        return false;
    }

    public String getDependenciesString() {
        String depString = "";
        for (PackageDependency packageDependency : dependencies) {
            String s = packageDependency.dependson;
            if (!s.equalsIgnoreCase("beast2")) {
                depString +=  s + ", ";
            }
        }
        if (depString.length() > 2) {
            depString = depString.substring(0, depString.length() - 2);
        }
        return depString;
    }

    public String toString() {
        return description;
    }

    public String toHTML() {
        String html = "TODO";
        return html;
    }
}
