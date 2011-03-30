package beast.app.beauti;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import beast.core.Description;
import beast.core.Input;
import beast.core.Plugin;

@Description("Beauti configuration object, used to find Beauti configuration " +
		"information from Beauti template files.")
public class BeautiConfig extends Plugin {
	public Input<String> m_inlineInput = new Input<String>("inlinePlugins","comma separated list of inputs that should " +
			"go inline, e.g. beast.core.MCMC.logger");
	public Input<String> m_suppressInputs = new Input<String>("suppressPlugins","comma separated list of inputs that should " +
			"be suppressed. e.g. beast.core.MCMC.operator");
	public Input<String> m_inputLabelMap = new Input<String>("inputLabelMap","comma separated list of inputs and their " +
			"display labels separated by a '=', e.g. beast.core.MCMC.logger=Loggers ");
	public Input<String> m_hidePanels = new Input<String>("hidePanels","comma separated list of panes that should not" +
			"be displayed when starting beauti, e.g. TAXON_SETS_PANEL,TIP_DATES_PANEL");
	public Input<String> m_buttonLabelMap = new Input<String>("buttonLabelMap","comma separated list of buttons in dialogs and their " +
			"display labels separated by a '=', e.g. beast.app.beauti.BeautiInitDlg.&gt;&gt; details=Edit parameters");
	public Input<String> m_disableMenus = new Input<String>("disableMenus","comma separated list of menus that should " +
	"not be visible, e.g., View.Show Data Panel,Mode");

	
	/** list of inputs for which the input editor should be expanded inline in a dialog 
	 * in the format <className>.<inputName>, e.g. beast.core.MCMC.state  
	 */
	public static Set<String> g_inlinePlugins = new HashSet<String>();
	/** list of inputs that should not be shown in a dialog. Same format as for m_inlinePlugins**/
	public static Set<String> g_suppressPlugins = new HashSet<String>();
    /** map that identifies the label to be used for a particular input **/
	public static HashMap<String, String> g_inputLabelMap = new HashMap<String, String>();
	public static HashMap<String, String> g_buttonLabelMap = new HashMap<String, String>();
	
	public static Set<String> g_sHidePanels = new HashSet<String>();
	public static Set<String> g_sDisabledMenus = new HashSet<String>();
	
	@Override
	public void initAndValidate() {
		parseSet(m_inlineInput.get(), null, g_inlinePlugins);
		parseSet(m_hidePanels.get(), "TAXON_SETS_PANEL,TIP_DATES_PANEL,PRIORS_PANEL,OPERATORS_PANEL", g_sHidePanels);
		parseSet(m_suppressInputs.get(), null, g_suppressPlugins);
		parseSet(m_disableMenus.get(), null, g_sDisabledMenus);
		
		parseMap(m_inputLabelMap.get(), g_inputLabelMap);
		parseMap(m_buttonLabelMap.get(), g_buttonLabelMap);
	}

	private void parseMap(String sStr, HashMap<String, String> stringMap) {
		if (sStr != null) {
			for (String sStr2: sStr.split(",")) {
				String [] sStrs = sStr2.split("=");
				stringMap.put(normalize(sStrs[0]), normalize(sStrs[1]));
			}
		}
	}

	private void parseSet(String sStr, String sDefault, Set<String> stringSet) {
		if (sStr == null) {
			sStr = sDefault;
		}
		if (sStr != null) {
			for (String sStr2 : sStr.split(",")) {
				stringSet.add(normalize(sStr2));
			}
		}
	}

	// remove leading and tailing spaces
	String normalize(String sStr) {
		int i = 0;
		int n = sStr.length();
		while (i < n && Character.isWhitespace(sStr.charAt(i))) {
			i++;
		}
		while (n > 0 && Character.isWhitespace(sStr.charAt(n-1))) {
			n--;
		}
		return sStr.substring(i, n);
	}

	public static String getButtonLabel(String sClass, String sStr) {
		if (g_buttonLabelMap.containsKey(sClass + "." + sStr)) {
			return g_buttonLabelMap.get(sClass + "." + sStr);
		}
		return sStr;
	}
	
	public static String getButtonLabel(Object o, String sStr) {
		if (g_buttonLabelMap.containsKey(o.getClass().getName() + "." + sStr)) {
			return g_buttonLabelMap.get(o.getClass().getName() + "." + sStr);
		}
		return sStr;
	}
	public static String getInputLabel(Plugin plugin, String sName) {
		if (g_inputLabelMap.containsKey(plugin.getClass().getName()+"."+sName)) {
			sName =  BeautiConfig.g_inputLabelMap.get(plugin.getClass().getName()+"."+sName);
		}
		return sName;
	}

	public static boolean menuIsInvisible(String sMenuName) {
		return g_sDisabledMenus.contains(sMenuName);
	}
} // class BeautiConfig
