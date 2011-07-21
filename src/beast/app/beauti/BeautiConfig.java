package beast.app.beauti;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import beast.app.draw.InputEditor;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;

@Description("Beauti configuration object, used to find Beauti configuration " +
		"information from Beauti template files.")
public class BeautiConfig extends Plugin {
	public Input<String> m_inlineInput = new Input<String>("inlinePlugins","comma separated list of inputs that should " +
			"go inline, e.g. beast.evolution.sitemodel.SiteModel.substModel");
	public Input<String> m_collapsedInput = new Input<String>("collapsedPlugins","comma separated list of inputs that should " +
		"go inline, but are initially collapsed, e.g. beast.core.MCMC.logger");
	public Input<String> m_suppressInputs = new Input<String>("suppressPlugins","comma separated list of inputs that should " +
			"be suppressed. e.g. beast.core.MCMC.operator");
	public Input<String> m_inputLabelMap = new Input<String>("inputLabelMap","comma separated list of inputs and their " +
			"display labels separated by a '=', e.g. beast.core.MCMC.logger=Loggers ");
//	public Input<String> m_hidePanels = new Input<String>("hidePanels","comma separated list of panes that should not" +
//			"be displayed when starting beauti, e.g. TAXON_SETS_PANEL,TIP_DATES_PANEL");
	public Input<String> m_buttonLabelMap = new Input<String>("buttonLabelMap","comma separated list of buttons in dialogs and their " +
			"display labels separated by a '=', e.g. beast.app.beauti.BeautiInitDlg.&gt;&gt; details=Edit parameters");
	public Input<String> m_disableMenus = new Input<String>("disableMenus","comma separated list of menus that should " +
			"not be visible, e.g., View.Show Data Panel,Mode");
	public Input<String> m_disableButtons = new Input<String>("disableButtons","comma separated list of buttons that should " +
			"not be visible, e.g., beast.app.beauti.BeautiInitDlg.Analysis template:");
//	public Input<String> m_editButtonStatus = new Input<String>("editButtonStatus","comma separated list of list-inputs with custom " +
//	"button status. One of 'none', 'addonly' 'delonly' +, e.g., beast.core.MCMC.operator=addonly");

	public Input<List<BeautiPanelConfig>> m_panels = new Input<List<BeautiPanelConfig>>("panel", "define custom panels and their properties", 
			new ArrayList<BeautiPanelConfig>());
	public Input<Boolean> m_bIsExpertInput = new Input<Boolean>("isExpert", "flag to indicate Beauti should start in expert mode", false);
	
	public Input<List<InputConstraint>> m_inputConstraints = new Input<List<InputConstraint>>("constraint", "defines constraints on inputs", 
			new ArrayList<InputConstraint>());

	
	public Input<BeautiSubTemplate> m_partitionTemplate = new Input<BeautiSubTemplate>("partitiontemplate", "defines template used when creating a partition", Validate.REQUIRED);
	public Input<List<BeautiSubTemplate>> m_subTemplates = new Input<List<BeautiSubTemplate>>("subtemplate", "defines subtemplates for creating selected classes", 
			new ArrayList<BeautiSubTemplate>());

	
	/** list of inputs for which the input editor should be expanded inline in a dialog 
	 * in the format <className>.<inputName>, e.g. beast.evolution.sitemodel.SiteModel.substModel */
	public static Set<String> g_inlinePlugins = new HashSet<String>();
	/** list of inputs for which the input editor should be expanded inline in a dialog but initially collapsed.  
	 * e.g. beast.evolution.sitemodel.SiteModel.substModel */
	public static Set<String> g_collapsedPlugins = new HashSet<String>();
	/** list of inputs that should not be shown in a dialog. Same format as for m_inlinePlugins**/
	public static Set<String> g_suppressPlugins = new HashSet<String>();
    /** map that identifies the label to be used for a particular input **/
	public static HashMap<String, String> g_inputLabelMap = new HashMap<String, String>();
	public static HashMap<String, String> g_buttonLabelMap = new HashMap<String, String>();
//	public static HashMap<String, String> g_sEditButtonStatus = new HashMap<String, String>();
		
//	public static Set<String> g_sHidePanels = new HashSet<String>();
	public static Set<String> g_sDisabledMenus = new HashSet<String>();
	public static Set<String> g_sDisabledButtons = new HashSet<String>();

	public static List<BeautiPanelConfig> g_panels = new ArrayList<BeautiPanelConfig>();

	public static HashMap<String, List<String>> g_constraintMap = new HashMap<String, List<String>>();

	public static List<BeautiSubTemplate> g_subTemplates;
	
