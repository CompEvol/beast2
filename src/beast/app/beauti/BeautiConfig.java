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
	
	
	/** list of inputs for which the input editor should be expanded inline in a dialog 
	 * in the format <className>.<inputName>, e.g. beast.core.MCMC.state  
	 */
	public static Set<String> g_inlinePlugins = new HashSet<String>();
	/** list of inputs that should not be shown in a dialog. Same format as for m_inlinePlugins**/
	public static Set<String> g_suppressPlugins = new HashSet<String>();
    /** map that identifies the label to be used for a particular input **/
	public static HashMap<String, String> g_inputLabelMap = new HashMap<String, String>();
	
	public static Set<String> g_sHidePanels = new HashSet<String>();
	
	@Override
	public void initAndValidate() {
		String sInlinePlugins = m_inlineInput.get();
		if (sInlinePlugins != null) {
			for (String sInlinePlugin: sInlinePlugins.split(",")) {
				g_inlinePlugins.add(normalize(sInlinePlugin));
			}
		}
		
		String sHidePanels = m_hidePanels.get();
		if (sHidePanels == null) {
			sHidePanels = "TAXON_SETS_PANEL,TIP_DATES_PANEL,PRIORS_PANEL,OPERATORS_PANEL";
		}
		for (String sPanel: sHidePanels.split(",")) {
			g_sHidePanels.add(normalize(sPanel));
		}
		
		String sInputLabelMap = m_inputLabelMap.get();
		if (sInputLabelMap != null) {
			for (String sLabelMap: sInputLabelMap.split(",")) {
				String [] sStr = sLabelMap.split("=");
				g_inputLabelMap.put(normalize(sStr[0]), normalize(sStr[1]));
			}
		}
		
		String sSuppressPlugins = m_suppressInputs.get();
		if (sSuppressPlugins != null) {
			for (String sSuppressPlugin: sSuppressPlugins.split(",")) {
				g_suppressPlugins.add(normalize(sSuppressPlugin));
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

} // class BeautiConfig
