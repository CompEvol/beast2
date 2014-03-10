package beast.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static beast.util.AddOnManager.*;

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
    public boolean isInstalled = false; // TODO duplicate to version.trim().length == 0
    public String version = "";

    public List<PluginDependency> dependencies = new ArrayList<PluginDependency>();

    public Plugin(List<String> list) {
        pluginDescription = list.get(PLUGIN_INTRO_INDEX);
        pluginDescription = formatPluginInfo(list);
        pluginURL = list.get(PLUGIN_URL_INDEX);
        pluginName = URL2AddOnName(pluginURL);
        setVersionDependencies();

    }

    public void setVersionDependencies() {
        isInstalled = false;
        List<String> sBeastDirs = getBeastDirectories();

        // gather version and dependency info for this plugin
        for (String sDir : sBeastDirs) {
            File f = new File(sDir + "/" + pluginName);
            if (f.exists()) {
                isInstalled = true;

                File vf = new File(sDir + "/" + pluginName + "/version.xml");

                if (vf.exists()) {
                    try {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        Document doc = factory.newDocumentBuilder().parse(vf);
                        doc.normalize();
                        // get name and version of plugin
                        Element pluginE = doc.getDocumentElement();
                        version = pluginE.getAttribute("version");

                        // get dependencies of add-n
                        NodeList nodes = doc.getElementsByTagName("depends");
                        for (int i = 0; i < nodes.getLength(); i++) {
                            Element depend_on = (Element) nodes.item(i);

                            PluginDependency dep = new PluginDependency();
                            String plugin = pluginE.getAttribute("name");
                            dep.plugin = plugin;
                            dep.dependson = depend_on.getAttribute("on");
                            String sAtLeast = depend_on.getAttribute("atleast");
                            dep.setAtLest(sAtLeast);
                            String sAtMost = depend_on.getAttribute("atmost");
                            dep.setAtMost(sAtMost);
                            dependencies.add(dep);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String getStatus() {
        return isInstalled ? version : NOT_INSTALLED;
    }

    /**
     * the latest plugin info is online
     * @return
     */
    public String getLatestVersion() {
        return ""; //TODO
    }


    public String toString() {
        return pluginDescription;
    }



    public static String URL2AddOnName(String sURL) {
        String sName = sURL.substring(sURL.lastIndexOf("/") + 1);
        if (sName.contains(".")) {
            sName = sName.substring(0, sName.indexOf("."));
        }
        return sName;
    }

    public static boolean checkIsInstalled(String pluginName) {
        boolean isInstalled = false;
        List<String> sBeastDirs = getBeastDirectories();
        for (String sDir : sBeastDirs) {
            File f = new File(sDir + "/" + pluginName);
            if (f.exists()) {
                isInstalled = true;
            }
        }
        return isInstalled;
    }

    /** pretty format plugin information in list of string form as produced by getAddOns() **/
    public static String formatPluginInfo(List<String> plugin) {
        StringBuffer buf = new StringBuffer();
        buf.append(plugin.get(PLUGIN_NAME_INDEX));
        if (plugin.get(PLUGIN_NAME_INDEX).length() < 12) {
            buf.append("             ".substring(plugin.get(PLUGIN_NAME_INDEX).length()));
        }
        buf.append(" (");
        if (plugin.size() > 4) {
            buf.append("v" + plugin.get(PLUGIN_VERSION_INDEX) + " " + plugin.get(PLUGIN_STATUS_INDEX));
            buf.append((plugin.get(PLUGIN_DEPENDENCIES_INDEX).length() > 0 ? " depends on " + plugin.get(PLUGIN_DEPENDENCIES_INDEX) : ""));
        } else {
            buf.append(plugin.get(PLUGIN_STATUS_INDEX));
        }
        buf.append(")" + ": " + plugin.get(PLUGIN_INTRO_INDEX).trim());
        return buf.toString();
    }




}
