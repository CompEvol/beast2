package beast.app.pluginloader;

import beast.core.Plugin;

import java.util.List;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public interface PluginPackage {
    List<Plugin> getPlugins();    
}