	@Override
	public void initAndValidate() {
		parseSet(m_inlineInput.get(), null, g_inlinePlugins);
		parseSet(m_collapsedInput.get(), null, g_collapsedPlugins);
		g_inlinePlugins.addAll(g_collapsedPlugins);
//		parseSet(m_hidePanels.get(), "TAXON_SETS_PANEL,TIP_DATES_PANEL,PRIORS_PANEL,OPERATORS_PANEL", g_sHidePanels);
		parseSet(m_suppressInputs.get(), null, g_suppressPlugins);
		parseSet(m_disableMenus.get(), null, g_sDisabledMenus);
		parseSet(m_disableButtons.get(), null, g_sDisabledButtons);
		
		parseMap(m_inputLabelMap.get(), g_inputLabelMap);
		parseMap(m_buttonLabelMap.get(), g_buttonLabelMap);
//		parseMap(m_editButtonStatus.get(), g_sEditButtonStatus);
		for (BeautiPanelConfig panel : m_panels.get()) {
			g_panels.add(panel);
			// check for duplicates
			for (BeautiPanelConfig panel2 : g_panels) {
				if (panel2.m_sNameInput.get().equals(panel.m_sNameInput.get()) && panel2!=panel) {
					g_panels.remove(g_panels.size()-1);
					break;
				}
			}
		}
		InputEditor.g_bExpertMode = m_bIsExpertInput.get();
		parseConstraints();
		g_subTemplates = m_subTemplates.get();
	}

	public static void clear() {
		g_inlinePlugins = new HashSet<String>();
		g_collapsedPlugins = new HashSet<String>();
		g_suppressPlugins = new HashSet<String>();
		g_inputLabelMap = new HashMap<String, String>();
		g_buttonLabelMap = new HashMap<String, String>();
		g_sDisabledMenus = new HashSet<String>();
		g_sDisabledButtons = new HashSet<String>();
		g_panels = new ArrayList<BeautiPanelConfig>();
		g_constraintMap = new HashMap<String, List<String>>();
	}

	private void parseConstraints() {
		List<InputConstraint> constraints = m_inputConstraints.get();
		for (InputConstraint constraint : constraints) {
			List<String> candidates = new ArrayList<String>();
			for (Plugin candidate : constraint.m_candidates.get()) {
				candidates.add(candidate.getID());
			}
			for (Plugin plugin : constraint.m_plugins.get()) {
				String sID = plugin.getID() + "." + constraint.m_inputName.get();
				g_constraintMap.put(sID, candidates);
			}
		}
	}
	
	public static List<BeautiSubTemplate> getInputCandidates(Plugin plugin, Input<?> input, Class<?> type) {
		List<BeautiSubTemplate> candidates = new ArrayList<BeautiSubTemplate>();
		for (BeautiSubTemplate template : g_subTemplates) {
			if (type.isAssignableFrom(template.m_class)) {
	        	try {
	        		if (input.canSetValue(template.m_instance, plugin)) {
	    				candidates.add(template);
	        		}
	        	} catch (Exception e) {
					// ignore: cannot set value
				}
			}
		}
		return candidates;
//		String sID = plugin.getID() + "." + input.getName();
//		if (g_constraintMap.containsKey(sID)) {
//			return g_constraintMap.get(sID);
//		}
//		return null;
	}

	private void parseMap(String sStr, HashMap<String, String> stringMap) {
		if (sStr != null) {
			for (String sStr2: sStr.split(",")) {
				String [] sStrs = sStr2.split("=");
				stringMap.put(normalize(sStrs[0]), normalize(sStrs.length==1?"":sStrs[1]));
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

	static BeautiSubTemplate NULL_TEMPLATE = new BeautiSubTemplate();
	
	public static BeautiSubTemplate getNullTemplate() {
		NULL_TEMPLATE.setID("[none]");
		NULL_TEMPLATE.m_class = Object.class;
		return NULL_TEMPLATE;
	}

//	public static boolean hasDeleteButton(String sFullInputName) {
//		if (!g_sEditButtonStatus.containsKey(sFullInputName)) {
//			return true;
//		}
//		String sStatus = g_sEditButtonStatus.get(sFullInputName);
//		if (sStatus.equals("none") || sStatus.equals("onlyadd")) {
//			return false;
//		}
//		return true;
//	}
//	public static boolean hasAddButton(String sFullInputName) {
//		if (!g_sEditButtonStatus.containsKey(sFullInputName)) {
//			return true;
//		}
//		String sStatus = g_sEditButtonStatus.get(sFullInputName);
//		if (sStatus.equals("none") || sStatus.equals("onlydel")) {
//			return false;
//		}
//		return true;
//	}
} // class BeautiConfig
