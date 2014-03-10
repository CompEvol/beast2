package beast.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static beast.util.AddOnManager.NOT_INSTALLED;
import static beast.util.AddOnManager.getBeastDirectories;

/**
 * BEAUti Plugin managed by AddOnManager
 * all property is for installed plugin only
 * the released/latest plugin info is online
 *
 * modified by Walter Xie
 */
public class Plugin {
    public String pluginDescription = "";
    public String pluginURL = "";
    public String pluginName = "";
    public String installedVersion = ""; // get from local /version.xml
    public String latestVersion = ""; // get from plugins.xml

    public List<PluginDependency> dependencies = new ArrayList<PluginDependency>();

    public Plugin(Element pluginE) {
        pluginURL = pluginE.getAttribute("url");
//        pluginName = URL2AddOnName(pluginURL);
        pluginName = pluginE.getAttribute("name");
        latestVersion = pluginE.getAttribute("version");
        pluginDescription = pluginE.getAttribute("description");

        NodeList nodes = pluginE.getElementsByTagName("depends");
        setVersionDependencies(nodes);
    }

    public void setVersionDependencies(NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Element depend_on = (Element) nodes.item(i);

            installedVersion = getVersionDependencyFromLocal(pluginName, dependencies);

            if (installedVersion == null) {
                installedVersion = "";
                PluginDependency dep = getPluginDependency(pluginName, depend_on);
                dependencies.add(dep);
            }
        }
    }

    public String getVersionDependencyFromLocal(String pluginName, List<PluginDependency> dependencies) {
        List<String> sBeastDirs = getBeastDirectories();

        // gather dependency info for this plugin
        for (String sDir : sBeastDirs) {
            File f = new File(sDir + "/" + pluginName);
            if (f.exists()) {
                File vf = new File(sDir + "/" + pluginName + "/version.xml");

                if (vf.exists()) {
                    try {
                        // parse installed version.xml
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        Document doc = factory.newDocumentBuilder().parse(vf);
                        doc.normalize();
                        // get name and version of plugin
                        Element pluginE = doc.getDocumentElement();
//                        String pluginName = pluginE.getAttribute("name");
                        String installedVersion = pluginE.getAttribute("version");

                        // get dependencies of add-n
                        NodeList nodes = doc.getElementsByTagName("depends");
                        for (int i = 0; i < nodes.getLength(); i++) {
                            Element depend_on = (Element) nodes.item(i);

                            PluginDependency dep = getPluginDependency(pluginName, depend_on);

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

    private PluginDependency getPluginDependency(String pluginName, Element depend_on) {
        PluginDependency dep = new PluginDependency();
        dep.plugin = pluginName;
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
     * the latest plugin info is online
     * @return
     */
    public String getLatestVersion() {
        return latestVersion;
    }

    public String getDependencies() {
        String depString = "";
        for (PluginDependency pluginDependency : dependencies) {
            String s = pluginDependency.dependson;
            if (!s.equals("beast2")) {
                depString +=  s + ", ";
            }
        }
        if (depString.length() > 2) {
            depString = depString.substring(0, depString.length() - 2);
        }
        return depString;
    }

    public String toString() {
        return pluginDescription;
    }


}
